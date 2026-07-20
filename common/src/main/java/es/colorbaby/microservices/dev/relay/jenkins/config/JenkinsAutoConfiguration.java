package es.colorbaby.microservices.dev.relay.jenkins.config;

import es.colorbaby.microservices.dev.relay.jenkins.client.JenkinsClient;
import es.colorbaby.microservices.dev.relay.jenkins.client.JenkinsClientImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Autoconfiguración del cliente Jenkins: basta añadir la dependencia y las propiedades
 * {@code maestro.jenkins.*}.
 *
 * <p>El {@link RestTemplate} se construye <b>privado</b> dentro del bean del cliente y NO se expone:
 * si fuera un bean de tipo RestTemplate chocaría con el {@code jiraRestTemplate}
 * ({@code @ConditionalOnMissingBean}) y uno anularía al otro.
 */
@AutoConfiguration
@EnableConfigurationProperties(JenkinsProperties.class)
public class JenkinsAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public JenkinsClient jenkinsClient(final JenkinsProperties jenkinsProperties) {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(jenkinsProperties.getConnectTimeoutMs());
    requestFactory.setReadTimeout(jenkinsProperties.getReadTimeoutMs());

    final RestTemplate jenkinsRestTemplate = new RestTemplate(requestFactory);
    jenkinsRestTemplate.getInterceptors().add(new JenkinsAuthInterceptor(jenkinsProperties));
    return new JenkinsClientImpl(jenkinsRestTemplate, jenkinsProperties);
  }
}
