package es.colorbaby.microservices.dev.relay.jenkins.client;

import es.colorbaby.microservices.dev.relay.jenkins.config.JenkinsProperties;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementación de {@link JenkinsClient} sobre {@link RestTemplate}. Construye las URIs con
 * {@link URI#create} (sin volver a codificar) para respetar el {@code %2F} del nombre de rama en
 * los jobs multibranch, que un template de RestTemplate rompería al doble-codificar.
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
    final URI uri = uri(jobPath + "/lastBuild/api/json?tree=number,result,building,url");
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
      throw new JenkinsClientException("Error leyendo el último build de " + jobPath, e);
    }
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

  private URI uri(final String pathAndQuery) {
    final String base = properties.getBaseUrl().replaceAll("/+$", "");
    return URI.create(base + "/" + pathAndQuery);
  }

  private static String str(final Object value) {
    return value == null ? null : value.toString();
  }
}
