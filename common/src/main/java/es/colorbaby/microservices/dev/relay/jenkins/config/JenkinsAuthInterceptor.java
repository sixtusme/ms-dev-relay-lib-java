package es.colorbaby.microservices.dev.relay.jenkins.config;

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
 * Añade autenticación básica (usuario + API token) a cada petición a Jenkins. Solo se añade si hay
 * usuario configurado, para no mandar una cabecera vacía cuando el módulo está apagado.
 */
@RequiredArgsConstructor
public class JenkinsAuthInterceptor implements ClientHttpRequestInterceptor {

  private final JenkinsProperties properties;

  @Override
  public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
      final ClientHttpRequestExecution execution) throws IOException {
    if (properties.getUser() != null && !properties.getUser().isBlank()) {
      final String raw = properties.getUser() + ":" + properties.getApiToken();
      final String token = Base64.getEncoder()
          .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
      request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic " + token);
    }
    return execution.execute(request, body);
  }
}
