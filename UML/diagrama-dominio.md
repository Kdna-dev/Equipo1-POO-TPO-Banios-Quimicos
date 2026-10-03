# Diagrama de Clases UML — Asignación Temporal de Infraestructura a Eventos

Este diagrama modela la relación entre los eventos masivos y la infraestructura sanitaria disponible, desacoplando los baños del ciclo de vida del evento mediante una entidad intermedia reificada (`AsignacionBanio`).

## Diagrama Mermaid

```mermaid
classDiagram
    direction TB

    %% Capa de Aplicación
    class EventoService {
        -IEventoRepository eventoRepository
        -IAsignacionBanioRepository asignacionRepository
        -IBanioRepository banioRepository
        +crearEvento(Evento evento) Evento
        +asignarBanio(int eventoId, int banioId, LocalDateTime fIni, LocalDateTime fFin, Ubicacion ub) AsignacionBanio
        +desasignarBanio(int asignacionId) void
        +obtenerAsignaciones(int eventoId) List~AsignacionBanio~
        +obtenerBaniosDeEvento(int eventoId) List~Banio~
    }

    %% Dominio: Evento y Asignación
    class Evento {
        -int id
        -String nombre
        -LocalDate fecha
        -String descripcion
        -LocalDateTime inicio
        -LocalDateTime fin
        -List~Banio~ banios
        +addBanio(Banio banio) void
        +getBanios() List~Banio~
    }

    class AsignacionBanio {
        -int id
        -Evento evento
        -Banio banio
        -LocalDateTime fechaAsignacion
        -LocalDateTime fechaDesasignacion
        -Ubicacion ubicacion
        +getFechaAsignacion() LocalDateTime
        +getFechaDesasignacion() LocalDateTime
        +getUbicacion() Ubicacion
        +getBanio() Banio
        +getEvento() Evento
    }

    class Banio {
        -int id
        -String nombre
        -Ubicacion ubicacion
        -int capacidad
        -int funcionalidades
        +getUbicacion() Ubicacion
        +setUbicacion(Ubicacion ubicacion) void
    }

    class Ubicacion {
        -double latitud
        -double longitud
        -String direccion
        +getLatitud() double
        +getLongitud() double
    }

    %% Relaciones
    EventoService ..> Evento : administra
    EventoService ..> AsignacionBanio : crea y consulta
    EventoService ..> Banio : asigna

    Evento "1" --> "0..*" AsignacionBanio : posee asignaciones
    AsignacionBanio "0..*" --> "1" Banio : referencia asignado
    AsignacionBanio "1" --> "1" Ubicacion : ubicación física en evento
    Banio "1" --> "1" Ubicacion : ubicación base
```

## Puntos Clave

1. **Desacoplamiento Temporal**: Un baño químico es un activo físico reusable. No pertenece permanentemente a un evento ni desaparece cuando el evento finaliza.
2. **Entidad Intermedia Reificada (`AsignacionBanio`)**: Resuelve la relación muchos a muchos con atributos propios de la asociación: `fechaAsignacion`, `fechaDesasignacion` y `ubicacion` específica en el predio del evento.
3. **Servicio Orquestador (`EventoService`)**: Encapsula las transacciones de asignación y desasignación interactuando con los repositorios correspondientes.
