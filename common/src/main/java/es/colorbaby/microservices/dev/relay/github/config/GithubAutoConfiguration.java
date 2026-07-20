package es.colorbaby.microservices.dev.relay.github.config;

import es.colorbaby.microservices.dev.relay.github.client.GithubClient;
import es.colorbaby.microservices.dev.relay.github.client.GithubClientImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

  @Bean
  @ConditionalOnMissingBean
  public GithubClient githubClient(final GithubProperties githubProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(githubProperties.getConnectTimeoutMs());
    requestFactory.setReadTimeout(githubProperties.getReadTimeoutMs());

    RestTemplate githubRestTemplate = new RestTemplate(requestFactory);
    githubRestTemplate.getInterceptors().add(new GithubAuthInterceptor(githubProperties));
    return new GithubClientImpl(githubRestTemplate, githubProperties);
  }
}
