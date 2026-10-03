# Diagrama de Clases UML — Patrón State (Estados y Ciclo Operativo del Baño)

Este diagrama modela el comportamiento del baño en función de su estado operativo, reemplazando condicionales múltiples (`if/switch`) por polimorfismo puro según la sección correspondiente de `Architecture.md`.

## Diagrama Mermaid

```mermaid
classDiagram
    direction TB

    %% Contexto
    class Banio {
        -int id
        -String nombre
        -int capacidad
        -EstadoBanio estado
        -boolean tieneOcupante
        +setEstado(EstadoBanio nuevoEstado) void
        +getEstado() EstadoBanio
        +getTipoEstado() TipoEstadoBanio
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

    %% Abstracción del Estado
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

    %% Estados Concretos
    class EstadoLibre {
        +getTipo() TipoEstadoBanio
        +permiteIngreso() boolean
        +esVisible() boolean
        +contabilizaFuncionalidades() boolean
        +entrar(Banio banio) void
        +saturar(Banio banio) void
    }

    class EstadoOcupado {
        +getTipo() TipoEstadoBanio
        +permiteIngreso() boolean
        +esVisible() boolean
        +contabilizaFuncionalidades() boolean
        +salir(Banio banio) void
        +saturar(Banio banio) void
    }

    class EstadoSaturado {
        +getTipo() TipoEstadoBanio
        +permiteIngreso() boolean
        +esVisible() boolean
        +contabilizaFuncionalidades() boolean
        +desaturar(Banio banio) void
    }

    class EstadoMantenimiento {
        +getTipo() TipoEstadoBanio
        +permiteIngreso() boolean
        +esVisible() boolean
        +contabilizaFuncionalidades() boolean
        +finalizarMantenimiento(Banio banio) void
    }

    class EstadoFueraDeServicio {
        +getTipo() TipoEstadoBanio
        +permiteIngreso() boolean
        +esVisible() boolean
        +contabilizaFuncionalidades() boolean
        +iniciarMantenimiento(Banio banio) void
    }

    %% Tipificación
    class TipoEstadoBanio {
        <<enumeration>>
        LIBRE
        OCUPADO
        SATURADO
        MANTENIMIENTO
        FUERA_SERVICIO
    }

    %% Capa de Aplicación Orquestadora
    class BanioService {
        +cambiarEstado(int banioId, EstadoBanio nuevo) void
        +descomponerBanio(int banioId) void
        +iniciarMantenimiento(int banioId) void
        +finalizarMantenimiento(int banioId) void
    }

    %% Relaciones
    Banio "1" o--> "1" EstadoBanio : delega operaciones
    EstadoBanio <|.. EstadoLibre
    EstadoBanio <|.. EstadoOcupado
    EstadoBanio <|.. EstadoSaturado
    EstadoBanio <|.. EstadoMantenimiento
    EstadoBanio <|.. EstadoFueraDeServicio

    EstadoBanio ..> TipoEstadoBanio : getTipo()
    BanioService ..> Banio : administra estado
```

## Puntos Clave

1. **Delegación Estricta**: La clase `Banio` no decide si se puede ingresar, entrar, salir o limpiar; delega directamente en `estado.entrar(this)`, `estado.salir(this)`, etc.
2. **Meta-Estado `EstadoSaturado`**: De acuerdo con `Architecture.md`, no es un estado físico cerrado sino un meta-estado de capacidad que bloquea nuevos ingresos a la cola virtual (`permiteIngreso() == false`), pero permite que el usuario adentro salga (`desaturar`) y que el personal ejecute mantenimiento.
3. **Mantenimiento y Contingencia**: La transición a `EstadoFueraDeServicio` puede darse desde cualquier estado ante avería (`descomponer()`). La reparación posterior deriva siempre primero a `EstadoMantenimiento` (limpieza e inspección técnica) antes de volver al circuito de disponibilidad `EstadoLibre`.
