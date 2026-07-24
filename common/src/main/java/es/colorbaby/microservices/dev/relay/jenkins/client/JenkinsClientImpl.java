package es.colorbaby.microservices.dev.relay.jenkins.client;

import es.colorbaby.microservices.dev.relay.jenkins.config.JenkinsProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementación de {@link JenkinsClient} sobre {@link RestTemplate}. Construye las URIs con
 * {@link URI#create} (sin volver a codificar) para respetar caracteres ya escapados, y añade el
 * crumb CSRF en los POST, que Jenkins exige por defecto.
 */
@Slf4j
public class JenkinsClientImpl implements JenkinsClient {

  private final RestTemplate jenkinsRestTemplate;
  private final JenkinsProperties properties;

  public JenkinsClientImpl(final RestTemplate jenkinsRestTemplate,
      final JenkinsProperties properties) {
    this.jenkinsRestTemplate = jenkinsRestTemplate;
    this.properties = properties;
  }

  @Override
  public Optional<Build> getLastBuild(final String jobPath) {
    return fetchBuild(jobPath + "/lastBuild", "el último build de " + jobPath);
  }

  @Override
  public Optional<Build> getBuild(final String jobPath, final int buildNumber) {
    return fetchBuild(jobPath + "/" + buildNumber,
        "el build " + buildNumber + " de " + jobPath);
  }

  @Override
  public String getConsoleLog(final String jobPath, final int buildNumber) {
    final URI uri = uri(jobPath + "/" + buildNumber + "/consoleText");
    try {
      final String consoleLog = jenkinsRestTemplate.getForObject(uri, String.class);
      return consoleLog == null ? "" : consoleLog;
    } catch (RestClientException e) {
      throw new JenkinsClientException(
          "Error leyendo la consola del build " + buildNumber + " de " + jobPath, e);
    }
  }

  @Override
  public String triggerBuild(final String jobPath, final Map<String, String> parameters) {
    final URI uri = uri(jobPath + "/buildWithParameters?" + queryString(parameters));
    try {
      final ResponseEntity<Void> response = jenkinsRestTemplate.exchange(
          uri, HttpMethod.POST, new HttpEntity<>(crumbHeaders()), Void.class);
      final URI location = response.getHeaders().getLocation();
      if (location == null) {
        throw new JenkinsClientException(
            "Jenkins aceptó el build de " + jobPath + " pero no devolvió el item en cola", null);
      }
      return location.toString();
    } catch (RestClientException e) {
      throw new JenkinsClientException("Error lanzando el job " + jobPath, e);
    }
  }

  @Override
  public Optional<Integer> resolveQueuedBuild(final String queueUrl) {
    final URI uri = URI.create(trimSlash(queueUrl) + "/api/json?tree=executable[number]");
    try {
      final Map<?, ?> json = jenkinsRestTemplate.getForObject(uri, Map.class);
      if (json == null) {
        return Optional.empty();
      }
      // Mientras sigue en cola no hay "executable": no es un error, es "todavía no ha arrancado".
      final Object executable = json.get("executable");
      if (executable instanceof Map<?, ?> exec && exec.get("number") instanceof Number n) {
        return Optional.of(n.intValue());
      }
      return Optional.empty();
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    } catch (RestClientException e) {
      throw new JenkinsClientException("Error resolviendo el item en cola " + queueUrl, e);
    }
  }

  private Optional<Build> fetchBuild(final String path, final String what) {
    final URI uri = uri(path + "/api/json?tree=number,result,building,url");
    try {
      final Map<?, ?> json = jenkinsRestTemplate.getForObject(uri, Map.class);
      if (json == null) {
        return Optional.empty();
      }
      final int number = json.get("number") instanceof Number n ? n.intValue() : 0;
      final String result = str(json.get("result"));
      final boolean building = Boolean.TRUE.equals(json.get("building"));
      final String url = str(json.get("url"));
      return Optional.of(new Build(number, result, building, url));
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    } catch (RestClientException e) {
      throw new JenkinsClientException("Error leyendo " + what, e);
    }
  }

  /**
   * Crumb CSRF. Si Jenkins lo tiene desactivado, el endpoint no existe y se sigue sin cabecera:
   * por eso el fallo se traga a propósito en vez de romper el disparo.
   */
  private HttpHeaders crumbHeaders() {
    final HttpHeaders headers = new HttpHeaders();
    try {
      final Map<?, ?> crumb = jenkinsRestTemplate.getForObject(
          uri("crumbIssuer/api/json"), Map.class);
      if (crumb != null) {
        final String field = str(crumb.get("crumbRequestField"));
        final String value = str(crumb.get("crumb"));
        if (field != null && value != null) {
          headers.set(field, value);
        }
      }
    } catch (RestClientException e) {
      log.debug("Jenkins sin crumb CSRF ({}); se lanza sin cabecera", e.getMessage());
    }
    return headers;
  }

  private static String queryString(final Map<String, String> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return "";
    }
    final StringBuilder query = new StringBuilder();
    parameters.forEach((key, value) -> {
      if (!query.isEmpty()) {
        query.append('&');
      }
      query.append(encode(key)).append('=').append(encode(value == null ? "" : value));
    });
    return query.toString();
  }

  private static String encode(final String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private URI uri(final String pathAndQuery) {
    return URI.create(trimSlash(properties.getBaseUrl()) + "/" + pathAndQuery);
  }

  private static String trimSlash(final String value) {
    return value == null ? "" : value.replaceAll("/+$", "");
  }

  private static String str(final Object value) {
    return value == null ? null : value.toString();
  }
}
