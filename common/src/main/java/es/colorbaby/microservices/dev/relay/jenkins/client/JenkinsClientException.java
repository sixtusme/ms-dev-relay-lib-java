package es.colorbaby.microservices.dev.relay.jenkins.client;

/** Error hablando con Jenkins. */
public class JenkinsClientException extends RuntimeException {

  public JenkinsClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
