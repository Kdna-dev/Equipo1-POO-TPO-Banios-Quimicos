package Vista;

import Modelo.Aplicacion.ResumenEvento;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cabecera del evento con su resumen. */
public final class EventoVista {
    private EventoVista() { }

    public static Map<String, Object> de(String nombreEvento, ResumenEvento r) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("baniosOperativos", r.baniosOperativos());
        resumen.put("personasEsperando", r.personasEsperando());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", 1);
        m.put("nombre", nombreEvento);
        m.put("fecha", LocalDate.now());
        m.put("resumen", resumen);
        return m;
    }
}
