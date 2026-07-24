package es.colorbaby.microservices.dev.relay.jenkins.client;

import java.util.Map;
import java.util.Optional;

/**
 * Cliente contra la API REST de Jenkins. En Colorbaby los jobs son <b>pipelines con nombre fijo y
 * parámetros</b> (no multibranch): {@code back_pipeline}/{@code front_pipeline}/
 * {@code back_library_pipeline} compilan un REPOSITORY+BRANCH y suben la imagen a Harbor, y
 * {@code deploy_pipeline} despliega un SERVICE+VERSION en un ENVIRONMENT. Por eso el cliente sabe
 * disparar jobs parametrizados y seguir su rastro (cola → build → resultado → consola).
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
   * Lanza un build parametrizado ({@code POST .../buildWithParameters}). Jenkins no devuelve el
   * build: devuelve un item en COLA, que tarda en convertirse en build. Por eso aquí se devuelve la
   * URL de ese item y luego se resuelve con {@link #resolveQueuedBuild(String)}.
   *
   * @param jobPath    ruta del job tras la URL base (ej. {@code job/back_pipeline})
   * @param parameters parámetros del job (ej. REPOSITORY, BRANCH, o SERVICE/VERSION/ENVIRONMENT)
   * @return URL del item encolado
   */
  String triggerBuild(String jobPath, Map<String, String> parameters);

  /**
   * Número de build de un item en cola. Vacío mientras Jenkins no lo haya sacado de la cola (puede
   * tardar: agente ocupado, quiet period…), lo que NO es un error sino "todavía no".
   *
   * @param queueUrl URL devuelta por {@link #triggerBuild}
   * @return número del build ya arrancado, o vacío si sigue en cola
   */
  Optional<Integer> resolveQueuedBuild(String queueUrl);

  /**
   * Un build concreto por número.
   *
   * @param jobPath     ruta del job tras la URL base
   * @param buildNumber número de build
   * @return el build, o vacío si no existe
   */
  Optional<Build> getBuild(String jobPath, int buildNumber);

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
