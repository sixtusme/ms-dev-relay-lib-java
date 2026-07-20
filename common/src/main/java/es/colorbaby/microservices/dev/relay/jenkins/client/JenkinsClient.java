package es.colorbaby.microservices.dev.relay.jenkins.client;

import java.util.Optional;

/**
 * Cliente de solo-lectura contra la API REST de Jenkins, lo justo para OBSERVAR un build (Fase 1):
 * consultar el último build de un job y descargar su consola. No dispara ni modifica nada.
 */
public interface JenkinsClient {

  /**
   * Último build de un job. Vacío si el job aún no existe (p. ej. una rama recién creada que el
   * multibranch todavía no ha indexado): un 404 se trata como "todavía no listo", no como error.
   *
   * @param jobPath ruta del job tras la URL base (ej. {@code job/ms-pim-java/job/sixai%2FUSA-1-2})
   * @return el último build, o vacío si el job no existe todavía
   */
  Optional<Build> getLastBuild(String jobPath);

  /**
   * Consola (texto plano) de un build concreto.
   *
   * @param jobPath     ruta del job tras la URL base
   * @param buildNumber número de build
   * @return el log de consola completo
   */
  String getConsoleLog(String jobPath, int buildNumber);

  /**
   * Un build de Jenkins. {@code result} es null mientras {@code building} es true; al terminar vale
   * {@code SUCCESS}, {@code FAILURE}, {@code UNSTABLE} o {@code ABORTED}.
   */
  record Build(int number, String result, boolean building, String url) {

    /** True si el build ha terminado (no está en curso y ya tiene resultado). */
    public boolean finished() {
      return !building && result != null;
    }

    /** True si terminó con éxito. */
    public boolean success() {
      return "SUCCESS".equals(result);
    }
  }
}
