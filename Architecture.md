# Sistema de Gestión de Baños Químicos para Eventos Masivos

## 1. Contexto

Con motivo de la llegada del Papa a la ciudad se instalarán baños químicos en distintos puntos cercanos al recorrido del Papamóvil y en zonas de alta concentración de personas.

El objetivo del sistema es gestionar y organizar el acceso a estos baños mediante tecnologías de información, evitando que la concentración de personas genere colapsos innecesarios y distribuyendo la demanda entre los distintos puntos disponibles.

El sistema debe estar enfocado principalmente en la **capa de negocio**, ya que corresponde a un trabajo práctico de Programación Orientada a Objetos.

La interfaz gráfica/web y la integración con mapas son funcionalidades complementarias. El sistema debe ser completamente funcional desde el backend/capa de negocio aun sin frontend.

---

# 2. Objetivo principal

Permitir que los asistentes a un evento masivo:

- Consulten baños disponibles cercanos.
    
- Conozcan el estado de cada baño.
    
- Consulten la cantidad de personas esperando.
    
- Consulten el tiempo estimado de espera.
    
- Ordenen/filtren los baños según diferentes criterios.
    
- Se incorporen a la cola virtual de un baño.
    
- Realicen seguimiento de su posición en la cola.
    
- Reciban actualizaciones cuando la cola cambie.
    

Al mismo tiempo, permitir que la administración del evento:

- Gestione los baños instalados.
    
- Gestione su ubicación.
    
- Gestione su estado operativo.
    
- Gestione las asignaciones de baños al evento.
    
- Supervise las colas.
    
- Mantenga la información necesaria para calcular disponibilidad y tiempos de espera.
    

---

# 3. Alcance

## Incluido

### Gestión de baños

Cada baño debe tener información como:

- Identificador.
    
- Ubicación.
    
- Capacidad.
    
- Estado operativo.
    
- Información de la cola.
    
- Datos necesarios para estimar el tiempo de espera.
    

### Gestión de personas

El sistema debe identificar a las personas que utilizan el sistema.

Una persona puede consultar baños y, como máximo, pertenecer simultáneamente a **una única cola**.

### Colas virtuales

Cada baño puede tener una cola virtual.

Una persona puede:

- Ingresar a una cola.
    
- Consultar su posición.
    
- Consultar el tiempo estimado.
    
- Salir de la cola.
    
- Ser desplazada en posición cuando otras personas abandonen o sean atendidas.
    

### Consulta de baños

El usuario puede consultar los baños disponibles y ordenarlos mediante distintos criterios:

- Cercanía.
    
- Cantidad de personas esperando.
    
- Tiempo estimado de espera.
    

También puede existir un criterio de "mejor opción" que combine más de una variable.


### Flujos de estados

Esta sección define los estados posibles de los baños, sus diagramas de flujo y las acciones permitidas en cada uno.

Los 5 estados contemplados son:
- LIBRE
- OCUPADO
- \*SATURADO
- MANTENIMIENTO
- FUERA DE SERVICIO

\**SATURADO no es un estado propiamente dicho, sino que es un meta-estado y su unico objetivo es limitar las acciones de los estados LIBRE y OCUPADO.* 
#### Acciones por estado

LIBRE
- permitir ingreso
- pasar a OCUPADO
- llamar a MANTENIMIENTO
- mostrar
- contabilizar en funcionalidades

OCUPADO
- permitir salida
- pasar a LIBRE
- llamar a MANTENIMIENTO
- pasar automaticamente a MANTENIMIENTO
- mostrar
- contabilizar en funcionalidades

SATURADO
- permitir salida
- pasar a LIBRE
- llamar a MANTENIMIENTO
- pasar automaticamente a MANTENIMIENTO
- no permitir ingreso
- mostrar
- contabilizar en funcionalidades

MANTENIMIENTO
- No permitir ingreso
- Pasar a ESTADO CORRRESPONDIENTE
	- Mantenimiento de rutina: llama a siguiente en la cola
	- Solicitado por un usuario previo a su uso: llama al usuario si sigue en la cola
	- Solicitado por un usuario posterior a su uso: llama al siguiente en la cola
	- Mantenimiento post reparacion: pasa a LIBRE
- Mostrar cooldown (solo a usuarios en cola)
	- Permite que el usuario siga esperando
	- No visible para los usuarios que estan en una busqueda inicial
- No contabilizar en funcionalidades

FUERA_SERVICIO
- no permitir ingreso
- pasar automaticamente a MANTENIMIENTO
- no mostrar
- no contabilizar en funcionalidades


#### Cambios de estados permitidos

LIBRE
   │
   │ usuario entra
   ▼
OCUPADO
   │
   │ usuario sale
   ▼
LIBRE

LIBRE/OCUPADO
   │
   │ alcanza límite
   ▼
SATURADO
   │
   │ se desagota la espera
   ▼
LIBRE/OCUPADO


LIBRE
   │
   │ usuario n-esimo entra
   ▼
OCUPADO
   │
   │ usuario n-esimo sale
   ▼
MANTENIMIENTO


LIBRE
   │
   │ usuario entra
   ▼
OCUPADO (El usuario deja el banio con mala higiene)
   │
   │ usuario presiona llamar a mantenimiento
   ▼
MANTENIMIENTO

LIBRE
   │
   │ usuario entra
   ▼
 (El banio se encuentra con falta de higiene)
   │
   │ usuario presiona llamar a mantenimiento
   ▼
MANTENIMIENTO


MANTENIMIENTO
   │
   │ personal realiza limpieza
   ▼
LIBRE/OCUPADO/SATURADO


FUERA DE SERVICIO
   │
   │ finaliza reparación
   ▼
MANTENIMIENTO
   │
   │ personal realiza limpieza
   ▼
LIBRE

CUALQUIER ESTADO
   │
   │ se descompone
   ▼
FUERA DE SERVICIO



---

# 4. Regla fundamental de la cola

Una persona **no puede estar simultáneamente en más de una cola**.

Esta regla existe para evitar que una persona se registre en todos los baños cercanos únicamente para observar cuál avanza primero.

El comportamiento esperado es:

```text
Persona
   |
   | intenta ingresar a una cola
   v
¿Ya pertenece a otra cola?
   |
   +-- Sí --> rechazar operación
   |
   +-- No --> verificar estado del baño
                    |
                    +-- no disponible --> rechazar
                    |
                    +-- disponible --> ingresar
```

La responsabilidad de esta regla corresponde a la lógica de negocio/servicio de colas y no debe depender exclusivamente de la interfaz.

---

# 5. Asignación de baños al evento

Los baños deben poder ser asignados al evento.

Una asignación debe contemplar una **fecha**, ya que la instalación/disponibilidad de un baño está vinculada temporalmente al evento.

No debe asumirse que un baño pertenece permanentemente al evento.

Conceptualmente:

```text
Evento
  |
  └── Asignación de baños
          |
          ├── Baño
          └── Fecha / información temporal
```

La asignación permite determinar qué baños forman parte de la infraestructura disponible para un evento determinado.

La fecha debe pertenecer a la relación/asignación cuando corresponda, en lugar de asumir que es una propiedad permanente del baño.

---

# 6. Patrón State

Se utilizará el patrón **State** para representar el comportamiento de un baño según su estado operativo.

La intención es evitar grandes bloques de:

```java
if (estado == ...)
```

repartidos por todo el sistema.

Conceptualmente:

```text
Banio
  |
  └── EstadoBanio
        |
        ├── EstadoLibre
        ├── EstadoSaturado
        ├── EstadoMantenimiento
        └── EstadoFueraDeServicio
```

El baño delega determinadas operaciones en su estado actual.

Ejemplo conceptual:

```text
banio.ingresarACola(persona)
        |
        v
estadoActual.ingresarACola(...)
```

Un estado puede permitir o rechazar una operación.

### Importante

State se utiliza para comportamiento dependiente del estado.

No debe utilizarse para reglas generales del sistema.

Por ejemplo:

> "Una persona no puede estar en dos colas"

es una regla de negocio general y corresponde a la lógica de gestión de colas.

En cambio:

> "Un baño fuera de servicio no permite ingresar a su cola"

es comportamiento dependiente del estado y corresponde naturalmente a State.

---

# 7. Patrón Strategy

Se utilizará **Strategy** para encapsular diferentes criterios de selección/ordenamiento de baños.

La aplicación debe poder cambiar el criterio sin modificar la lógica principal de consulta.

Criterios previstos:

```text
CriterioBanio
    |
    ├── PorCercania
    ├── PorCantidadEspera
    ├── PorTiempoEspera
    └── MejorOpcion
```

Ejemplo conceptual:

```java
interface CriterioBanio {
    List<Banio> ordenar(List<Banio> banios, Ubicacion origen);
}
```

El servicio puede recibir una estrategia y utilizarla para ordenar los resultados.

---

# 8. Criterio de cercanía

La distancia entre el usuario y un baño debe poder calcularse a partir de sus ubicaciones.

Se recomienda mantener el cálculo de distancia desacoplado de la entidad `Banio`.

Conceptualmente:

```text
CalculadorDistancia
       |
       └── CalculadorDistanciaHaversine
```

Esto permite calcular distancia geográfica entre dos coordenadas sin depender necesariamente de un servicio externo de mapas.

La distancia puede utilizarse por Strategy para ordenar los baños desde el más cercano al más lejano.

---

# 9. Criterio de cantidad de personas

Un baño puede ser ordenado según la cantidad de personas actualmente esperando.

Ejemplo:

```text
Baño A → 2 personas
Baño B → 8 personas
Baño C → 4 personas
```

Orden ascendente:

```text
A → C → B
```

La información debe obtenerse de la cola actual del baño.

---

# 10. Criterio de tiempo de espera

El sistema debe poder estimar el tiempo de espera de un usuario.

El tiempo puede depender de:

- Cantidad de personas delante del usuario.
    
- Tiempo promedio de atención.
    
- Información de la cola.
    

Ejemplo conceptual:

```text
Personas delante: 4
Tiempo promedio: 3 minutos

Tiempo estimado ≈ 12 minutos
```

La fórmula concreta debe estar encapsulada en la lógica de negocio correspondiente y no dispersarse por la interfaz.

---

# 11. Criterio "mejor opción"

Puede existir una Strategy que combine distintos factores para determinar una opción conveniente.

Por ejemplo:

- distancia;
    
- cantidad de personas;
    
- tiempo estimado.
    

La implementación exacta debe mantenerse simple y justificable.

No se debe convertir el proyecto en un sistema de inteligencia artificial ni en un sistema de rutas.

---

# 12. Patrón Observer

Se utilizará **Observer** para notificar cambios en las colas.

La `ColaVirtual` funciona conceptualmente como el sujeto observable.

Los objetos interesados pueden suscribirse a cambios.

```text
                 ColaVirtual
                     |
                  notifica
                     |
          ┌──────────┼──────────┐
          ↓          ↓          ↓
       Observer   Observer   Observer
```

Cambios que pueden generar notificaciones:

- Ingreso de una persona.
    
- Salida de una persona.
    
- Atención de la siguiente persona.
    
- Cambio de posición.
    
- Cambio del tiempo estimado.
    

El Observer debe utilizarse primero dentro de la lógica de negocio.

No se debe hacer depender la implementación del patrón de una interfaz gráfica.

---

# 13. Tiempo real

La actualización en tiempo real es una funcionalidad complementaria.

El patrón Observer **no implica necesariamente tener un servidor escuchando permanentemente**.

Dentro del backend:

```text
Cambio en ColaVirtual
        |
        v
Observer
        |
        v
Objetos interesados
```

Si posteriormente se incorpora una interfaz web, las notificaciones pueden exponerse mediante WebSocket.

Arquitectura potencial:

```text
ColaVirtual
    |
    v
Observer
    |
    v
Servicio de notificaciones
    |
    v
WebSocket
    |
    v
Frontend
```

WebSocket no debe ser una dependencia necesaria para que la lógica de negocio funcione.

Si el tiempo del TP no permite implementar WebSocket, Observer debe seguir estando correctamente implementado en el backend.

---

# 14. Arquitectura general

La aplicación debe mantener separación de responsabilidades.

Arquitectura conceptual:

```text
                  FRONTEND
               HTML / CSS / JS
                      |
               HTTP / WebSocket
                      |
                      v
               CONTROLLERS/API
                      |
                      v
                  SERVICES
                      |
          ┌───────────┼───────────┐
          |           |           |
       Strategy      State     Observer
          |           |           |
          └───────────┼───────────┘
                      |
                    DOMAIN
                      |
                      v
                 REPOSITORIES
                      |
                      v
                   DATABASE
```

El frontend no debe contener reglas de negocio.

Los controllers tampoco deben implementar reglas complejas.

La lógica principal debe permanecer en los servicios y objetos del dominio.

---

# 15. Dominio

El modelo debe mantenerse centrado en el problema.

Entidades/conceptos principales previstos:

```text
Evento
Baño
Persona
ColaVirtual
Ubicacion
AsignacionBanio
```

Además de los objetos necesarios para implementar:

```text
EstadoBanio
Strategy de selección/ordenamiento
Observer de cola
```

No agregar entidades solamente para "tener más clases".

Cada clase debe tener una responsabilidad clara dentro del dominio.

---

# 16. Evento

El evento representa la ocasión masiva para la cual se instala y administra la infraestructura.

Puede contener información como:

- Identificador.
    
- Nombre.
    
- Fecha.
    
- Información relevante del evento.
    
- Baños asignados.
    

La relación entre evento y baño debe contemplar la asignación temporal.

---

# 17. Baño

El baño representa una unidad física de infraestructura.

Debe conocer o permitir consultar:

- Identificador.
    
- Ubicación.
    
- Capacidad.
    
- Estado actual.
    
- Cola virtual.
    
- Información necesaria para estimar espera.
    

El baño debe delegar el comportamiento dependiente del estado en `EstadoBanio`.

No debe contener la lógica de presentación del mapa.

---

# 18. Ubicación

Representa la ubicación geográfica de una persona o baño.

Debe permitir trabajar con coordenadas geográficas.

El dominio no debe depender directamente de Leaflet, OpenStreetMap ni de componentes visuales.

La representación en mapa pertenece a la capa de presentación/integración.

---

# 19. ColaVirtual

Representa la cola de personas asociada a un baño.

Responsabilidades principales:

- Agregar personas.
    
- Retirar personas.
    
- Consultar cantidad de personas.
    
- Consultar posición.
    
- Obtener siguiente persona.
    
- Calcular/obtener información necesaria para tiempo estimado.
    
- Notificar cambios a sus observers.
    

La cola no debe decidir reglas que pertenecen al servicio general del sistema, como verificar si una persona ya está en otra cola.

---

# 20. Persona

Representa al usuario del sistema.

Debe poder:

- Consultar baños.
    
- Seleccionar una cola.
    
- Ingresar a una cola.
    
- Consultar su posición.
    
- Salir de la cola.
    

El sistema debe garantizar que una persona no pertenezca a más de una cola simultáneamente.

---

# 21. Administración

Debe existir lógica para administrar la infraestructura.

Operaciones previstas:

- Alta de baño.
    
- Modificación de baño.
    
- Baja de baño.
    
- Cambio de estado.
    
- Asignación de baño a evento.
    
- Consulta de baños asignados.
    
- Gestión/supervisión de colas.
    

El detalle de roles/autenticación no forma parte del alcance principal salvo que sea necesario.

---

# 22. Repositorios

El acceso a persistencia debe estar desacoplado del dominio.

Conceptualmente:

```text
Repository
    |
    ├── BanioRepository
    ├── PersonaRepository
    ├── EventoRepository
    └── ...
```

Los servicios utilizan repositorios y no acceden directamente a la base de datos.

Si se mantiene la arquitectura utilizada en el resto del TP, se conservará la separación:

```text
Controller
   ↓
Service
   ↓
Repository
   ↓
Database
```

---

# 23. Persistencia

La persistencia debe permitir almacenar al menos la información necesaria para:

- Eventos.
    
- Baños.
    
- Personas.
    
- Ubicaciones.
    
- Estados.
    
- Asignaciones.
    
- Información de las colas cuando corresponda.
    

La implementación concreta de la base de datos no debe contaminar las entidades del dominio.

---

# 24. Frontend / interfaz gráfica

El frontend es secundario respecto de la lógica de negocio.

Si el tiempo lo permite, se implementará una interfaz web utilizando:

- HTML.
    
- CSS.
    
- JavaScript.
    
- Leaflet para mapas.
    
- OpenStreetMap como fuente de mapas.
    

La interfaz podría mostrar:

### Lista de baños

```text
--------------------------------------------------
Baño 12
Distancia: 120 m
Personas esperando: 4
Tiempo estimado: 8 min
Estado: LIBRE

[Ver en mapa] [Ingresar a cola]
--------------------------------------------------
```

### Mapa

El mapa puede mostrar:

- Ubicación del usuario.
    
- Ubicación de los baños.
    
- Estado de cada baño mediante indicadores visuales.
    
- Información al seleccionar un baño.
    

La interfaz no debe contener la lógica de negocio.

---

# 25. Visualización de estados

Si se incorpora mapa/interfaz, los estados pueden representarse visualmente:

```text
LIBRE              → verde
SATURADO           → rojo
EN_MANTENIMIENTO   → azul
FUERA_DE_SERVICIO  → negro/gris
```

Estos colores pertenecen a la presentación y no deben estar codificados dentro de las entidades del dominio.

---

# 26. Restricciones de diseño

## No hacer

No implementar reglas de negocio dentro del frontend.

No hacer que el dominio dependa de:

- Swing.
    
- JavaFX.
    
- HTML.
    
- JavaScript.
    
- Leaflet.
    
- OpenStreetMap.
    

No introducir patrones únicamente para aumentar el número de patrones utilizados.

No utilizar Builder, Decorator u otros patrones si no existe una necesidad real del dominio.

No implementar geocercas alrededor de la ruta del Papamóvil en la versión actual.

No implementar cálculo de rutas peatonales reales.

No depender de APIs externas para que la lógica principal funcione.

---

# 27. Patrones confirmados

Los patrones principales del proyecto son:

### State

Para el comportamiento del baño según su estado operativo.

### Strategy

Para los diferentes criterios de selección/ordenamiento de baños.

### Observer

Para notificar cambios en las colas y permitir posteriormente conectar esas notificaciones con una interfaz en tiempo real.

Estos tres patrones son parte del diseño principal.

Otros patrones solamente deben agregarse si aparecen necesidades concretas durante la implementación.

---

# 28. Flujo principal del usuario

```text
1. Usuario ingresa al sistema.
        |
2. Se obtiene su ubicación.
        |
3. Consulta baños disponibles.
        |
4. Se filtran/ordenan según Strategy.
        |
5. Usuario selecciona un baño.
        |
6. Consulta estado y cola.
        |
7. Solicita ingresar a la cola.
        |
8. Servicio verifica:
       - que no esté en otra cola;
       - que el baño permita el ingreso.
        |
9. Se agrega a ColaVirtual.
        |
10. Cola notifica el cambio mediante Observer.
        |
11. Se actualizan posiciones/tiempos.
        |
12. Usuario puede consultar/recibir actualizaciones.
        |
13. Cuando abandona o es atendido:
       - se elimina de la cola;
       - se notifica nuevamente.
```

---

# 29. Flujo administrativo

```text
Administrador
     |
     +--> Crear baño
     |
     +--> Modificar baño
     |
     +--> Cambiar estado
     |
     +--> Asignar baño a evento
     |
     +--> Consultar baños
     |
     +--> Supervisar colas
```

---

# 30. Prioridad de implementación

El desarrollo debe priorizar la lógica de negocio.

## Fase 1 — Núcleo

- Entidades.
    
- Relaciones.
    
- Repositorios.
    
- Servicios.
    
- Persistencia.
    
- Reglas básicas.
    

## Fase 2 — POO

- State.
    
- Strategy.
    
- Observer.
    
- Cálculo de distancia.
    
- Cálculo de tiempo de espera.
    
- Validaciones de negocio.
    

## Fase 3 — Interfaz

- API.
    
- Frontend.
    
- Lista de baños.
    
- Filtros/ordenamiento.
    
- Mapa.
    
- Visualización de estados.
    

## Fase 4 — Tiempo real

Si existe tiempo disponible:

- WebSocket.
    
- Actualización automática de las colas.
    
- Notificaciones al frontend.
    

La aplicación debe ser considerada completa aunque las fases 3 y 4 no lleguen a implementarse completamente.

---

# 31. Criterio de calidad

El objetivo no es construir una aplicación comercial completa.

El objetivo es demostrar correctamente:

- Modelado de dominio.
    
- Encapsulamiento.
    
- Responsabilidad de clases.
    
- Polimorfismo.
    
- Interfaces.
    
- Composición.
    
- Separación de capas.
    
- Reglas de negocio.
    
- Patrones de diseño aplicados justificadamente.
    
- Persistencia desacoplada.
    
- Capacidad de extender el sistema sin modificar excesivamente el código existente.
    

El código debe priorizar **claridad, cohesión y bajo acoplamiento** antes que cantidad de funcionalidades.

---

# 32. Regla para el agente de código

Este documento representa el diseño acordado del sistema.

Antes de implementar una funcionalidad nueva:

1. Revisar este documento.
    
2. Mantener las responsabilidades definidas.
    
3. No introducir patrones innecesarios.
    
4. No mover lógica de negocio al frontend.
    
5. No modificar el dominio para resolver problemas exclusivamente de presentación.
    
6. Si una nueva funcionalidad contradice este diseño, señalar la contradicción antes de realizar una modificación arquitectónica importante.
    
7. Mantener State, Strategy y Observer como patrones centrales del diseño.
    
8. Priorizar una implementación simple y defendible académicamente antes que una solución comercialmente compleja.
    

El sistema debe poder evolucionar hacia una aplicación web con mapa y actualizaciones en tiempo real, pero la capa de negocio debe permanecer independiente de esas tecnologías.