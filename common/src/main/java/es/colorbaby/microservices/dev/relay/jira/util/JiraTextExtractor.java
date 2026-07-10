package es.colorbaby.microservices.dev.relay.jira.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;

/**
 * Extrae texto plano tanto de comentarios/descripciones en Atlassian Document
 * Format (ADF, objeto - Jira Cloud) como en texto plano (String - Jira
 * Server/DC), de forma transparente para el resto del código.
 */
public final class JiraTextExtractor {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JiraTextExtractor() {
  }

  /**
   * Extrae el texto plano de un campo de Jira (comentario, descripción...)
   * que puede venir como String (texto plano) o como objeto ADF.
   *
   * @param body valor del campo tal y como lo devuelve Jira/el modelo generado.
   * @return texto plano, o cadena vacía si {@code body} es {@code null} o no
   *     contiene texto reconocible.
   */
  public static String extractPlainText(Object body) {
    if (body == null) {
      return "";
    }
    if (body instanceof String text) {
      return text;
    }
    JsonNode node = OBJECT_MAPPER.valueToTree(body);
    StringBuilder result = new StringBuilder();
    appendAdfText(node, result);
    return result.toString().strip();
  }

  private static void appendAdfText(JsonNode node, StringBuilder result) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return;
    }
    if (node.isTextual()) {
      result.append(node.asText());
      return;
    }
    if (node.isArray()) {
      Iterator<JsonNode> elements = node.elements();
      while (elements.hasNext()) {
        appendAdfText(elements.next(), result);
      }
      return;
    }
    if (!node.isObject()) {
      return;
    }
    String type = node.path("type").asText("");
    if ("text".equals(type)) {
      result.append(node.path("text").asText(""));
      return;
    }
    if ("hardBreak".equals(type)) {
      result.append('\n');
      return;
    }
    appendAdfText(node.get("content"), result);
    if (isBlockType(type)) {
      result.append('\n');
    }
  }

  private static boolean isBlockType(String type) {
    return "paragraph".equals(type)
        || "listItem".equals(type)
        || "bulletList".equals(type)
        || "orderedList".equals(type)
        || "heading".equals(type)
        || "codeBlock".equals(type)
        || "blockquote".equals(type);
  }
}
