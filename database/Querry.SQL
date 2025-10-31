CREATE DATABASE agrotech_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'agrotech'@'localhost' IDENTIFIED BY 'agrotech123';
GRANT ALL PRIVILEGES ON agrotech_db.* TO 'agrotech'@'localhost';
FLUSH PRIVILEGES;


USE agrotech_db;

CREATE TABLE lecturas (
  id_sensor     VARCHAR(10) NOT NULL,
  fecha         DATETIME    NOT NULL,
  humedad       DOUBLE      NOT NULL,
  temperatura   DOUBLE      NOT NULL,
  PRIMARY KEY (id_sensor, fecha)
);

SELECT * FROM agrotech_db.lecturas;
