package es.colorbaby.microservices.dev.relay.github.config;

import es.colorbaby.microservices.dev.relay.github.client.GithubClient;
import es.colorbaby.microservices.dev.relay.github.client.GithubClientImpl;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Autoconfiguración del cliente GitHub: basta añadir la dependencia y las propiedades
 * {@code maestro.github.*}.
 *
 * <p>El {@link RestTemplate} se construye <b>privado</b> dentro del bean del cliente y NO se expone:
 * si fuera un bean de tipo RestTemplate chocaría con el {@code jiraRestTemplate}
 * ({@code @ConditionalOnMissingBean}) y uno anularía al otro.
 */
@AutoConfiguration
@EnableConfigurationProperties(GithubProperties.class)
public class GithubAutoConfiguration {

  /**
   * OJO con la fábrica: tiene que ser la del {@code java.net.http.HttpClient}, no
   * {@code SimpleClientHttpRequestFactory}. Esa va sobre {@code HttpURLConnection}, que <b>no
   * admite PATCH</b> y falla con {@code ProtocolException: Invalid HTTP method}. Y PATCH hace falta:
   * es como GitHub mueve una rama ({@code PATCH /git/refs/heads/…}), que es el último paso del
   * commit atómico de varios ficheros.
   */
  @Bean
  @ConditionalOnMissingBean
  public GithubClient githubClient(final GithubProperties githubProperties) {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(githubProperties.getConnectTimeoutMs()))
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofMillis(githubProperties.getReadTimeoutMs()));

    RestTemplate githubRestTemplate = new RestTemplate(requestFactory);
    githubRestTemplate.getInterceptors().add(new GithubAuthInterceptor(githubProperties));
    return new GithubClientImpl(githubRestTemplate, githubProperties);
  }
}
