package es.colorbaby.microservices.dev.relay.github.client;

/** Error al hablar con la API de GitHub. */
public class GithubClientException extends RuntimeException {

  public GithubClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
