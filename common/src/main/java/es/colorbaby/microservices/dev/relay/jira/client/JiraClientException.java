package es.colorbaby.microservices.dev.relay.jira.client;

/**
 * Error de comunicación con la REST API de Jira (fallo de red, respuesta
 * inesperada, etc). El consumidor de la librería decide cómo tratarla.
 */
public class JiraClientException extends RuntimeException {

  public JiraClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public JiraClientException(String message) {
    super(message);
  }
}
