package es.colorbaby.microservices.dev.relay.jira.config;

import es.colorbaby.microservices.dev.relay.jira.client.JiraClient;
import es.colorbaby.microservices.dev.relay.jira.client.JiraClientImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Autoconfiguración Spring Boot del cliente Jira: basta con añadir esta
 * dependencia y las propiedades maestro.jira.* para tener {@link JiraClient}
 * disponible como bean, sin declarar nada a mano.
 */
@AutoConfiguration
@EnableConfigurationProperties(JiraProperties.class)
@ConditionalOnProperty(prefix = "maestro.jira", name = "base-url")
public class JiraAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public RestTemplate jiraRestTemplate(JiraProperties jiraProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(jiraProperties.getConnectTimeoutMs());
    requestFactory.setReadTimeout(jiraProperties.getReadTimeoutMs());

    RestTemplate restTemplate = new RestTemplate(requestFactory);
    ClientHttpRequestInterceptor authInterceptor = new JiraAuthInterceptor(jiraProperties);
    restTemplate.getInterceptors().add(authInterceptor);
    return restTemplate;
  }

  @Bean
  @ConditionalOnMissingBean
  public JiraClient jiraClient(RestTemplate jiraRestTemplate, JiraProperties jiraProperties) {
    return new JiraClientImpl(jiraRestTemplate, jiraProperties);
  }
}
