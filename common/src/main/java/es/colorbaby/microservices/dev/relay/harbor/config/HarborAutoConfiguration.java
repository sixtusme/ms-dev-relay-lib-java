package es.colorbaby.microservices.dev.relay.harbor.config;

import es.colorbaby.microservices.dev.relay.harbor.client.HarborClient;
import es.colorbaby.microservices.dev.relay.harbor.client.HarborClientImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Autoconfiguración del cliente Harbor: basta añadir la dependencia y las propiedades
 * {@code maestro.harbor.*}.
 *
 * <p>El {@link RestTemplate} se construye <b>privado</b> dentro del bean del cliente y NO se expone,
 * para no chocar con el {@code jiraRestTemplate} ({@code @ConditionalOnMissingBean}).
 */
@AutoConfiguration
@EnableConfigurationProperties(HarborProperties.class)
public class HarborAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public HarborClient harborClient(final HarborProperties harborProperties) {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(harborProperties.getConnectTimeoutMs());
    requestFactory.setReadTimeout(harborProperties.getReadTimeoutMs());

    final RestTemplate harborRestTemplate = new RestTemplate(requestFactory);
    harborRestTemplate.getInterceptors().add(new HarborAuthInterceptor(harborProperties));
    return new HarborClientImpl(harborRestTemplate, harborProperties);
  }
}
