package es.colorbaby.microservices.dev.relay.infra.config;

import es.colorbaby.microservices.dev.relay.infra.client.InfraClient;
import es.colorbaby.microservices.dev.relay.infra.client.SshInfraClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguración del acceso a las máquinas: basta con las propiedades {@code maestro.infra.*}.
 * No hay RestTemplate aquí, así que no aplica la precaución de los otros clientes.
 */
@AutoConfiguration
@EnableConfigurationProperties(InfraProperties.class)
public class InfraAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public InfraClient infraClient(final InfraProperties infraProperties) {
    return new SshInfraClient(infraProperties);
  }
}
