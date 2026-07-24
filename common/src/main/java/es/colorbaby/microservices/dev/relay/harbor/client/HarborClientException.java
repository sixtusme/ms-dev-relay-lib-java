package es.colorbaby.microservices.dev.relay.harbor.client;

/** Error hablando con Harbor. */
public class HarborClientException extends RuntimeException {

  public HarborClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
