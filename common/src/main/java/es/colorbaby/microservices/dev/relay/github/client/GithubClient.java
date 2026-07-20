package es.colorbaby.microservices.dev.relay.github.client;

import java.util.List;
import java.util.Optional;

/**
 * Cliente contra la API REST de GitHub: lo justo para que sixai arranque una PR (leer ramas, crear
 * rama, commitear, abrir la PR) y para que el coder lea el repo y escriba cambios (árbol, contenido
 * de ficheros, upsert). Sin lógica de negocio.
 */
public interface GithubClient {

  /** Rama por defecto de un repo (nombre + SHA de su último commit). Se usa para PROD (main/master). */
  DefaultBranch getDefaultBranch(String repo);

  /**
   * SHA del último commit de una rama concreta (ej. {@code develop}), para ramificar desde ella.
   *
   * @param repo   nombre del repo (sin la org)
   * @param branch nombre de la rama (sin refs/heads/)
   * @return el SHA de la punta de esa rama
   */
  String getBranchSha(String repo, String branch);

  /**
   * Crea una rama nueva en un repo apuntando a un SHA.
   *
   * @param repo   nombre del repo (sin la org)
   * @param branch nombre de la rama nueva (sin refs/heads/)
   * @param sha    commit al que apunta
   */
  void createBranch(String repo, String branch, String sha);

  /**
   * Crea un fichero nuevo en una rama, generando un commit. Atajo de
   * {@link #putFile(String, String, String, String, String, String)} con {@code sha = null}.
   *
   * @param repo    nombre del repo
   * @param branch  rama sobre la que commitear
   * @param path    ruta del fichero en el repo
   * @param content contenido en texto plano (se codifica en base64 aquí)
   * @param message mensaje de commit
   */
  default void putFile(String repo, String branch, String path, String content, String message) {
    putFile(repo, branch, path, content, message, null);
  }

  /**
   * Crea o actualiza un fichero en una rama, generando un commit. Para actualizar uno existente,
   * GitHub exige el SHA del blob actual; para crear uno nuevo, {@code sha} debe ser null.
   *
   * @param repo    nombre del repo
   * @param branch  rama sobre la que commitear
   * @param path    ruta del fichero en el repo
   * @param content contenido en texto plano (se codifica en base64 aquí)
   * @param message mensaje de commit
   * @param sha     SHA del blob actual si se actualiza; null si se crea
   */
  void putFile(String repo, String branch, String path, String content, String message, String sha);

  /**
   * Rutas de todos los ficheros (blobs) de una rama, recursivo. Le da al coder el mapa del repo.
   *
   * @param repo nombre del repo
   * @param ref  rama o SHA
   * @return lista de rutas (puede ser grande; el llamante la acota)
   */
  List<String> listPaths(String repo, String ref);

  /**
   * Contenido (texto ya decodificado) y SHA de un fichero en una rama. Vacío si no existe (404).
   *
   * @param repo nombre del repo
   * @param ref  rama o SHA
   * @param path ruta del fichero
   * @return contenido + sha, o vacío si no existe
   */
  Optional<FileContent> getFileContent(String repo, String ref, String path);

  /**
   * Abre una pull request.
   *
   * @param repo  nombre del repo
   * @param head  rama origen
   * @param base  rama destino
   * @param title título
   * @param body  descripción
   * @param draft si se abre como borrador
   * @return número y URL de la PR
   */
  PullRequest createPullRequest(
      String repo, String head, String base, String title, String body, boolean draft);

  /** Rama por defecto de un repo. */
  record DefaultBranch(String name, String sha) {
  }

  /** Pull request creada. */
  record PullRequest(int number, String url, String head) {
  }

  /** Contenido (texto decodificado) de un fichero y el SHA de su blob (necesario para actualizarlo). */
  record FileContent(String content, String sha) {
  }
}
