package es.colorbaby.microservices.dev.relay.harbor.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Añade autenticación básica a cada petición a Harbor. Solo si hay usuario configurado. */
@RequiredArgsConstructor
public class HarborAuthInterceptor implements ClientHttpRequestInterceptor {

  private final HarborProperties properties;

  @Override
  public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
      final ClientHttpRequestExecution execution) throws IOException {
    if (properties.getUser() != null && !properties.getUser().isBlank()) {
      final String raw = properties.getUser() + ":" + properties.getPassword();
      final String token = Base64.getEncoder()
          .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
      request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic " + token);
    }
    return execution.execute(request, body);
  }
}
