package es.colorbaby.microservices.dev.relay.infra.client;

/** Error accediendo a una máquina de la infraestructura. */
public class InfraClientException extends RuntimeException {

  public InfraClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
