package es.colorbaby.microservices.dev.relay.jira.config;

/**
 * Modo de autenticación contra la REST API de Jira.
 */
public enum JiraAuthMode {

  /**
   * Jira Cloud: Basic Auth con email + API token.
   */
  CLOUD_BASIC,

  /**
   * Jira Server/Data Center: Bearer con Personal Access Token.
   */
  SERVER_PAT
}
