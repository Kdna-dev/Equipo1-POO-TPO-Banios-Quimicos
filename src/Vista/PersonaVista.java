package Vista;

import Modelo.Dominio.Persona;

import java.util.LinkedHashMap;
import java.util.Map;

/** Visitante recién registrado (el frontend guarda su id). */
public final class PersonaVista {
    private PersonaVista() { }

    public static Map<String, Object> visitante(Persona p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(p.getId()));
        m.put("nombre", "Visitante");
        return m;
    }
}
