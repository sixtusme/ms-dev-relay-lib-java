package es.colorbaby.microservices.dev.relay.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Acceso a las máquinas donde corren los contenedores ({@code maestro.infra}). Solo el "cómo llegar
 * al host"; qué host toca cada entorno lo decide el servicio.
 *
 * <p>Es el acceso más delicado de sixai, así que la credencial debe ser de <b>solo lectura y mínimo
 * privilegio</b>: con poder leer logs y estado de contenedores basta.
 */
@Data
@ConfigurationProperties(prefix = "maestro.infra")
public class InfraProperties {

  /** Usuario SSH de los hosts. */
  private String username = "";

  /** Contraseña (si no se usa clave). */
  private String password = "";

  /** Ruta a la clave privada, preferible a la contraseña. */
  private String privateKeyPath;

  /** Ruta al known_hosts para verificar el host. */
  private String knownHostsPath;

  /** Verificación estricta del host. */
  private boolean strictHostChecking = false;

  private int port = 22;

  private int connectTimeoutMs = 10000;

  /** Tope de segundos que se espera a que un comando termine. */
  private int commandTimeoutSeconds = 30;
}
