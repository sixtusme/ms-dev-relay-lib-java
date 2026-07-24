package es.colorbaby.microservices.dev.relay.harbor.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente de solo-lectura contra Harbor. Sirve para una cosa concreta: averiguar con qué VERSION
 * quedó etiquetada la imagen que acaba de construir Jenkins, porque el job de despliegue la pide.
 */
public interface HarborClient {

  /**
   * Etiquetas de una imagen, de la más reciente a la más antigua.
   *
   * @param repository nombre de la imagen dentro del proyecto (ej. {@code colorbaby-docs-java})
   * @return etiquetas ordenadas por fecha de subida descendente
   */
  List<String> listTags(String repository);

  /**
   * Última etiqueta subida de una imagen: la VERSION que acaba de producir el build.
   *
   * @param repository nombre de la imagen dentro del proyecto
   * @return la etiqueta más reciente, o vacío si la imagen no tiene ninguna
   */
  Optional<String> latestTag(String repository);

  /**
   * Resumen del escaneo de vulnerabilidades de una imagen. Es lo que explica por qué el gate de
   * Trivy tumbó un despliegue: sin esto, en Jenkins solo se ve que "falló el gate".
   *
   * @param repository nombre de la imagen dentro del proyecto
   * @param tag        etiqueta (versión) concreta
   * @return el resumen, o vacío si la imagen o su escaneo no existen
   */
  Optional<ScanSummary> scan(String repository, String tag);

  /**
   * Resultado del escaneo: en qué estado está y cuántas vulnerabilidades hay por severidad.
   *
   * @param status  estado del escaneo (Success, Error…)
   * @param counts  número de vulnerabilidades por severidad (Critical, High…)
   * @param topCves algunos CVE concretos, para poder nombrarlos en el diagnóstico
   */
  record ScanSummary(String status, Map<String, Integer> counts, List<String> topCves) {

    /** True si hay vulnerabilidades críticas, que es lo que bloquea el despliegue. */
    public boolean hasCritical() {
      return counts.getOrDefault("Critical", 0) > 0;
    }
  }
}
