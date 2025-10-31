package com.agrotech.service;

public class ServicioAnalitica {
  public String getUltimoValor(String id) {
    return String.format(
      "{\"id\":\"%s\",\"humedad\":48,\"temperatura\":26.7,\"fecha\":\"2025-05-22 10:30:00\"}",
      id
    );
  }
}
