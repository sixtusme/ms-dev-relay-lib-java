package es.colorbaby.microservices.dev.relay.harbor.client;

import es.colorbaby.microservices.dev.relay.harbor.config.HarborProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementación de {@link HarborClient} sobre la API v2.0 de Harbor. Pide los artefactos ordenados
 * por fecha de subida descendente ({@code sort=-push_time}), así la primera etiqueta que aparece es
 * la que acaba de publicar el build.
 */
@Slf4j
public class HarborClientImpl implements HarborClient {

  private static final int PAGE_SIZE = 20;

  /** Tope de CVE que se nombran; con unos pocos ya se puede actuar, la lista entera es ruido. */
  private static final int MAX_CVES = 8;

  private final RestTemplate harborRestTemplate;
  private final HarborProperties properties;

  public HarborClientImpl(final RestTemplate harborRestTemplate,
      final HarborProperties properties) {
    this.harborRestTemplate = harborRestTemplate;
    this.properties = properties;
  }

  @Override
  public List<String> listTags(final String repository) {
    final URI uri = URI.create(base() + "/api/v2.0/projects/" + encode(properties.getProject())
        + "/repositories/" + encode(repository)
        + "/artifacts?page_size=" + PAGE_SIZE + "&with_tag=true&sort=-push_time");
    final List<?> artifacts;
    try {
      artifacts = harborRestTemplate.getForObject(uri, List.class);
    } catch (HttpClientErrorException.NotFound e) {
      return List.of();
    } catch (RestClientException e) {
      throw new HarborClientException("Error leyendo las imágenes de " + repository, e);
    }
    if (artifacts == null) {
      return List.of();
    }
    final List<String> tags = new ArrayList<>();
    for (final Object artifact : artifacts) {
      if (artifact instanceof Map<?, ?> item && item.get("tags") instanceof List<?> tagList) {
        for (final Object tag : tagList) {
          if (tag instanceof Map<?, ?> t && t.get("name") != null) {
            tags.add(t.get("name").toString());
          }
        }
      }
    }
    return tags;
  }

  @Override
  public Optional<String> latestTag(final String repository) {
    final List<String> tags = listTags(repository);
    return tags.isEmpty() ? Optional.empty() : Optional.of(tags.get(0));
  }

  @Override
  public Optional<ScanSummary> scan(final String repository, final String tag) {
    final URI uri = URI.create(base() + "/api/v2.0/projects/" + encode(properties.getProject())
        + "/repositories/" + encode(repository) + "/artifacts/" + encode(tag)
        + "?with_scan_overview=true");
    final Map<?, ?> artifact;
    try {
      artifact = harborRestTemplate.getForObject(uri, Map.class);
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    } catch (RestClientException e) {
      throw new HarborClientException("Error leyendo el escaneo de " + repository + ":" + tag, e);
    }
    if (artifact == null || !(artifact.get("scan_overview") instanceof Map<?, ?> overview)) {
      return Optional.empty();
    }
    // scan_overview viene indexado por mime-type del informe; solo interesa el primero.
    final Object report = overview.values().stream().findFirst().orElse(null);
    if (!(report instanceof Map<?, ?> scan)) {
      return Optional.empty();
    }
    return Optional.of(new ScanSummary(str(scan.get("scan_status")), severities(scan),
        topCves(repository, tag)));
  }

  private static Map<String, Integer> severities(final Map<?, ?> scan) {
    final Map<String, Integer> counts = new LinkedHashMap<>();
    if (scan.get("summary") instanceof Map<?, ?> summary
        && summary.get("summary") instanceof Map<?, ?> bySeverity) {
      bySeverity.forEach((key, value) -> {
        if (key != null && value instanceof Number number) {
          counts.put(key.toString(), number.intValue());
        }
      });
    }
    return counts;
  }

  /** Algunos CVE concretos: sin nombres, el diagnóstico no serviría para actuar. */
  private List<String> topCves(final String repository, final String tag) {
    final URI uri = URI.create(base() + "/api/v2.0/projects/" + encode(properties.getProject())
        + "/repositories/" + encode(repository) + "/artifacts/" + encode(tag)
        + "/additions/vulnerabilities");
    final List<String> cves = new ArrayList<>();
    try {
      final Map<?, ?> payload = harborRestTemplate.getForObject(uri, Map.class);
      if (payload == null) {
        return cves;
      }
      final Object report = payload.values().stream().findFirst().orElse(null);
      if (report instanceof Map<?, ?> data
          && data.get("vulnerabilities") instanceof List<?> vulnerabilities) {
        for (final Object item : vulnerabilities) {
          if (item instanceof Map<?, ?> vulnerability
              && "Critical".equalsIgnoreCase(str(vulnerability.get("severity")))) {
            cves.add(str(vulnerability.get("id")) + " (" + str(vulnerability.get("package")) + ")");
          }
          if (cves.size() >= MAX_CVES) {
            break;
          }
        }
      }
    } catch (RestClientException e) {
      log.debug("No se pudo leer el detalle de vulnerabilidades: {}", e.getMessage());
    }
    return cves;
  }

  private String base() {
    final String url = properties.getBaseUrl();
    return url == null ? "" : url.replaceAll("/+$", "");
  }

  private static String encode(final String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private static String str(final Object value) {
    return value == null ? null : value.toString();
  }
}
