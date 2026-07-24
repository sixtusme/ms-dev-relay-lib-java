package es.colorbaby.microservices.dev.relay.infra.client;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import es.colorbaby.microservices.dev.relay.infra.config.InfraProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación por SSH. Los comandos se <b>construyen aquí</b> a partir de plantillas fijas; lo
 * único que viene de fuera es el nombre del contenedor, que además se valida contra una expresión
 * estricta. Así no hay forma de colar una orden arbitraria aunque el nombre venga de configuración
 * o de un modelo.
 */
@Slf4j
@RequiredArgsConstructor
public class SshInfraClient implements InfraClient {

  /** Nombres de contenedor admitidos: lo que Docker permite y nada más. */
  private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}");

  private final InfraProperties properties;

  @Override
  public String containerLogs(final String host, final String container, final int lines) {
    final int tail = Math.max(1, Math.min(lines, 2000));
    return run(host, "docker logs --tail " + tail + " " + safe(container) + " 2>&1",
        "leyendo los logs de " + container);
  }

  @Override
  public String containerStatus(final String host, final String container) {
    return run(host,
        "docker inspect --format "
            + "'{{.State.Status}} restarts={{.RestartCount}} exit={{.State.ExitCode}}' "
            + safe(container),
        "consultando el estado de " + container);
  }

  /** Valida el nombre antes de que llegue a formar parte de ninguna orden. */
  private static String safe(final String container) {
    if (container == null || !SAFE_NAME.matcher(container).matches()) {
      throw new InfraClientException("Nombre de contenedor no válido: " + container, null);
    }
    return container;
  }

  private String run(final String host, final String command, final String what) {
    Session session = null;
    ChannelExec channel = null;
    try {
      session = openSession(host);
      channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand(command);
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      channel.setOutputStream(out);
      channel.setErrStream(out);
      try (InputStream in = channel.getInputStream()) {
        channel.connect(properties.getConnectTimeoutMs());
        waitFor(channel);
        drain(in, out);
      }
      return out.toString(StandardCharsets.UTF_8).strip();
    } catch (JSchException | IOException e) {
      log.warn("Error {} en {}: {}", what, host, e.getMessage());
      return "";
    } finally {
      if (channel != null) {
        channel.disconnect();
      }
      if (session != null) {
        session.disconnect();
      }
    }
  }

  private void waitFor(final ChannelExec channel) {
    final long deadline = System.currentTimeMillis()
        + properties.getCommandTimeoutSeconds() * 1000L;
    while (!channel.isClosed() && System.currentTimeMillis() < deadline) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  private static void drain(final InputStream in, final ByteArrayOutputStream out)
      throws IOException {
    final byte[] buffer = new byte[4096];
    while (in.available() > 0) {
      final int read = in.read(buffer);
      if (read < 0) {
        break;
      }
      out.write(buffer, 0, read);
    }
  }

  private Session openSession(final String host) throws JSchException {
    final JSch jsch = new JSch();
    if (properties.getPrivateKeyPath() != null && !properties.getPrivateKeyPath().isBlank()) {
      jsch.addIdentity(properties.getPrivateKeyPath());
    }
    if (properties.getKnownHostsPath() != null && !properties.getKnownHostsPath().isBlank()) {
      jsch.setKnownHosts(properties.getKnownHostsPath());
    }
    final Session session = jsch.getSession(properties.getUsername(), host, properties.getPort());
    if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
      session.setPassword(properties.getPassword());
    }
    final Properties config = new Properties();
    config.put("StrictHostKeyChecking", properties.isStrictHostChecking() ? "yes" : "no");
    session.setConfig(config);
    session.connect(properties.getConnectTimeoutMs());
    return session;
  }
}
