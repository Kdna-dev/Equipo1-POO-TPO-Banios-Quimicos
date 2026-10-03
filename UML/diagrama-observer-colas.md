# Diagrama de Clases UML — Patrón Observer y Colas Virtuales

Este diagrama modela la gestión reactiva de las colas virtuales asociadas a cada baño mediante el patrón `Observer`, junto con la regla de negocio que prohíbe que una persona pertenezca a más de una cola simultáneamente.

## Diagrama Mermaid

```mermaid
classDiagram
    direction TB

    %% Capa de Aplicación
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

    %% Sujeto y Elementos de Cola
    class Banio {
        -int id
        -String nombre
        -int capacidad
        -ColaVirtual colaVirtual
        -double tiempoPromedioAtencionMinutos
        -boolean tieneOcupante
        +ingresarACola(Persona persona) void
        +retirarDeCola(Persona persona) boolean
        +atenderSiguiente() Persona
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

    %% Patrón Observer
    class ObserverCola {
        <<interface>>
        +alIngresarPersona(ColaVirtual cola, Persona persona) void
        +alSalirPersona(ColaVirtual cola, Persona persona) void
        +alAtenderSiguiente(ColaVirtual cola, Persona persona) void
        +alCambiarPosicionOTiempo(ColaVirtual cola) void
    }

    class NotificadorConsolaObserver {
        +alIngresarPersona(ColaVirtual cola, Persona persona) void
        +alSalirPersona(ColaVirtual cola, Persona persona) void
        +alAtenderSiguiente(ColaVirtual cola, Persona persona) void
        +alCambiarPosicionOTiempo(ColaVirtual cola) void
    }

    class NotificadorWebSocketObserver {
        +alIngresarPersona(ColaVirtual cola, Persona persona) void
        +alSalirPersona(ColaVirtual cola, Persona persona) void
        +alAtenderSiguiente(ColaVirtual cola, Persona persona) void
        +alCambiarPosicionOTiempo(ColaVirtual cola) void
    }

    class Persona {
        -int id
        -String nombre
        -String apellido
        -int edad
        -String email
        +getId() int
        +getNombre() String
    }

    %% Relaciones
    BanioService --> ColaService : orquesta regla
    ColaService ..> Banio : valida ingreso
    ColaService ..> Persona : controla regla 1 cola

    Banio "1" *--> "1" ColaVirtual : compone
    ColaVirtual "1" o--> "0..*" ObserverCola : notifica a
    ObserverCola <|.. NotificadorConsolaObserver
    ObserverCola <|.. NotificadorWebSocketObserver
    ColaVirtual "1" o--> "0..*" Persona : contiene en espera
```

## Puntos Clave

1. **Sujeto Observable Puro (`ColaVirtual`)**: Notifica cambios de estado (altas, bajas, desplazamientos de posición) a la interfaz `ObserverCola` sin conocer detalles de presentación.
2. **Extensibilidad de Notificaciones**: Permite desacoplar salidas a consola (`NotificadorConsolaObserver`) de notificaciones reactivas para WebSocket o Server-Sent Events (`NotificadorWebSocketObserver`).
3. **Control de Regla de Negocio (`ColaService`)**: La restricción "una persona, como máximo una cola" no es responsabilidad de la cola individual sino del orquestador de colas en la capa de aplicación.
