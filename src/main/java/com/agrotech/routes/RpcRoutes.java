package com.agrotech.routes;

import org.apache.camel.builder.RouteBuilder;
import com.agrotech.service.ServicioAnalitica;

public class RpcRoutes extends RouteBuilder {
  @Override
  public void configure() {

    // SERVIDOR (AgroAnalyzer)
    from("direct:rpc.obtenerUltimo")
      .routeId("rpc-servidor")
      .log("[SERVIDOR] Solicitud recibida para sensor ${header.id_sensor}")
      .setBody(header("id_sensor"))                 // el método recibe String id
      .bean(new ServicioAnalitica(), "getUltimoValor");

    // CLIENTE (FieldControl)
    from("direct:solicitarLectura")
      .routeId("rpc-cliente")
      .setHeader("id_sensor", simple("${body}"))
      .log("[CLIENTE] Solicitando lectura del sensor ${header.id_sensor}")
      .to("direct:rpc.obtenerUltimo")
      .log("[CLIENTE] Respuesta recibida: ${body}");
  }
}
