package es.colorbaby.microservices.dev.relay.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión con la API de GitHub (prefijo {@code maestro.github}). Solo el "cómo hablar con GitHub";
 * el "qué hacer" (activar, dry-run, mapeo de repos) vive en el servicio.
 */
@Data
@ConfigurationProperties(prefix = "maestro.github")
public class GithubProperties {

  /** URL base de la API de GitHub. */
  private String baseUrl = "https://api.github.com";

  /** Organización dueña de los repos (ej. {@code colorbaby}). */
  private String org = "colorbaby";

  /** Token de acceso con permisos de contents (escritura) y pull requests. */
  private String token = "";

  /** Timeout de conexión en milisegundos. */
  private int connectTimeoutMs = 5000;

  /** Timeout de lectura en milisegundos. */
  private int readTimeoutMs = 15000;
}
