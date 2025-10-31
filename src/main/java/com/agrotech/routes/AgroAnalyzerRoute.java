package com.agrotech.routes;

import org.apache.camel.builder.RouteBuilder;
import java.util.Map;

public class AgroAnalyzerRoute extends RouteBuilder {
  @Override
  public void configure() {

    from("direct:agro.intake")
      .routeId("agro-analyzer")
      .unmarshal().json(Map.class)
      .process(e -> {
        @SuppressWarnings("unchecked")
        Map<String,Object> row = e.getIn().getBody(Map.class);

        String id = String.valueOf(row.get("id_sensor")).trim();
        String fechaRaw = String.valueOf(row.get("fecha")).trim();
        String fecha = fechaRaw.contains("T") ? fechaRaw.replace("T", " ")
                                              : (fechaRaw + " 00:00:00");

        double humedad = Double.parseDouble(String.valueOf(row.get("humedad")).trim());
        double temperatura = Double.parseDouble(String.valueOf(row.get("temperatura")).trim());

        String sql = String.format(
          "INSERT INTO lecturas (id_sensor, fecha, humedad, temperatura) " +
          "VALUES ('%s','%s',%s,%s)",
          id, fecha, humedad, temperatura
        );

        e.getIn().setBody(sql);
      })
      .to("jdbc:dataSource")
      .log("[DB] Insertada lectura en MySQL");
  }
}
