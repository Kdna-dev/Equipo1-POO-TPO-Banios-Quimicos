# Esquema de Base de Datos - Supabase (PostgreSQL)

Este archivo contiene el script DDL para inicializar las tablas necesarias para el Sistema de Gestión de Baños Químicos para Eventos Masivos.

```sql
-- 1. Tabla Persona
CREATE TABLE IF NOT EXISTS persona (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    edad INT NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL
);

-- 2. Tabla Banio
CREATE TABLE IF NOT EXISTS banio (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    latitud DOUBLE PRECISION,
    longitud DOUBLE PRECISION,
    nombre_ubicacion VARCHAR(100),
    estado VARCHAR(30) NOT NULL DEFAULT 'LIBRE',
    capacidad INT NOT NULL DEFAULT 10,
    funcionalidades INT NOT NULL DEFAULT 0,
    tiempo_promedio DOUBLE PRECISION DEFAULT 3.0
);

-- 3. Tabla Evento
CREATE TABLE IF NOT EXISTS evento (
    id SERIAL PRIMARY KEY,
    fecha DATE NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    inicio TIMESTAMP,
    fin TIMESTAMP
);

-- 4. Tabla Asignacion de Banio a Evento (relación temporal)
CREATE TABLE IF NOT EXISTS asignacion_banio (
    id SERIAL PRIMARY KEY,
    banio_id INT NOT NULL REFERENCES banio(id) ON DELETE CASCADE,
    evento_id INT NOT NULL REFERENCES evento(id) ON DELETE CASCADE,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_desasignacion TIMESTAMP,
    latitud DOUBLE PRECISION,
    longitud DOUBLE PRECISION,
    nombre_ubicacion VARCHAR(100)
);

-- Índices recomendados para optimización de consultas
CREATE INDEX IF NOT EXISTS idx_banio_funcionalidades ON banio (funcionalidades);
CREATE INDEX IF NOT EXISTS idx_banio_estado ON banio (estado);
CREATE INDEX IF NOT EXISTS idx_asignacion_evento ON asignacion_banio (evento_id);
CREATE INDEX IF NOT EXISTS idx_asignacion_banio ON asignacion_banio (banio_id);
```

### Datos de Ejemplo (Opcional para pruebas)

```sql
-- Personas de prueba
INSERT INTO persona (nombre, apellido, edad, email) VALUES
('Juan', 'Perez', 28, 'juan.perez@example.com'),
('Maria', 'Gomez', 34, 'maria.gomez@example.com'),
('Carlos', 'Lopez', 45, 'carlos.lopez@example.com');

-- Baños de prueba
-- Nota sobre funcionalidades (bitmask):
-- 1: Movilidad Reducida, 2: Lavamanos, 4: Dispenser Alcohol, 8: Cambiador Bebes, 16: Espejo
-- Ejemplo: 3 = 1 + 2 (Movilidad reducida + Lavamanos)
-- Ejemplo: 7 = 1 + 2 + 4 (Movilidad reducida + Lavamanos + Alcohol)
INSERT INTO banio (nombre, latitud, longitud, nombre_ubicacion, estado, capacidad, funcionalidades, tiempo_promedio) VALUES
('Baño Plaza de Mayo 1', -34.6083, -58.3712, 'Plaza de Mayo - Norte', 'LIBRE', 10, 7, 3.0),
('Baño Plaza de Mayo 2', -34.6087, -58.3715, 'Plaza de Mayo - Sur', 'LIBRE', 10, 2, 2.5),
('Baño Obelisco 1', -34.6037, -58.3816, 'Av. 9 de Julio y Corrientes', 'LIBRE', 15, 1, 3.0),
('Baño Catedral', -34.6076, -58.3732, 'Catedral Metropolitana', 'LIBRE', 8, 31, 3.5);

-- Evento de prueba
INSERT INTO evento (fecha, nombre, descripcion, inicio, fin) VALUES
('2026-10-15', 'Visita Papal', 'Recorrido del Papamóvil y misa central', '2026-10-15 08:00:00', '2026-10-15 18:00:00');

-- Asignación de baño al evento
INSERT INTO asignacion_banio (banio_id, evento_id, fecha_asignacion, latitud, longitud, nombre_ubicacion) VALUES
(1, 1, '2026-10-15 06:00:00', -34.6083, -58.3712, 'Plaza de Mayo - Norte'),
(2, 1, '2026-10-15 06:00:00', -34.6087, -58.3715, 'Plaza de Mayo - Sur'),
(3, 1, '2026-10-15 06:00:00', -34.6037, -58.3816, 'Av. 9 de Julio y Corrientes'),
(4, 1, '2026-10-15 06:00:00', -34.6076, -58.3732, 'Catedral Metropolitana');
```
