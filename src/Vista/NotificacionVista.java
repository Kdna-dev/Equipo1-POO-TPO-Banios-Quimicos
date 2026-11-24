package Vista;

import Modelo.Aplicacion.Notificacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Avisos pendientes de una persona. */
public final class NotificacionVista {
    private NotificacionVista() { }

    public static List<Map<String, Object>> lista(List<Notificacion> notificaciones) {
        List<Map<String, Object>> r = new ArrayList<>();
        for (Notificacion n : notificaciones) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.id());
            m.put("mensaje", n.mensaje());
            m.put("instante", n.instante().toString());
            r.add(m);
        }
        return r;
    }
}
