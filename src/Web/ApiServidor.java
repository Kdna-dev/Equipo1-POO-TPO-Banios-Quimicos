package Web;

import Aplicacion.BanioService;
import Aplicacion.ColaService;
import Aplicacion.PersonaService;
import Dominio.Banio;
import Dominio.FuncionalidadesBanio;
import Dominio.Persona;
import Dominio.TipoEstadoBanio;
import Dominio.Ubicacion;
import EstrategiaBusqueda.EstrategiaOrdenamiento;
import EstrategiaBusqueda.MejorOpcion;
import EstrategiaBusqueda.PorCantidadEspera;
import EstrategiaBusqueda.PorProximidad;
import EstrategiaBusqueda.PorTiempoEspera;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Capa HTTP fina sobre el backend del equipo (BanioService / ColaService / PersonaService).
 * No agrega reglas de negocio: las decide el dominio (State, Strategy, Observer) y los servicios.
 * Solo traduce request -> servicio -> JSON, y mantiene el "quién usa qué baño" que el dominio no guarda por persona.
 */
public class ApiServidor {

    private static class NoEncontrado extends RuntimeException {
        NoEncontrado(String m) { super(m); }
    }

    private final BanioService banios;
    private final PersonaService personasSrv;
    private final ColaService colas;
    private final Path raizWeb;
    private final String nombreEvento;
    private final Object candado = new Object();

    private final Notificaciones notificaciones = new Notificaciones();
    private final Map<Integer, Integer> ocupantes = new ConcurrentHashMap<>();   // personaId -> banioId
    private final Map<Integer, Persona> personas = new ConcurrentHashMap<>();    // misma instancia en cola y en repositorio
    private final java.util.Set<Integer> observados = new java.util.HashSet<>();
    private int visitantes = 0;

    public ApiServidor(BanioService banios, PersonaService personasSrv, Path raizWeb, String nombreEvento) {
        this.banios = banios;
        this.personasSrv = personasSrv;
        this.colas = banios.getColaService();
        this.raizWeb = raizWeb.toAbsolutePath().normalize();
        this.nombreEvento = nombreEvento;
        synchronized (candado) { banios.obtenerTodos().forEach(this::observar); }
    }

    public HttpServer iniciar(int puerto) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.createContext("/api/", this::manejarApi);
        server.createContext("/", this::manejarEstatico);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        return server;
    }

    // ------------------------------------------------------------ alta / observer

    /** Alta de un baño; también lo usa el seed. El nombre visible es "Baño #id". */
    public Banio altaBanio(String direccion, double lat, double lng, int capacidad, int mascara, double tiempoPromedio) {
        synchronized (candado) {
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
    }

    private void observar(Banio b) {
        if (observados.add(b.getId())) {
            b.getColaVirtual().agregarObserver(new ObservadorWeb(b.getId(), b.getNombre(), notificaciones, ocupantes));
        }
    }

    /** Suma a la persona a la cola. Si el baño está sin ocupante, el dominio la atiende enseguida. */
    private void ingresar(Banio b, Persona p) {
        if (ocupantes.containsKey(p.getId())) throw new IllegalStateException("Estás usando un baño: terminá antes de sumarte a otra cola.");
        banios.ingresarACola(b.getId(), p);
        TipoEstadoBanio t = b.getTipoEstado();
        if (!b.tieneOcupante() && (t == TipoEstadoBanio.LIBRE || t == TipoEstadoBanio.SATURADO)) banios.atenderSiguiente(b.getId());
    }

    private void liberarOcupante(Banio b) {
        ocupantes.values().removeIf(id -> id == b.getId());
        b.setTieneOcupante(false);
    }

    private void llamarAlSiguiente(Banio b) {
        if (!b.tieneOcupante() && !b.getColaVirtual().estaVacia()) banios.atenderSiguiente(b.getId());
    }

    // ------------------------------------------------------------------ API

    private void manejarApi(HttpExchange ex) throws IOException {
        int status = 200;
        Object cuerpo;
        try {
            ex.getRequestBody().readAllBytes();
            Map<String, String> q = parsearQuery(ex.getRequestURI().getRawQuery());
            synchronized (candado) {
                cuerpo = enrutar(ex.getRequestMethod(), ex.getRequestURI().getPath(), q);
            }
        } catch (NoEncontrado e) {
            status = 404; cuerpo = error(e.getMessage());
        } catch (IllegalStateException e) {
            status = 409; cuerpo = error(e.getMessage());
        } catch (IllegalArgumentException e) {
            status = 400; cuerpo = error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            status = 500; cuerpo = error("Error interno");
        }
        byte[] bytes = Json.escribir(cuerpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private Object enrutar(String metodo, String ruta, Map<String, String> q) {
        List<String> t = new ArrayList<>();
        for (String s : ruta.split("/")) if (!s.isEmpty() && !s.equals("api")) t.add(s);
        boolean get = metodo.equals("GET"), post = metodo.equals("POST");
        int n = t.size();
        String r0 = n > 0 ? t.get(0) : "";

        if (get && n == 1 && r0.equals("evento")) return evento();
        if (get && n == 1 && r0.equals("banios")) return consultar(q);
        if (get && n == 2 && r0.equals("banios")) return vista(banio(t.get(1)), q.containsKey("lat") ? origen(q) : null);

        if (post && n == 1 && r0.equals("personas")) {
            Persona p = personasSrv.registrarPersona("Visitante", "Anónimo", 0, "anon-" + UUID.randomUUID() + "@ejemplo.local");
            personas.put(p.getId(), p);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", String.valueOf(p.getId()));
            m.put("nombre", "Visitante");
            return m;
        }
        if (get && n == 3 && r0.equals("personas") && t.get(2).equals("cola")) return seguimiento(persona(t.get(1)));
        if (get && n == 3 && r0.equals("personas") && t.get(2).equals("notificaciones")) {
            Persona p = persona(t.get(1));
            return notificaciones.desde(p.getId(), vacio(q.get("desde")) ? 0 : Long.parseLong(q.get("desde")));
        }
        if (post && n == 3 && r0.equals("banios") && t.get(2).equals("cola")) {
            ingresar(banio(t.get(1)), persona(requerido(q, "personaId")));
            return ok();
        }
        if (post && n == 2 && r0.equals("cola") && t.get(1).equals("salir")) {
            Persona p = persona(requerido(q, "personaId"));
            Integer id = colas.obtenerBanioIdDePersona(p);
            if (id == null || !banios.salirDeCola(id, p)) throw new IllegalStateException("No estás en ninguna cola.");
            return ok();
        }
        if (post && n == 2 && r0.equals("uso") && t.get(1).equals("salir")) {
            salirDelBanio(persona(requerido(q, "personaId")), "mala".equalsIgnoreCase(q.get("higiene")));
            return ok();
        }
        if (r0.equals("admin")) return enrutarAdmin(metodo, t, q);
        throw new NoEncontrado("Ruta desconocida: " + metodo + " " + ruta);
    }

    private void salirDelBanio(Persona p, boolean sucio) {
        Integer id = ocupantes.get(p.getId());
        if (id == null) throw new IllegalStateException("No estás usando ningún baño.");
        Banio b = banio(String.valueOf(id));
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

    private Object enrutarAdmin(String metodo, List<String> t, Map<String, String> q) {
        boolean get = metodo.equals("GET"), post = metodo.equals("POST");
        int n = t.size();
        if (get && n == 2 && t.get(1).equals("banios")) {
            List<Object> r = new ArrayList<>();
            for (Banio b : banios.obtenerTodos()) { observar(b); r.add(vista(b, null)); }
            return r;
        }
        if (post && n == 2 && t.get(1).equals("banios")) {
            Banio b = altaBanio(q.getOrDefault("direccion", ""), Double.parseDouble(requerido(q, "lat")), Double.parseDouble(requerido(q, "lng")),
                    Integer.parseInt(q.getOrDefault("capacidad", "5")), mascara(q.get("funcionalidades")),
                    Double.parseDouble(q.getOrDefault("tiempoPromedio", "3")));
            return Collections.singletonMap("id", b.getId());
        }
        if (post && n == 4 && t.get(1).equals("banios")) {
            Banio b = banio(t.get(2));
            switch (t.get(3)) {
                case "baja":
                    for (Persona p : new ArrayList<>(b.getColaVirtual().getPersonas())) banios.salirDeCola(b.getId(), p);
                    liberarOcupante(b);
                    banios.eliminarBanio(b.getId());
                    observados.remove(b.getId());
                    return ok();
                case "accion":
                    accion(b, requerido(q, "tipo"));
                    return ok();
                case "simular": {
                    int pedidas = Integer.parseInt(q.getOrDefault("cantidad", "3")), agregadas = 0;
                    for (int i = 0; i < pedidas; i++) {
                        Persona p = personasSrv.registrarPersona("Visitante", "N°" + (++visitantes), 18, "visitante" + visitantes + "@ejemplo.local");
                        personas.put(p.getId(), p);
                        try { ingresar(b, p); agregadas++; } catch (IllegalStateException e) { break; }   // cola llena o baño sin ingreso
                    }
                    return Collections.singletonMap("agregadas", agregadas);
                }
                default: break;
            }
        }
        throw new NoEncontrado("Ruta de administración desconocida");
    }

    private void accion(Banio b, String tipo) {
        int id = b.getId();
        TipoEstadoBanio e = b.getTipoEstado();
        switch (tipo) {
            case "DESCOMPONER":
                banios.descomponerBanio(id);
                break;
            case "INICIAR_MANTENIMIENTO":      // el State solo permite mantenimiento desde "fuera de servicio"
                if (e != TipoEstadoBanio.FUERA_SERVICIO && e != TipoEstadoBanio.MANTENIMIENTO) banios.descomponerBanio(id);
                if (b.getTipoEstado() == TipoEstadoBanio.FUERA_SERVICIO) banios.iniciarMantenimiento(id);
                break;
            case "FINALIZAR_MANTENIMIENTO":
                banios.finalizarMantenimiento(id);
                liberarOcupante(b);
                llamarAlSiguiente(b);
                break;
            case "REPARAR":
                if (e != TipoEstadoBanio.FUERA_SERVICIO) throw new IllegalStateException("Solo se puede reparar un baño fuera de servicio.");
                banios.iniciarMantenimiento(id);
                banios.finalizarMantenimiento(id);
                liberarOcupante(b);
                llamarAlSiguiente(b);
                break;
            default:
                throw new IllegalArgumentException("Acción desconocida: " + tipo);
        }
    }

    // ------------------------------------------------------------- consultas

    private Object evento() {
        int operativos = 0, esperando = 0;
        for (Banio b : banios.obtenerTodos()) {
            TipoEstadoBanio e = b.getTipoEstado();
            if (e == TipoEstadoBanio.LIBRE || e == TipoEstadoBanio.OCUPADO || e == TipoEstadoBanio.SATURADO) operativos++;
            esperando += b.getCantidadEnEspera();
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("baniosOperativos", operativos);
        resumen.put("personasEsperando", esperando);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", 1);
        m.put("nombre", nombreEvento);
        m.put("fecha", LocalDate.now());
        m.put("resumen", resumen);
        return m;
    }

    private Object consultar(Map<String, String> q) {
        Ubicacion origen = origen(q);
        List<Banio> lista = banios.consultarBanios(origen, estrategia(q.get("criterio")), mascara(q.get("funcionalidades")), true);
        java.util.Set<String> estados = new java.util.HashSet<>();
        if (!vacio(q.get("estados"))) for (String s : q.get("estados").split(",")) estados.add(s.trim().toUpperCase());
        Double max = vacio(q.get("distanciaMax")) ? null : Double.parseDouble(q.get("distanciaMax"));
        List<Object> r = new ArrayList<>();
        for (Banio b : lista) {
            observar(b);
            if (max != null && b.calcularDistanciaA(origen, null) > max) continue;
            if (!estados.isEmpty() && !estados.contains(estadoFront(b))) continue;
            r.add(vista(b, origen));
        }
        return r;
    }

    private static EstrategiaOrdenamiento estrategia(String codigo) {
        switch (vacio(codigo) ? "CERCANIA" : codigo.toUpperCase()) {
            case "CERCANIA": return new PorProximidad();
            case "CANTIDAD_ESPERA": return new PorCantidadEspera();
            case "TIEMPO_ESPERA": return new PorTiempoEspera();
            case "MEJOR_OPCION": return new MejorOpcion();
            default: throw new IllegalArgumentException("Criterio desconocido: " + codigo);
        }
    }

    private Map<String, Object> vista(Banio b, Ubicacion origen) {
        Map<String, Object> d = new LinkedHashMap<>();
        Ubicacion u = b.getUbicacion();
        d.put("id", b.getId());
        d.put("nombre", b.getNombre());
        d.put("direccion", u == null ? "" : u.getNombre());
        d.put("latitud", u == null ? null : u.getLatitud());
        d.put("longitud", u == null ? null : u.getLongitud());
        d.put("distanciaMetros", origen == null || u == null ? null : Math.round(b.calcularDistanciaA(origen, null)));
        d.put("estado", estadoFront(b));
        d.put("capacidad", b.getCapacidad());
        d.put("funcionalidades", nombresFuncionalidades(b.getFuncionalidades()));
        d.put("personasEsperando", b.getCantidadEnEspera());
        d.put("tiempoEstimadoMin", (long) Math.ceil(b.getTiempoEstimadoEspera()));
        d.put("puedeIngresar", b.permiteIngreso());
        d.put("mantenimientoRestanteSeg", null);   // el dominio del equipo no modela cooldown de mantenimiento
        d.put("motivoMantenimiento", null);
        return d;
    }

    private Object seguimiento(Persona p) {
        Integer enUso = ocupantes.get(p.getId());
        Integer enCola = colas.obtenerBanioIdDePersona(p);
        Integer id = enUso != null ? enUso : enCola;
        if (id == null) return Collections.singletonMap("enCola", false);
        Banio b = banios.buscarPorId(id);
        if (b == null) return Collections.singletonMap("enCola", false);
        boolean usando = enUso != null;
        int pos = usando ? 0 : b.getColaVirtual().getPosicion(p);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("enCola", true);
        d.put("banioId", b.getId());
        d.put("nombreBanio", b.getNombre());
        d.put("direccion", b.getUbicacion() == null ? "" : b.getUbicacion().getNombre());
        d.put("estado", estadoFront(b));
        d.put("usandoBanio", usando);
        d.put("posicion", pos);
        d.put("personasDelante", usando ? 0 : pos - 1);
        d.put("tiempoEstimadoMin", usando ? 0L : (long) Math.ceil(((pos - 1) + (b.tieneOcupante() ? 1 : 0)) * b.getTiempoPromedioAtencionMinutos()));
        d.put("esMiTurno", false);   // al llegar el turno el dominio ya la mete al baño (usandoBanio = true)
        d.put("mantenimientoRestanteSeg", null);
        d.put("motivoMantenimiento", null);
        return d;
    }

    // ------------------------------------------------------------ utilidades

    private static String estadoFront(Banio b) {
        return b.getTipoEstado() == TipoEstadoBanio.FUERA_SERVICIO ? "FUERA_DE_SERVICIO" : b.getTipoEstado().name();
    }

    private static String nombreFront(FuncionalidadesBanio f) {
        return f == FuncionalidadesBanio.ACCESIBILIDAD_MOVILIDAD_REDUCIDA ? "MOVILIDAD_REDUCIDA" : f.name();
    }

    private static List<String> nombresFuncionalidades(int mascara) {
        List<String> r = new ArrayList<>();
        for (FuncionalidadesBanio f : FuncionalidadesBanio.fromMask(mascara)) r.add(nombreFront(f));
        return r;
    }

    /** Nombres de funcionalidad (CSV) -> bitmask del dominio. El frontend nunca ve la máscara. */
    private static int mascara(String csv) {
        EnumSet<FuncionalidadesBanio> set = EnumSet.noneOf(FuncionalidadesBanio.class);
        if (!vacio(csv)) for (String s : csv.split(",")) {
            String nombre = s.trim().toUpperCase();
            if (nombre.isEmpty()) continue;
            boolean encontrada = false;
            for (FuncionalidadesBanio f : FuncionalidadesBanio.values()) {
                if (nombreFront(f).equals(nombre)) { set.add(f); encontrada = true; }
            }
            if (!encontrada) throw new IllegalArgumentException("Funcionalidad desconocida: " + s);
        }
        return FuncionalidadesBanio.toMask(set);
    }

    private Banio banio(String id) {
        Banio b = banios.buscarPorId(Integer.parseInt(id));
        if (b == null) throw new NoEncontrado("No existe el baño " + id);
        observar(b);
        return b;
    }

    private Persona persona(String id) {
        Persona p = personas.get(Integer.parseInt(id));
        if (p == null) throw new NoEncontrado("No existe la persona " + id);
        return p;
    }

    private Ubicacion origen(Map<String, String> q) {
        return new Ubicacion("Origen", Double.parseDouble(requerido(q, "lat")), Double.parseDouble(requerido(q, "lng")));
    }

    private static String requerido(Map<String, String> q, String clave) {
        String v = q.get(clave);
        if (vacio(v)) throw new IllegalArgumentException("Falta el parámetro '" + clave + "'");
        return v;
    }

    private static boolean vacio(String s) { return s == null || s.trim().isEmpty(); }

    private static Map<String, Object> ok() { return Collections.<String, Object>singletonMap("ok", true); }
    private static Map<String, Object> error(String mensaje) { return Collections.<String, Object>singletonMap("error", mensaje); }

    private static Map<String, String> parsearQuery(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String par : raw.split("&")) {
            int i = par.indexOf('=');
            String k = i < 0 ? par : par.substring(0, i);
            String v = i < 0 ? "" : par.substring(i + 1);
            m.put(decodificar(k), decodificar(v));
        }
        return m;
    }

    private static String decodificar(String s) {
        try { return URLDecoder.decode(s, "UTF-8"); } catch (java.io.UnsupportedEncodingException e) { throw new IllegalStateException(e); }
    }

    // ------------------------------------------------------------- estáticos

    private void manejarEstatico(HttpExchange ex) throws IOException {
        String ruta = ex.getRequestURI().getPath();
        if (ruta.equals("/")) ruta = "/index.html";
        Path archivo = raizWeb.resolve(ruta.substring(1)).normalize();
        if (!archivo.startsWith(raizWeb) || !Files.isRegularFile(archivo)) {
            byte[] msg = "No encontrado".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, msg.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(msg); }
            return;
        }
        byte[] bytes = Files.readAllBytes(archivo);
        ex.getResponseHeaders().set("Content-Type", tipoContenido(archivo.getFileName().toString()));
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String tipoContenido(String nombre) {
        if (nombre.endsWith(".html")) return "text/html; charset=utf-8";
        if (nombre.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (nombre.endsWith(".css")) return "text/css; charset=utf-8";
        if (nombre.endsWith(".json") || nombre.endsWith(".webmanifest")) return "application/json; charset=utf-8";
        if (nombre.endsWith(".svg")) return "image/svg+xml";
        if (nombre.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
