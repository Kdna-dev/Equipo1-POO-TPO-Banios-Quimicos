# Diagrama de Clases UML — Patrón Strategy (Búsqueda y Cálculo de Distancia)

Este diagrama modela el desacoplamiento de los algoritmos de ordenamiento y selección de baños respecto de los servicios y del modelo de dominio, permitiendo incorporar nuevos criterios sin modificar la lógica existente (Principio Open/Closed).

## Diagrama Mermaid

```mermaid
classDiagram
    direction TB

    %% Contexto / Consumidor
    class BanioService {
        -IBanioRepository banioRepository
        -ColaService colaService
        +consultarBanios(Ubicacion origen, CriterioBanio criterio) List~Banio~
        +consultarBanios(Ubicacion origen, CriterioBanio criterio, Integer mascaraFunc, boolean soloVisibles) List~Banio~
    }

    %% Estrategia Principal de Búsqueda / Ordenamiento
    class CriterioBanio {
        <<interface>>
        +ordenar(List~Banio~ banios, Ubicacion origen) List~Banio~
    }

    class PorCercania {
        -CalculadorDistancia calculadorDistancia
        +PorCercania()
        +PorCercania(CalculadorDistancia calculador)
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
        +MejorOpcion()
        +MejorOpcion(CalculadorDistancia calculador)
        +ordenar(List~Banio~ banios, Ubicacion origen) List~Banio~
        -calcularCostoTotal(Banio banio, Ubicacion origen) double
    }

    %% Estrategia Secundaria de Cálculo de Distancia
    class CalculadorDistancia {
        <<interface>>
        +calcularDistancia(Ubicacion u1, Ubicacion u2) double
    }

    class CalculadorDistanciaHaversine {
        -double RADIO_TIERRA_METROS
        +calcularDistancia(Ubicacion u1, Ubicacion u2) double
    }

    %% Elementos de Dominio Colaboradores
    class Banio {
        -int id
        -String nombre
        -Ubicacion ubicacion
        -int capacidad
        +getCantidadEnEspera() int
        +getTiempoEstimadoEspera() double
        +calcularDistanciaA(Ubicacion origen, CalculadorDistancia calc) double
    }

    class Ubicacion {
        -double latitud
        -double longitud
        -String direccion
        +getLatitud() double
        +getLongitud() double
    }

    %% Relaciones
    BanioService ..> CriterioBanio : inyecta por parámetro
    CriterioBanio <|.. PorCercania
    CriterioBanio <|.. PorCantidadEspera
    CriterioBanio <|.. PorTiempoEspera
    CriterioBanio <|.. MejorOpcion

    PorCercania o--> CalculadorDistancia : compone
    MejorOpcion o--> CalculadorDistancia : compone
    CalculadorDistancia <|.. CalculadorDistanciaHaversine

    CriterioBanio ..> Banio : ordena
    CriterioBanio ..> Ubicacion : calcula respecto a origen
    Banio --> Ubicacion : tiene
```

## Puntos Clave

1. **Inyección de Dependencia**: `BanioService` no instancia estrategias; recibe la abstracción `CriterioBanio` dinámicamente en tiempo de ejecución.
2. **Estrategias Anidadas**: Las estrategias que calculan proximidad (`PorCercania` y `MejorOpcion`) no se acoplan a la fórmula trigonométrica de la Tierra; delegan en `CalculadorDistancia`, permitiendo sustituir `CalculadorDistanciaHaversine` por cualquier otro algoritmo de cálculo métrico sin impacto.
3. **Cohesión**: `MejorOpcion` pondera la combinación de tiempo de espera en cola y tiempo de caminata a velocidad estándar sin contaminar a la entidad `Banio`.
