package com.agrotech.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import java.util.Map;
import java.util.LinkedHashMap;

public class FileTransferRoute extends RouteBuilder {
  @Override
  public void configure() {

    // CSV → List<Map<String,Object>>
    CsvDataFormat csv = new CsvDataFormat()
        .setUseMaps(true)
        .setSkipHeaderRecord(true)     // saltar cabecera
        .setIgnoreEmptyLines(true);

    from("file:{{agrotech.inbox}}?include=.*\\.csv"
       + "&move={{agrotech.outbox}}/${file:name}.done"
       + "&moveFailed={{agrotech.outbox}}/error/${file:name}.${date:now:yyyyMMddHHmmss}.failed")
      .routeId("file-transfer")
      .log("[FILE] Leyendo ${file:name}")
      .convertBodyTo(String.class)      // GenericFile -> String (evita leaks)
      .unmarshal(csv)                   // List<Map<String,Object>>
      .split(body())
        .process(e -> {
          @SuppressWarnings("unchecked")
          Map<String,Object> raw = e.getIn().getBody(Map.class);

          // normaliza llaves: trim + lower-case
          Map<String,Object> row = new LinkedHashMap<>();
          for (Map.Entry<?,?> en : raw.entrySet()) {
            String k = String.valueOf(en.getKey()).trim().toLowerCase();
            row.put(k, en.getValue());
          }

          // valida campos mínimos
          String[] req = {"id_sensor","fecha","humedad","temperatura"};
          for (String r : req) {
            Object v = row.get(r);
            if (v == null || String.valueOf(v).isBlank()) {
              throw new IllegalArgumentException("CSV inválido: falta '" + r + "' en fila: " + raw);
            }
          }

          e.getIn().setBody(row);
        })
        .marshal().json()
        .to("direct:agro.intake")
        .log("[FILE] Fila enviada a AgroAnalyzer: ${body}")
      .end()
      .log("[FILE] Archivo ${file:name} movido a outbox/");
  }
}
