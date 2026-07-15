package es.colorbaby.microservices.dev.relay.jira.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye documentos ADF (Atlassian Document Format) para comentarios de Jira Cloud.
 *
 * <p>Solo cubre lo que necesita Sixai: texto, mención, párrafo, cita y el nodo raíz {@code doc}.
 * <b>Es exclusivo de Jira Cloud</b>: en Server/DC los comentarios son wiki markup y este formato no
 * aplica. Los nodos se devuelven como {@link Map} listos para serializar a JSON tal cual.
 */
public final class JiraAdf {

  private JiraAdf() {
  }

  /** Nodo de texto plano. */
  public static Map<String, Object> text(String value) {
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("type", "text");
    node.put("text", value == null ? "" : value);
    return node;
  }

  /**
   * Mención real que notifica al usuario y aparece clicable. {@code accountId} es el de Jira Cloud;
   * {@code displayName} solo alimenta el texto visible del pill (Jira lo resuelve por el id).
   */
  public static Map<String, Object> mention(String accountId, String displayName) {
    Map<String, Object> attrs = new LinkedHashMap<>();
    attrs.put("id", accountId);
    attrs.put("text", "@" + (displayName == null ? "" : displayName.strip()));

    Map<String, Object> node = new LinkedHashMap<>();
    node.put("type", "mention");
    node.put("attrs", attrs);
    return node;
  }

  /** Párrafo con los nodos inline dados (texto, menciones…). */
  public static Map<String, Object> paragraph(List<Map<String, Object>> inline) {
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("type", "paragraph");
    node.put("content", inline);
    return node;
  }

  /** Cita (blockquote) que envuelve los bloques dados (normalmente un párrafo). */
  public static Map<String, Object> blockquote(List<Map<String, Object>> blocks) {
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("type", "blockquote");
    node.put("content", blocks);
    return node;
  }

  /** Documento raíz ADF a partir de sus bloques (párrafos, citas…). */
  public static Map<String, Object> doc(List<Map<String, Object>> blocks) {
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("type", "doc");
    node.put("version", 1);
    node.put("content", blocks);
    return node;
  }
}