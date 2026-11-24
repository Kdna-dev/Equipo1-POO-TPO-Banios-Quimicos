package Controlador.Http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Request HTTP ya parseada: método, segmentos de la ruta (sin el prefijo "api") y query string. */
public class Peticion {

    private final String metodo;
    private final String ruta;
    private final List<String> segmentos;
    private final Map<String, String> query;

    public Peticion(String metodo, String ruta, String rawQuery) {
        this.metodo = metodo;
        this.ruta = ruta;
        List<String> t = new ArrayList<>();
        for (String s : ruta.split("/")) if (!s.isEmpty() && !s.equals("api")) t.add(s);
        this.segmentos = Collections.unmodifiableList(t);
        this.query = parsearQuery(rawQuery);
    }

    public String metodo() { return metodo; }
    public String ruta() { return ruta; }
    public boolean esGet() { return metodo.equals("GET"); }
    public boolean esPost() { return metodo.equals("POST"); }

    public int cantidadSegmentos() { return segmentos.size(); }

    public String segmento(int i) { return i < segmentos.size() ? segmentos.get(i) : ""; }

    public boolean tiene(String clave) { return query.containsKey(clave); }

    public String parametro(String clave) { return query.get(clave); }

    public String parametro(String clave, String porDefecto) { return query.getOrDefault(clave, porDefecto); }

    public String requerido(String clave) {
        String v = query.get(clave);
        if (vacio(v)) throw new IllegalArgumentException("Falta el parámetro '" + clave + "'");
        return v;
    }

    public static boolean vacio(String s) { return s == null || s.trim().isEmpty(); }

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
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
