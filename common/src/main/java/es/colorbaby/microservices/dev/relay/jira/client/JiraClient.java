package es.colorbaby.microservices.dev.relay.jira.client;

import es.colorbaby.microservices.dev.relay.openapi.model.JiraCommentDto;
import es.colorbaby.microservices.dev.relay.openapi.model.JiraIssueDto;
import es.colorbaby.microservices.dev.relay.openapi.model.JiraTransitionDto;
import java.util.List;

/**
 * Cliente reutilizable contra la REST API de Jira (v2/v3), sin lógica de
 * negocio ni de filtrado. Compatible con Jira Cloud (Basic email+API token)
 * y Jira Server/DC (Bearer PAT) según maestro.jira.auth-mode.
 */
public interface JiraClient {

  /**
   * Obtiene una issue por su key (ej. "COLORBABY-123").
   */
  JiraIssueDto getIssue(String issueKey);

  /**
   * Busca todas las issues que cumplen la JQL, recorriendo internamente la
   * paginación (por cursor en Jira Cloud, por offset en Server/DC) hasta el
   * tope de páginas configurado (maestro.jira.search-max-pages).
   */
  List<JiraIssueDto> searchIssuesByJql(String jql);

  /**
   * Lee todos los comentarios de una issue.
   */
  List<JiraCommentDto> getComments(String issueKey);

  /**
   * Añade un comentario en texto plano a una issue.
   */
  JiraCommentDto addComment(String issueKey, String plainTextBody);

  /**
   * Lista las transiciones de workflow disponibles para una issue en su
   * estado actual.
   */
  List<JiraTransitionDto> getTransitions(String issueKey);

  /**
   * Ejecuta una transición de workflow por su id.
   */
  void transitionIssue(String issueKey, String transitionId);

  /**
   * Ejecuta la transición cuyo estado destino coincide con el nombre dado
   * (resuelve el id internamente vía {@link #getTransitions(String)}).
   *
   * @throws JiraClientException si ninguna transición disponible lleva a ese estado.
   */
  void transitionIssueByStatusName(String issueKey, String targetStatusName);

  /**
   * Reasigna una issue al usuario indicado (accountId en Jira Cloud, username
   * en Jira Server/DC).
   */
  void assignIssue(String issueKey, String accountIdOrUsername);
}
