package es.colorbaby.microservices.dev.relay.jenkins.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión con Jenkins (prefijo {@code maestro.jenkins}). Solo el "cómo hablar con Jenkins"; el
 * "qué hacer" (activar, dry-run, mapeo de jobs, sondeo) vive en el servicio.
 */
@Data
@ConfigurationProperties(prefix = "maestro.jenkins")
public class JenkinsProperties {

  /** URL base de Jenkins (ej. {@code https://jenkins.colorbaby.es}), sin la barra final. */
  private String baseUrl = "";

  /** Usuario de Jenkins para la autenticación básica. */
  private String user = "";

  /** API token del usuario (no la contraseña). */
  private String apiToken = "";

  /** Timeout de conexión en milisegundos. */
  private int connectTimeoutMs = 5000;

  /** Timeout de lectura en milisegundos. */
  private int readTimeoutMs = 15000;
}
