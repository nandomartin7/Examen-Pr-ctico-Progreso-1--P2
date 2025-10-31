# Evaluación Práctica – Agrotech (Camel + MySQL)

> **Objetivo**  
Implementar un flujo de integración **File Transfer → Procesamiento → Persistencia en MySQL**, más un **RPC simulado (síncrono)** usando **Apache Camel 4.x** en Java 17. El sistema lee un CSV de lecturas de sensores, transforma los datos y los inserta en una tabla `lecturas`. Se acompaña con logs de evidencia y una reflexión técnica (VIII).

---

## 🧱 Arquitectura y Patrones

- **File Transfer (Polling):** `file://inbox` con `include=.*\.csv` y `move=./outbox`  
- **Message Translator:** mapeo de columnas CSV → campos (id_sensor, fecha, humedad, temperatura)
- **Content Enricher / Validator:** limpieza y validación (manejo de `null`, tipos numéricos)
- **Data Sink (JDBC):** inserción en MySQL vía `camel-jdbc`
- **RPC simulado (síncrono):** `direct:solicitarLectura` ↔ `direct:rpc.obtenerUltimo`
- **Logs y Observabilidad:** `log:` en cada etapa para evidenciar funcionamiento

---

## 📂 Estructura del proyecto

```
evaluacion-practica-agrotech/
├─ pom.xml
├─ application.properties
├─ inbox/                # Coloca aquí sensores.csv
├─ outbox/               # El archivo procesado se mueve aquí
├─ target/
│  └─ evaluacion-practica-agrotech-1.0.0.jar (y -shaded.jar)
└─ src/
   ├─ main/java/com/agrotech/
   │  ├─ App.java
   │  └─ routes/
   │     ├─ FileTransferRoute.java
   │     ├─ AgroAnalyzerRoute.java
   │     └─ RpcRoutes.java
   └─ main/resources/
      └─ application.properties
```

---

## 🗃️ Requisitos

- **Java 17** (JDK 17)
- **Maven 3.9+**
- **MySQL 8.x** (local)
- **Conector MySQL**: se incluye como dependencia `com.mysql:mysql-connector-j:8.4.0`

---

## 🛢️ Base de datos MySQL

1) Crear base y tabla:

```sql
CREATE DATABASE agrotech CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE agrotech;

CREATE TABLE lecturas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  id_sensor VARCHAR(20) NOT NULL,
  fecha DATETIME NOT NULL,
  humedad DOUBLE NULL,
  temperatura DOUBLE NULL
);
```

2) Usuario y permisos (ejemplo):
```sql
CREATE USER 'agro'@'localhost' IDENTIFIED BY 'agro123';
GRANT ALL PRIVILEGES ON agrotech.* TO 'agro'@'localhost';
FLUSH PRIVILEGES;
```

> **Nota auth MySQL**: si usas `caching_sha2_password` y ves `Public Key Retrieval is not allowed`, puedes:
> - Agregar `allowPublicKeyRetrieval=true&useSSL=false` al JDBC URL, o  
> - Cambiar el plugin del usuario:  
>   `ALTER USER 'agro'@'localhost' IDENTIFIED WITH mysql_native_password BY 'agro123';`

---

## ⚙️ Configuración (`src/main/resources/application.properties`)

```properties
# Ruta de archivos
agrotech.inbox=./inbox
agrotech.outbox=./outbox

# Datasource MySQL (Shared Database)
camel.datasource.url=jdbc:mysql://localhost:3306/agrotech_db?useSSL=false&serverTimezone=UTC
camel.datasource.username=agrotech
camel.datasource.password=agrotech123
camel.datasource.driverClassName=com.mysql.cj.jdbc.Driver

# Logging nivel info
logging.level.org.apache.camel=INFO

```

---

## ▶️ Build & Run

### 1) Compilar y crear JAR ejecutable
```bash
mvn -U clean package
```

Se genera un **JAR sombreado** en `target/evaluacion-practica-agrotech-1.0.0.jar`.

### 2) Ejecutar
```bash
java -jar target/evaluacion-practica-agrotech-1.0.0.jar
```

### 3) Probar
- Copia `sensores.csv` dentro de `inbox/`.
- Observa logs: lectura, transformación e inserciones en MySQL.
- El CSV procesado se mueve a `outbox/`.

**Formato esperado del CSV (coma-separado, con encabezado):**
```csv
sensor_id,fecha,humedad,temperatura
S001,2025-05-22,45,26.4
S002,2025-05-22,50,25.1
S003,2025-05-22,47,27.3
```

> La fecha puede enriquecerse a `YYYY-MM-DD 00:00:00` durante el procesamiento.

---

## 🔌 Rutas principales

### `FileTransferRoute`
- **from:** `file:{{agrotech.inbox}}?include=.*\.csv&move={{agrotech.outbox}}`
- **procesa:** transforma cada fila CSV → SQL `INSERT`
- **to:** `direct:agro.intake`

### `AgroAnalyzerRoute`
- **from:** `direct:agro.intake`
- **procesa:** valida/construye SQL y ejecuta `jdbc:dataSource`
- **to:** inserción en MySQL y logs

### `RpcRoutes`
- **from:** `direct:solicitarLectura`
- **to:** `direct:rpc.obtenerUltimo` (simula llamada síncrona)
- **respuesta:** última lectura mock o consultada (extensible)

---

## 🧯 Troubleshooting

- **`The POM for mysql:mysql-connector-j:9.0.0 is missing`**  
  Usa la dependencia LTS **8.4.0**:  
  ```xml
  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
  </dependency>
  ```

- **`NoClassDefFoundError: org/apache/camel/RoutesBuilder`**  
  Ejecuta el **JAR sombreado** (shade plugin). Ya configurado en `pom.xml`.

- **`No type converter available ... GenericFile → String/InputStream`**  
  Asegúrate de **convertir el body** a `String` o usar el `CsvDataFormat` sobre `Exchange.FILE_CONTENT`. Este proyecto ya lo hace.

- **`NumberFormatException: For input string: "null"`**  
  Validación: si un campo numérico viene vacío/`null`, se convierte a `NULL` o se ignora la fila (este proyecto maneja nulos).

- **`Public Key Retrieval is not allowed`**  
  Añade `allowPublicKeyRetrieval=true` al JDBC URL **o** cambia el plugin del usuario a `mysql_native_password` (ver sección DB arriba).

- **Encoding Windows (Cp1252) / UTF-8**  
  El `maven-compiler-plugin` fuerza `UTF-8`. Evita caracteres “rizados” en comentarios/cadenas.

---

## 📝 Reflexión (VIII) – resumen

1. **Patrones aplicados:** File Transfer, Translator, Enricher/Validator, Data Sink (JDBC), RPC síncrono (direct).
2. **Riesgos DB compartida:** acoplamiento, contención, bloqueos, integridad, escalabilidad y seguridad.
3. **RPC simulado:** `direct:` fuerza espera de respuesta, modela acoplamiento temporal y control de errores síncronos.
4. **Limitaciones patrones clásicos:** batch/polling y acoplamientos vs. **event-driven/streaming** y **microservicios** modernos.

> La versión completa editable está en `Reflexion_Agrotech_Progreso1.docx`.

---

## 🧾 Licencia

Proyecto académico – uso educativo.
