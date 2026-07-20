package es.colorbaby.microservices.dev.relay.github.config;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Añade el token y las cabeceras que exige la API de GitHub a toda petición saliente. */
@RequiredArgsConstructor
public class GithubAuthInterceptor implements ClientHttpRequestInterceptor {

  private final GithubProperties githubProperties;

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    HttpHeaders headers = request.getHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + githubProperties.getToken());
    headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
    headers.set("X-GitHub-Api-Version", "2022-11-28");
    return execution.execute(request, body);
  }
}
