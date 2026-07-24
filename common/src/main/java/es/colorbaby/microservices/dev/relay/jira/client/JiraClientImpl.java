package es.colorbaby.microservices.dev.relay.jira.client;

import es.colorbaby.microservices.dev.relay.jira.config.JiraAuthMode;
import es.colorbaby.microservices.dev.relay.jira.config.JiraProperties;
import es.colorbaby.microservices.dev.relay.openapi.model.JiraCommentDto;
import es.colorbaby.microservices.dev.relay.openapi.model.JiraCommentsResultDto;
import es.colorbaby.microservices.dev.relay.openapi.model.JiraIssueDto;
import es.colorbaby.microservices.dev.relay.openapi.model.JiraSearchResultDto;
import es.colorbaby.microservices.dev.relay.openapi.model.JiraTransitionDto;
import es.colorbaby.microservices.dev.relay.openapi.model.JiraTransitionsResultDto;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Implementación de {@link JiraClient} sobre {@link RestTemplate}. Sin
 * lógica de negocio: solo traduce cada operación a la llamada REST
 * correspondiente de Jira. Toda llamada saliente pasa por un Retry
 * (solo ante fallos transitorios) y un CircuitBreaker, configurados de
 * forma programática para no imponer AOP a quien use la librería.
 */
@Slf4j
public class JiraClientImpl implements JiraClient {

  /**
   * Campos que pedimos a Jira en las búsquedas. El nuevo endpoint de Cloud
   * (/search/jql) solo devuelve id+key si no se especifican.
   */
  private static final List<String> SEARCH_FIELDS =
      List.of("summary", "assignee", "reporter", "status", "labels");

  private final RestTemplate jiraRestTemplate;
  private final JiraProperties jiraProperties;
  private final Retry retry;
  private final CircuitBreaker circuitBreaker;

  public JiraClientImpl(RestTemplate jiraRestTemplate, JiraProperties jiraProperties) {
    this.jiraRestTemplate = jiraRestTemplate;
    this.jiraProperties = jiraProperties;

    Predicate<Throwable> transient503 = JiraClientImpl::isTransient;
    this.retry = Retry.of("jira", RetryConfig.custom()
        .maxAttempts(jiraProperties.getRetryMaxAttempts())
        .waitDuration(Duration.ofMillis(jiraProperties.getRetryWaitMs()))
        .retryOnException(transient503)
        .build());
    this.circuitBreaker = CircuitBreaker.of("jira", CircuitBreakerConfig.custom()
        .recordException(transient503)
        .build());
  }

  @Override
  public JiraIssueDto getIssue(String issueKey) {
    URI uri = apiUri("/issue/{key}").buildAndExpand(issueKey).toUri();
    return execute(() -> jiraRestTemplate.getForObject(uri, JiraIssueDto.class),
        "Error obteniendo la issue " + issueKey);
  }

  @Override
  public byte[] downloadAttachment(String contentUrl) {
    // La URL ya viene completa de Jira; se usa tal cual con el RestTemplate autenticado.
    URI uri = URI.create(contentUrl);
    byte[] content = execute(() -> jiraRestTemplate.getForObject(uri, byte[].class),
        "Error descargando el adjunto " + contentUrl);
    return content == null ? new byte[0] : content;
  }

  @Override
  public List<JiraIssueDto> searchIssuesByJql(String jql) {
    return jiraProperties.getAuthMode() == JiraAuthMode.SERVER_PAT
        ? searchByOffset(jql)
        : searchByCursor(jql);
  }

  /**
   * Jira Cloud: POST /rest/api/3/search/jql, paginación por cursor.
   */
  private List<JiraIssueDto> searchByCursor(String jql) {
    List<JiraIssueDto> all = new ArrayList<>();
    String nextPageToken = null;
    int pages = 0;
    while (pages < jiraProperties.getSearchMaxPages()) {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("jql", jql);
      body.put("maxResults", jiraProperties.getSearchPageSize());
      body.put("fields", SEARCH_FIELDS);
      if (nextPageToken != null) {
        body.put("nextPageToken", nextPageToken);
      }
      URI uri = apiUri("/search/jql").build().toUri();
      JiraSearchResultDto page = execute(
          () -> jiraRestTemplate.postForObject(uri, body, JiraSearchResultDto.class),
          "Error buscando issues por JQL: " + jql);
      if (page != null) {
        if (page.getIssues() != null) {
          all.addAll(page.getIssues());
        }
        pages++;
        nextPageToken = page.getNextPageToken();
      }
      if (page == null || Boolean.TRUE.equals(page.getIsLast()) || nextPageToken == null) {
        break;
      }
    }
    return all;
  }

  /**
   * Jira Server/DC: GET /rest/api/2/search, paginación por offset.
   */
  private List<JiraIssueDto> searchByOffset(String jql) {
    List<JiraIssueDto> all = new ArrayList<>();
    int startAt = 0;
    int pages = 0;
    while (pages < jiraProperties.getSearchMaxPages()) {
      int currentStart = startAt;
      URI uri = apiUri("/search")
          .queryParam("jql", jql)
          .queryParam("startAt", currentStart)
          .queryParam("maxResults", jiraProperties.getSearchPageSize())
          .queryParam("fields", String.join(",", SEARCH_FIELDS))
          .build()
          .toUri();
      JiraSearchResultDto page = execute(
          () -> jiraRestTemplate.getForObject(uri, JiraSearchResultDto.class),
          "Error buscando issues por JQL: " + jql);
      List<JiraIssueDto> issues = page == null || page.getIssues() == null
          ? List.of() : page.getIssues();
      all.addAll(issues);
      pages++;
      startAt += issues.size();
      int total = page == null || page.getTotal() == null ? 0 : page.getTotal();
      if (issues.isEmpty() || startAt >= total) {
        break;
      }
    }
    return all;
  }

  @Override
  public List<JiraCommentDto> getComments(String issueKey) {
    URI uri = apiUri("/issue/{key}/comment").buildAndExpand(issueKey).toUri();
    JiraCommentsResultDto result = execute(
        () -> jiraRestTemplate.getForObject(uri, JiraCommentsResultDto.class),
        "Error obteniendo comentarios de " + issueKey);
    return result == null || result.getComments() == null ? List.of() : result.getComments();
  }

  @Override
  public JiraCommentDto addComment(String issueKey, String plainTextBody) {
    URI uri = apiUri("/issue/{key}/comment").buildAndExpand(issueKey).toUri();
    Map<String, Object> requestBody = jiraProperties.getAuthMode() == JiraAuthMode.SERVER_PAT
        ? Map.of("body", plainTextBody)
        : Map.of("body", adfParagraph(plainTextBody));
    return execute(() -> jiraRestTemplate.postForObject(uri, requestBody, JiraCommentDto.class),
        "Error añadiendo comentario en " + issueKey);
  }

  @Override
  public JiraCommentDto addCommentAdf(String issueKey, Object adfDocument) {
    URI uri = apiUri("/issue/{key}/comment").buildAndExpand(issueKey).toUri();
    Map<String, Object> requestBody = Map.of("body", adfDocument);
    return execute(() -> jiraRestTemplate.postForObject(uri, requestBody, JiraCommentDto.class),
        "Error añadiendo comentario (ADF) en " + issueKey);
  }

  @Override
  public void addLabel(String issueKey, String label) {
    URI uri = apiUri("/issue/{key}").buildAndExpand(issueKey).toUri();
    // update.labels.add es no destructivo: conserva las etiquetas ya existentes.
    Map<String, Object> requestBody =
        Map.of("update", Map.of("labels", List.of(Map.of("add", label))));
    execute(() -> {
      jiraRestTemplate.put(uri, requestBody);
      return null;
    }, "Error añadiendo la etiqueta '" + label + "' en " + issueKey);
  }

  @Override
  public List<JiraTransitionDto> getTransitions(String issueKey) {
    URI uri = apiUri("/issue/{key}/transitions").buildAndExpand(issueKey).toUri();
    JiraTransitionsResultDto result = execute(
        () -> jiraRestTemplate.getForObject(uri, JiraTransitionsResultDto.class),
        "Error obteniendo transiciones de " + issueKey);
    return result == null || result.getTransitions() == null ? List.of() : result.getTransitions();
  }

  @Override
  public void transitionIssue(String issueKey, String transitionId) {
    URI uri = apiUri("/issue/{key}/transitions").buildAndExpand(issueKey).toUri();
    Map<String, Object> requestBody = Map.of("transition", Map.of("id", transitionId));
    execute(() -> {
      jiraRestTemplate.postForLocation(uri, requestBody);
      return null;
    }, "Error ejecutando transición " + transitionId + " en " + issueKey);
  }

  @Override
  public void transitionIssueByStatusName(String issueKey, String targetStatusName) {
    String transitionId = getTransitions(issueKey).stream()
        .filter(t -> t.getTo() != null
            && targetStatusName.equalsIgnoreCase(t.getTo().getName()))
        .map(JiraTransitionDto::getId)
        .findFirst()
        .orElseThrow(() -> new JiraClientException(
            "No hay transición disponible hacia el estado \"" + targetStatusName
                + "\" para " + issueKey));
    transitionIssue(issueKey, transitionId);
  }

  @Override
  public void assignIssue(String issueKey, String accountIdOrUsername) {
    URI uri = apiUri("/issue/{key}/assignee").buildAndExpand(issueKey).toUri();
    Map<String, Object> requestBody = jiraProperties.getAuthMode() == JiraAuthMode.SERVER_PAT
        ? Map.of("name", accountIdOrUsername)
        : Map.of("accountId", accountIdOrUsername);
    execute(() -> {
      jiraRestTemplate.put(uri, requestBody);
      return null;
    }, "Error reasignando " + issueKey + " a " + accountIdOrUsername);
  }

  private Map<String, Object> adfParagraph(String text) {
    Map<String, Object> textNode = new LinkedHashMap<>();
    textNode.put("type", "text");
    textNode.put("text", text);

    Map<String, Object> paragraph = new LinkedHashMap<>();
    paragraph.put("type", "paragraph");
    paragraph.put("content", List.of(textNode));

    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put("type", "doc");
    doc.put("version", 1);
    doc.put("content", List.of(paragraph));
    return doc;
  }

  private UriComponentsBuilder apiUri(String path) {
    String apiVersion = jiraProperties.getAuthMode() == JiraAuthMode.SERVER_PAT ? "2" : "3";
    return UriComponentsBuilder.fromUriString(jiraProperties.getBaseUrl())
        .path("/rest/api/" + apiVersion + path);
  }

  private <T> T execute(Supplier<T> call, String errorMessage) {
    Supplier<T> resilient = Retry.decorateSupplier(retry,
        CircuitBreaker.decorateSupplier(circuitBreaker, call));
    try {
      return resilient.get();
    } catch (CallNotPermittedException e) {
      log.error("Circuito abierto hacia Jira: {}", errorMessage);
      throw new JiraClientException(errorMessage + " (circuito abierto hacia Jira)", e);
    } catch (RestClientException e) {
      log.error(errorMessage, e);
      throw new JiraClientException(errorMessage, e);
    }
  }

  /**
   * Solo son reintentables (y solo abren el circuito) los fallos transitorios:
   * errores de red y respuestas 5xx. Un 4xx no mejora al reintentar.
   */
  private static boolean isTransient(Throwable throwable) {
    return throwable instanceof ResourceAccessException
        || (throwable instanceof HttpServerErrorException)
        || (throwable instanceof RestClientException
            && !(throwable instanceof HttpClientErrorException)
            && !(throwable instanceof HttpServerErrorException));
  }
}
