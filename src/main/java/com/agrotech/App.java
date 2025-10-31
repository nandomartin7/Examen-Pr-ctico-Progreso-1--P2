package com.agrotech;

import org.apache.camel.main.Main;
import org.apache.commons.dbcp2.BasicDataSource;

public class App {
  public static void main(String[] args) throws Exception {
    Main main = new Main();

    // DataSource MySQL (bean name: dataSource)
    BasicDataSource ds = new BasicDataSource();
    ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
    ds.setUrl("jdbc:mysql://localhost:3306/agrotech_db"
        + "?allowPublicKeyRetrieval=true"
        + "&useSSL=false"
        + "&serverTimezone=UTC");
    ds.setUsername("agrotech");
    ds.setPassword("agrotech123");
    ds.setInitialSize(1);
    main.bind("dataSource", ds);

    // Rutas
    main.configure().addRoutesBuilder(new com.agrotech.routes.FileTransferRoute());
    main.configure().addRoutesBuilder(new com.agrotech.routes.AgroAnalyzerRoute());
    main.configure().addRoutesBuilder(new com.agrotech.routes.RpcRoutes());

    main.run();
  }
}
