package es.colorbaby.microservices.dev.relay.jira.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Añade la cabecera de autenticación a toda petición saliente al cliente Jira,
 * según el auth-mode configurado: Basic (email:api-token) para Jira Cloud,
 * Bearer (api-token como Personal Access Token) para Jira Server/DC.
 */
@RequiredArgsConstructor
public class JiraAuthInterceptor implements ClientHttpRequestInterceptor {

  private final JiraProperties jiraProperties;

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    request.getHeaders().set(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader());
    return execution.execute(request, body);
  }

  private String buildAuthorizationHeader() {
    if (jiraProperties.getAuthMode() == JiraAuthMode.SERVER_PAT) {
      return "Bearer " + jiraProperties.getApiToken();
    }
    String credentials = jiraProperties.getEmail() + ":" + jiraProperties.getApiToken();
    String encoded = Base64.getEncoder()
        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    return "Basic " + encoded;
  }
}
