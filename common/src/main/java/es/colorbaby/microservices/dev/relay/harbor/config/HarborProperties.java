package es.colorbaby.microservices.dev.relay.harbor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión con Harbor (prefijo {@code maestro.harbor}), el registro de imágenes de Colorbaby.
 * Sixai lo usa para saber qué VERSION produjo un build: el pipeline de Jenkins sube la imagen
 * etiquetada con una versión semántica, y el job de deploy necesita esa etiqueta.
 */
@Data
@ConfigurationProperties(prefix = "maestro.harbor")
public class HarborProperties {

  /** URL base de Harbor (ej. {@code https://harbor.colorbaby.es}), sin la barra final. */
  private String baseUrl = "https://harbor.colorbaby.es";

  /** Proyecto de Harbor donde viven las imágenes. */
  private String project = "colorbaby";

  /** Usuario de Harbor. */
  private String user = "";

  /** Contraseña o token del usuario. */
  private String password = "";

  /** Timeout de conexión en milisegundos. */
  private int connectTimeoutMs = 5000;

  /** Timeout de lectura en milisegundos. */
  private int readTimeoutMs = 15000;
}
