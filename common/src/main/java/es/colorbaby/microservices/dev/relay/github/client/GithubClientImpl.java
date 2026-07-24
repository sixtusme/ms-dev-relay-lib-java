package es.colorbaby.microservices.dev.relay.github.client;

import es.colorbaby.microservices.dev.relay.github.config.GithubProperties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementación de {@link GithubClient} sobre {@link RestTemplate}. Traduce cada operación a la
 * llamada REST de GitHub correspondiente; la autenticación la pone el interceptor del RestTemplate.
 */
@Slf4j
public class GithubClientImpl implements GithubClient {

  private final RestTemplate githubRestTemplate;
  private final GithubProperties properties;

  public GithubClientImpl(final RestTemplate githubRestTemplate, final GithubProperties properties) {
    this.githubRestTemplate = githubRestTemplate;
    this.properties = properties;
  }

  @Override
  public DefaultBranch getDefaultBranch(final String repo) {
    Map<?, ?> repoInfo = execute(
        () -> githubRestTemplate.getForObject(repoUrl(repo), Map.class),
        "leyendo el repo " + repo);
    String branch = str(repoInfo == null ? null : repoInfo.get("default_branch"));
    return new DefaultBranch(branch, getBranchSha(repo, branch));
  }

  @Override
  public String getBranchSha(final String repo, final String branch) {
    Map<?, ?> ref = execute(
        () -> githubRestTemplate.getForObject(
            repoUrl(repo) + "/git/ref/heads/" + branch, Map.class),
        "leyendo la rama " + branch + " de " + repo);
    Object object = ref == null ? null : ref.get("object");
    return object instanceof Map<?, ?> map ? str(map.get("sha")) : null;
  }

  @Override
  public void createBranch(final String repo, final String branch, final String sha) {
    Map<String, Object> body = Map.of("ref", "refs/heads/" + branch, "sha", sha);
    execute(
        () -> githubRestTemplate.postForObject(repoUrl(repo) + "/git/refs", body, Map.class),
        "creando la rama " + branch + " en " + repo);
  }

  @Override
  public void putFile(
      final String repo, final String branch, final String path,
      final String content, final String message, final String sha) {
    String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    Map<String, Object> body = new HashMap<>();
    body.put("message", message);
    body.put("content", encoded);
    body.put("branch", branch);
    if (sha != null && !sha.isBlank()) {
      body.put("sha", sha);
    }
    execute(() -> {
      githubRestTemplate.put(repoUrl(repo) + "/contents/" + path, body);
      return null;
    }, "escribiendo el fichero " + path + " en " + repo);
  }

  @Override
  public List<String> listPaths(final String repo, final String ref) {
    Map<?, ?> tree = execute(
        () -> githubRestTemplate.getForObject(
            repoUrl(repo) + "/git/trees/" + ref + "?recursive=1", Map.class),
        "leyendo el árbol de " + repo + "@" + ref);
    Object entries = tree == null ? null : tree.get("tree");
    List<String> paths = new ArrayList<>();
    if (entries instanceof List<?> list) {
      for (Object entry : list) {
        if (entry instanceof Map<?, ?> node && "blob".equals(str(node.get("type")))) {
          String path = str(node.get("path"));
          if (path != null) {
            paths.add(path);
          }
        }
      }
    }
    return paths;
  }

  @Override
  public Optional<FileContent> getFileContent(
      final String repo, final String ref, final String path) {
    try {
      Map<?, ?> file = githubRestTemplate.getForObject(
          repoUrl(repo) + "/contents/" + path + "?ref=" + ref, Map.class);
      if (file == null) {
        return Optional.empty();
      }
      String encoded = str(file.get("content"));
      String decoded = encoded == null ? "" : new String(
          Base64.getMimeDecoder().decode(encoded.replaceAll("\\s", "")), StandardCharsets.UTF_8);
      return Optional.of(new FileContent(decoded, str(file.get("sha"))));
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    } catch (RestClientException e) {
      throw new GithubClientException("Error leyendo " + path + " de " + repo, e);
    }
  }

  @Override
  public PullRequest createPullRequest(
      final String repo, final String head, final String base,
      final String title, final String body, final boolean draft) {
    Map<String, Object> request = Map.of(
        "title", title, "head", head, "base", base, "body", body, "draft", draft);
    Map<?, ?> response = execute(
        () -> githubRestTemplate.postForObject(repoUrl(repo) + "/pulls", request, Map.class),
        "abriendo la PR en " + repo);
    int number = response != null && response.get("number") instanceof Number n ? n.intValue() : 0;
    String url = response == null ? null : str(response.get("html_url"));
    return new PullRequest(number, url, head);
  }

  @Override
  public List<PullRequest> listPullRequests(final String repo, final String baseBranch,
      final String state) {
    final String effectiveState = state == null || state.isBlank() ? "open" : state;
    final String url = baseBranch == null || baseBranch.isBlank()
        ? repoUrl(repo) + "/pulls?state=" + effectiveState
        : repoUrl(repo) + "/pulls?state=" + effectiveState + "&base=" + baseBranch;
    final List<?> result = execute(
        () -> githubRestTemplate.getForObject(url, List.class),
        "listando PRs (" + effectiveState + ") de " + repo);
    final List<PullRequest> prs = new ArrayList<>();
    if (result != null) {
      for (final Object item : result) {
        if (item instanceof Map<?, ?> pr) {
          final int number = pr.get("number") instanceof Number n ? n.intValue() : 0;
          final String prUrl = str(pr.get("html_url"));
          final String head = pr.get("head") instanceof Map<?, ?> h ? str(h.get("ref")) : null;
          prs.add(new PullRequest(number, prUrl, head));
        }
      }
    }
    return prs;
  }

  @Override
  public void mergeBranches(final String repo, final String base, final String head,
      final String message) {
    final Map<String, Object> body = Map.of("base", base, "head", head, "commit_message", message);
    execute(
        () -> githubRestTemplate.postForObject(repoUrl(repo) + "/merges", body, Map.class),
        "mergeando " + head + " en " + base + " de " + repo);
  }

  @Override
  public void mergePullRequest(final String repo, final int number, final String method) {
    final Map<String, Object> body = Map.of("merge_method", method == null ? "squash" : method);
    execute(() -> {
      githubRestTemplate.put(repoUrl(repo) + "/pulls/" + number + "/merge", body);
      return null;
    }, "mergeando la PR #" + number + " de " + repo);
  }

  private String repoUrl(final String repo) {
    return properties.getBaseUrl() + "/repos/" + properties.getOrg() + "/" + repo;
  }

  private <T> T execute(final Supplier<T> call, final String what) {
    try {
      return call.get();
    } catch (RestClientException e) {
      log.error("Error {}: {}", what, e.getMessage());
      throw new GithubClientException("Error " + what, e);
    }
  }

  private static String str(final Object value) {
    return value == null ? null : value.toString();
  }
}
