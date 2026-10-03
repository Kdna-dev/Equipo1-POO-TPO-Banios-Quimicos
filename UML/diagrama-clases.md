# Diagrama de Clases UML — Arquitectura de Dominio y Patrones

Para garantizar máxima legibilidad y evitar tipografía reducida por densidad de nodos, la arquitectura estática se encuentra modularizada en **4 diagramas temáticos de responsabilidad única**:

1. [**Patrón Strategy: Búsqueda y Distancia**](diagrama-strategy.md)
2. [**Patrón State: Estados y Ciclo Operativo del Baño**](diagrama-state.md)
3. [**Patrón Observer: Colas Virtuales y Notificaciones**](diagrama-observer-colas.md)
4. [**Dominio: Asignación Temporal de Infraestructura a Eventos**](diagrama-asignacion-evento.md)

---

## 1. Patrón Strategy: Criterios de Ordenamiento y Cálculo de Distancia

Desacopla los algoritmos de selección y ordenamiento de baños respecto del servicio y del dominio.

```mermaid
classDiagram
    direction TB

    class BanioService {
        +consultarBanios(Ubicacion origen, CriterioBanio criterio) List~Banio~
    }

    class CriterioBanio {
        <<interface>>
        +ordenar(List~Banio~ banios, Ubicacion origen) List~Banio~
    }

    class PorCercania {
        -CalculadorDistancia calculadorDistancia
        +ordenar(List~Banio~ banios, Ubicacion origen) List~Banio~
    }

    class PorCantidadEspera {
        +ordenar(List~Banio~ banios, Ubicacion origen) List~Banio~
    }

    class PorTiempoEspera {
        +ordenar(List~Banio~ banios, Ubicacion origen) List~Banio~
    }

    class MejorOpcion {
        -CalculadorDistancia calculadorDistancia
        -double METROS_POR_MINUTO_CAMINATA
        +ordenar(List~Banio~ banios, Ubicacion origen) List~Banio~
        -calcularCostoTotal(Banio banio, Ubicacion origen) double
    }

    class CalculadorDistancia {
        <<interface>>
        +calcularDistancia(Ubicacion u1, Ubicacion u2) double
    }

    class CalculadorDistanciaHaversine {
        +calcularDistancia(Ubicacion u1, Ubicacion u2) double
    }

    class Banio {
        -int id
        -Ubicacion ubicacion
        +getCantidadEnEspera() int
        +getTiempoEstimadoEspera() double
        +calcularDistanciaA(Ubicacion origen, CalculadorDistancia calc) double
    }

    class Ubicacion {
        -double latitud
        -double longitud
    }

    BanioService ..> CriterioBanio : inyecta por parámetro
    CriterioBanio <|.. PorCercania
    CriterioBanio <|.. PorCantidadEspera
    CriterioBanio <|.. PorTiempoEspera
    CriterioBanio <|.. MejorOpcion

    PorCercania o--> CalculadorDistancia : compone
    MejorOpcion o--> CalculadorDistancia : compone
    CalculadorDistancia <|.. CalculadorDistanciaHaversine

    CriterioBanio ..> Banio : ordena
    Banio --> Ubicacion : tiene
```

---

## 2. Patrón State: Ciclo Operativo y Estados del Baño

Reemplaza condicionales de estado por polimorfismo puro. Modela el meta-estado de saturación y las contingencias según la documentación técnica.

```mermaid
classDiagram
    direction TB

    class Banio {
        -int id
        -String nombre
        -int capacidad
        -EstadoBanio estado
        -boolean tieneOcupante
        +setEstado(EstadoBanio nuevoEstado) void
        +ingresarACola(Persona persona) void
        +entrar() void
        +salir() void
        +saturar() void
        +desaturar() void
        +iniciarMantenimiento() void
        +finalizarMantenimiento() void
        +descomponer() void
        +permiteIngreso() boolean
        +esVisible() boolean
        +contabilizaFuncionalidades() boolean
    }

    class EstadoBanio {
        <<interface>>
        +getTipo() TipoEstadoBanio
        +permiteIngreso() boolean
        +esVisible() boolean
        +contabilizaFuncionalidades() boolean
        +ingresarACola(Banio banio, Persona persona) void
        +entrar(Banio banio) void
        +salir(Banio banio) void
        +saturar(Banio banio) void
        +desaturar(Banio banio) void
        +iniciarMantenimiento(Banio banio) void
        +finalizarMantenimiento(Banio banio) void
        +descomponer(Banio banio) void
    }

    class EstadoLibre {
        +entrar(Banio banio) void
        +saturar(Banio banio) void
    }

    class EstadoOcupado {
        +salir(Banio banio) void
        +saturar(Banio banio) void
    }

    class EstadoSaturado {
        +desaturar(Banio banio) void
    }

    class EstadoMantenimiento {
        +finalizarMantenimiento(Banio banio) void
    }

    class EstadoFueraDeServicio {
        +iniciarMantenimiento(Banio banio) void
    }

    class TipoEstadoBanio {
        <<enumeration>>
        LIBRE
        OCUPADO
        SATURADO
        MANTENIMIENTO
        FUERA_SERVICIO
    }

    Banio "1" o--> "1" EstadoBanio : delega estado actual
    EstadoBanio <|.. EstadoLibre
    EstadoBanio <|.. EstadoOcupado
    EstadoBanio <|.. EstadoSaturado
    EstadoBanio <|.. EstadoMantenimiento
    EstadoFueraDeServicio ..|> EstadoBanio
    EstadoBanio ..> TipoEstadoBanio : clasifica
```

---

## 3. Patrón Observer: Colas Virtuales y Notificaciones

Modela la reactividad de las colas virtuales ante altas, bajas y avances, desacoplado de interfaces gráficas o frameworks.

```mermaid
classDiagram
    direction TB

    class ColaService {
        -Map~int, int~ colasActivas
        +ingresarACola(Banio banio, Persona persona) void
        +salirDeCola(Banio banio, Persona persona) boolean
        +atenderSiguiente(Banio banio) Persona
        +estaEnAlgunaCola(Persona persona) boolean
    }

    class BanioService {
        -ColaService colaService
        +ingresarACola(int banioId, Persona persona) void
        +salirDeCola(int banioId, Persona persona) boolean
        +atenderSiguiente(int banioId) Persona
        +suscribirObservador(int banioId, ObserverCola obs) void
    }

    class Banio {
        -int id
        -String nombre
        -ColaVirtual colaVirtual
        -double tiempoPromedioAtencionMinutos
        +getCantidadEnEspera() int
        +getTiempoEstimadoEspera() double
        +getColaVirtual() ColaVirtual
    }

    class ColaVirtual {
        -List~Persona~ cola
        -List~ObserverCola~ observadores
        +agregarObserver(ObserverCola observer) void
        +removerObserver(ObserverCola observer) void
        +agregarPersona(Persona persona) void
        +retirarPersona(Persona persona) boolean
        +atenderSiguiente() Persona
        +getCantidadPersonas() int
        +getPosicion(Persona persona) int
        -notificarIngreso(Persona persona) void
        -notificarSalida(Persona persona) void
        -notificarAtencion(Persona persona) void
        -notificarCambioPosicionOTiempo() void
    }

    class ObserverCola {
        <<interface>>
        +alIngresarPersona(ColaVirtual cola, Persona persona) void
        +alSalirPersona(ColaVirtual cola, Persona persona) void
        +alAtenderSiguiente(ColaVirtual cola, Persona persona) void
        +alCambiarPosicionOTiempo(ColaVirtual cola) void
    }

    class NotificadorConsolaObserver {
        +alIngresarPersona(ColaVirtual cola, Persona persona) void
    }

    class NotificadorWebSocketObserver {
        +alIngresarPersona(ColaVirtual cola, Persona persona) void
    }

    class Persona {
        -int id
        -String nombre
        -String apellido
        -int edad
        -String email
    }

    BanioService --> ColaService : orquesta regla
    ColaService ..> Banio : valida ingreso
    ColaService ..> Persona : controla regla 1 cola

    Banio "1" *--> "1" ColaVirtual : compone
    ColaVirtual "1" o--> "0..*" ObserverCola : notifica a
    ObserverCola <|.. NotificadorConsolaObserver
    ObserverCola <|.. NotificadorWebSocketObserver
    ColaVirtual "1" o--> "0..*" Persona : contiene en espera
```

---

## 4. Dominio: Asignación Temporal de Infraestructura a Eventos

Modela la entidad intermedia `AsignacionBanio` que otorga vigencia temporal y ubicación espacial a los baños dentro de un evento masivo.

```mermaid
classDiagram
    direction TB

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
    }

    class Ubicacion {
        -double latitud
        -double longitud
        -String direccion
        +getLatitud() double
        +getLongitud() double
    }

    EventoService ..> Evento : administra
    EventoService ..> AsignacionBanio : crea y consulta
    EventoService ..> Banio : asigna

    Evento "1" --> "0..*" AsignacionBanio : posee asignaciones
    AsignacionBanio "0..*" --> "1" Banio : referencia asignado
    AsignacionBanio "1" --> "1" Ubicacion : ubicación física en evento
    Banio "1" --> "1" Ubicacion : ubicación base
```
