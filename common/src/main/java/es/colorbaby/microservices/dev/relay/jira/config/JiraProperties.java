package es.colorbaby.microservices.dev.relay.jira.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de conexión del cliente Jira. Se resuelve en local desde
 * application.yml/variables de entorno y en producción desde Vault usando
 * los mismos nombres de propiedad (prefijo maestro.jira).
 */
@Data
@ConfigurationProperties(prefix = "maestro.jira")
public class JiraProperties {

  /**
   * URL base del site de Jira, ej. https://empresa.atlassian.net.
   */
  private String baseUrl;

  /**
   * Email de la cuenta usada para Basic Auth en Jira Cloud (auth-mode = CLOUD_BASIC).
   */
  private String email;

  /**
   * API token (Jira Cloud) o Personal Access Token (Jira Server/DC), según auth-mode.
   */
  private String apiToken;

  /**
   * Modo de autenticación: CLOUD_BASIC (Jira Cloud) o SERVER_PAT (Jira Server/DC).
   */
  private JiraAuthMode authMode = JiraAuthMode.CLOUD_BASIC;

  /**
   * Timeout de conexión en milisegundos.
   */
  private int connectTimeoutMs = 5000;

  /**
   * Timeout de lectura en milisegundos.
   */
  private int readTimeoutMs = 10000;

  /**
   * Nº máximo de intentos (incluido el primero) ante fallos transitorios de
   * Jira (errores de red o 5xx). Los errores 4xx no se reintentan.
   */
  private int retryMaxAttempts = 3;

  /**
   * Espera entre reintentos, en milisegundos.
   */
  private long retryWaitMs = 500;

  /**
   * Tamaño de página usado en las búsquedas por JQL.
   */
  private int searchPageSize = 50;

  /**
   * Tope de seguridad de páginas a recorrer en una búsqueda por JQL, para no
   * quedar atrapados paginando indefinidamente.
   */
  private int searchMaxPages = 20;
}
