package Modelo.Aplicacion;

import Modelo.Dominio.Banio;
import Modelo.Dominio.Persona;
import Modelo.Dominio.TipoEstadoBanio;
import Modelo.Dominio.Ubicacion;
import Modelo.EstrategiaBusqueda.EstrategiaOrdenamiento;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Atención de visitantes en los baños del evento (capa de modelo).
 *
 * Orquesta BanioService, ColaService y PersonaService para los casos de uso que necesita la app:
 * registrar visitantes, sumarse a una cola, salir de la cola o del baño, acciones de administración.
 * Además lleva el registro de "quién está usando qué baño", que el dominio no guarda por persona.
 *
 * No conoce HTTP ni JSON: eso queda en Controlador y Vista.
 */
public class AtencionService {

    private final BanioService banios;
    private final PersonaService personasSrv;
    private final ColaService colas;

    private final Notificaciones notificaciones = new Notificaciones();
    private final Map<Integer, Integer> ocupantes = new ConcurrentHashMap<>();   // personaId -> banioId
    private final Map<Integer, Persona> personas = new ConcurrentHashMap<>();    // misma instancia en cola y en repositorio
    private final Set<Integer> observados = new HashSet<>();
    private int visitantes = 0;

    public AtencionService(BanioService banios, PersonaService personasSrv) {
        this.banios = banios;
        this.personasSrv = personasSrv;
        this.colas = banios.getColaService();
        synchronized (this) { banios.obtenerTodos().forEach(this::observar); }
    }

    // ------------------------------------------------------------ baños

    /** Alta de un baño; también lo usa el seed. El nombre visible es "Baño #id". */
    public synchronized Banio altaBanio(String direccion, double lat, double lng, int capacidad, int mascara, double tiempoPromedio) {
        if (capacidad < 1) throw new IllegalArgumentException("La capacidad debe ser al menos 1");
        if (tiempoPromedio <= 0) throw new IllegalArgumentException("El tiempo promedio debe ser mayor a 0");
        Banio b = new Banio(0, "Baño", new Ubicacion(direccion, lat, lng), capacidad, mascara);
        b.setTiempoPromedioAtencionMinutos(tiempoPromedio);
        banios.crearBanio(b);
        b.setNombre("Baño #" + b.getId());
        banios.actualizarBanio(b);
        observar(b);
        return b;
    }

    /** Busca un baño y se asegura de que su cola tenga el observer de turnos. */
    public Banio buscarBanio(int id) {
        Banio b = banios.buscarPorId(id);
        if (b == null) throw new RecursoNoEncontradoException("No existe el baño " + id);
        observar(b);
        return b;
    }

    public List<Banio> todosLosBanios() {
        List<Banio> r = new ArrayList<>();
        for (Banio b : banios.obtenerTodos()) { observar(b); r.add(b); }
        return r;
    }

    /** Consulta de baños visibles ordenados por la estrategia elegida (Strategy), con filtros opcionales. */
    public List<Banio> consultar(Ubicacion origen, EstrategiaOrdenamiento estrategia, int mascara, Double distanciaMaxMetros) {
        List<Banio> r = new ArrayList<>();
        for (Banio b : banios.consultarBanios(origen, estrategia, mascara, true)) {
            observar(b);
            if (distanciaMaxMetros != null && b.calcularDistanciaA(origen, null) > distanciaMaxMetros) continue;
            r.add(b);
        }
        return r;
    }

    public ResumenEvento resumen() {
        int operativos = 0, esperando = 0;
        for (Banio b : banios.obtenerTodos()) {
            TipoEstadoBanio e = b.getTipoEstado();
            if (e == TipoEstadoBanio.LIBRE || e == TipoEstadoBanio.OCUPADO || e == TipoEstadoBanio.SATURADO) operativos++;
            esperando += b.getCantidadEnEspera();
        }
        return new ResumenEvento(operativos, esperando);
    }

    private void observar(Banio b) {
        if (observados.add(b.getId())) {
            b.getColaVirtual().agregarObserver(new ObservadorTurnos(b.getId(), b.getNombre(), notificaciones, ocupantes));
        }
    }

    // ------------------------------------------------------------ visitantes

    public Persona registrarVisitante() {
        Persona p = personasSrv.registrarPersona("Visitante", "Anónimo", 0, "anon-" + UUID.randomUUID() + "@ejemplo.local");
        personas.put(p.getId(), p);
        return p;
    }

    public Persona buscarVisitante(int id) {
        Persona p = personas.get(id);
        if (p == null) throw new RecursoNoEncontradoException("No existe la persona " + id);
        return p;
    }

    public List<Notificacion> notificacionesDesde(Persona p, long ultimoId) {
        return notificaciones.desde(p.getId(), ultimoId);
    }

    // ------------------------------------------------------------ colas y uso

    /** Suma a la persona a la cola. Si el baño está sin ocupante, el dominio la atiende enseguida. */
    public void ingresar(Banio b, Persona p) {
        if (ocupantes.containsKey(p.getId())) throw new IllegalStateException("Estás usando un baño: terminá antes de sumarte a otra cola.");
        banios.ingresarACola(b.getId(), p);
        TipoEstadoBanio t = b.getTipoEstado();
        if (!b.tieneOcupante() && (t == TipoEstadoBanio.LIBRE || t == TipoEstadoBanio.SATURADO)) banios.atenderSiguiente(b.getId());
    }

    public void salirDeCola(Persona p) {
        Integer id = colas.obtenerBanioIdDePersona(p);
        if (id == null || !banios.salirDeCola(id, p)) throw new IllegalStateException("No estás en ninguna cola.");
    }

    /** La persona deja el baño. Si avisa mala higiene, el baño sale de circulación para limpieza. */
    public void salirDelBanio(Persona p, boolean sucio) {
        Integer id = ocupantes.get(p.getId());
        if (id == null) throw new IllegalStateException("No estás usando ningún baño.");
        Banio b = buscarBanio(id);
        ocupantes.remove(p.getId());
        if (sucio && b.getTipoEstado() == TipoEstadoBanio.OCUPADO) {
            // limpieza solicitada: el baño sale de circulación (State: ocupado -> libre -> fuera de servicio -> mantenimiento)
            b.salir();
            banios.actualizarBanio(b);
            banios.descomponerBanio(id);
            banios.iniciarMantenimiento(id);
        } else {
            banios.atenderSiguiente(id);   // el próximo de la cola entra (el dominio lo notifica) o el baño queda libre
        }
    }

    /** Estado de la persona respecto de colas y baños, o null si no está en ninguno. */
    public Seguimiento seguimiento(Persona p) {
        Integer enUso = ocupantes.get(p.getId());
        Integer enCola = colas.obtenerBanioIdDePersona(p);
        Integer id = enUso != null ? enUso : enCola;
        if (id == null) return null;
        Banio b = banios.buscarPorId(id);
        if (b == null) return null;
        boolean usando = enUso != null;
        int pos = usando ? 0 : b.getColaVirtual().getPosicion(p);
        int delante = usando ? 0 : pos - 1;
        long minutos = usando ? 0L : (long) Math.ceil(((pos - 1) + (b.tieneOcupante() ? 1 : 0)) * b.getTiempoPromedioAtencionMinutos());
        return new Seguimiento(b, usando, pos, delante, minutos);
    }

    // ------------------------------------------------------------ administración

    public void darDeBaja(Banio b) {
        for (Persona p : new ArrayList<>(b.getColaVirtual().getPersonas())) banios.salirDeCola(b.getId(), p);
        liberarOcupante(b);
        banios.eliminarBanio(b.getId());
        observados.remove(b.getId());
    }

    /** Suma visitantes de prueba a la cola hasta llegar a la cantidad pedida o hasta que el baño no acepte más. */
    public int simularVisitantes(Banio b, int pedidas) {
        int agregadas = 0;
        for (int i = 0; i < pedidas; i++) {
            Persona p = personasSrv.registrarPersona("Visitante", "N°" + (++visitantes), 18, "visitante" + visitantes + "@ejemplo.local");
            personas.put(p.getId(), p);
            try { ingresar(b, p); agregadas++; } catch (IllegalStateException e) { break; }   // cola llena o baño sin ingreso
        }
        return agregadas;
    }

    public void ejecutarAccion(Banio b, AccionBanio accion) {
        int id = b.getId();
        TipoEstadoBanio e = b.getTipoEstado();
        switch (accion) {
            case DESCOMPONER:
                banios.descomponerBanio(id);
                break;
            case INICIAR_MANTENIMIENTO:      // el State solo permite mantenimiento desde "fuera de servicio"
                if (e != TipoEstadoBanio.FUERA_SERVICIO && e != TipoEstadoBanio.MANTENIMIENTO) banios.descomponerBanio(id);
                if (b.getTipoEstado() == TipoEstadoBanio.FUERA_SERVICIO) banios.iniciarMantenimiento(id);
                break;
            case FINALIZAR_MANTENIMIENTO:
                banios.finalizarMantenimiento(id);
                liberarOcupante(b);
                llamarAlSiguiente(b);
                break;
            case REPARAR:
                if (e != TipoEstadoBanio.FUERA_SERVICIO) throw new IllegalStateException("Solo se puede reparar un baño fuera de servicio.");
                banios.iniciarMantenimiento(id);
                banios.finalizarMantenimiento(id);
                liberarOcupante(b);
                llamarAlSiguiente(b);
                break;
        }
    }

    private void liberarOcupante(Banio b) {
        ocupantes.values().removeIf(id -> id == b.getId());
        b.setTieneOcupante(false);
    }

    private void llamarAlSiguiente(Banio b) {
        if (!b.tieneOcupante() && !b.getColaVirtual().estaVacia()) banios.atenderSiguiente(b.getId());
    }
}
