package es.colorbaby.microservices.dev.relay.infra.client;

/**
 * Mirar qué pasa en la máquina donde corre un contenedor. Es lo que permite diagnosticar un
 * despliegue fallido más allá de "el job salió en rojo".
 *
 * <p><b>Deliberadamente NO existe un método "ejecuta este comando".</b> El catálogo es cerrado:
 * leer logs y ver el estado de un contenedor. El agente elige entre estas acciones, no compone
 * órdenes de shell — es la regla de "herramientas tipadas, nunca terminal en producción".
 */
public interface InfraClient {

  /**
   * Últimas líneas del log de un contenedor.
   *
   * @param host      máquina destino (ej. {@code devops03})
   * @param container nombre del contenedor
   * @param lines     cuántas líneas del final
   * @return el log, o vacío si no se pudo leer
   */
  String containerLogs(String host, String container, int lines);

  /**
   * Estado del contenedor (si está arriba, cuántas veces ha reiniciado, código de salida). Un
   * contenedor en bucle de reinicio se ve aquí antes que en ningún log.
   *
   * @param host      máquina destino
   * @param container nombre del contenedor
   * @return una línea con el estado, o vacío si no existe
   */
  String containerStatus(String host, String container);
}
