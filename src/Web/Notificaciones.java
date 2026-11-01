package Web;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Buzón de avisos por persona. Lo llena el observer de la cola; el frontend lo consulta por polling. */
public class Notificaciones {

    private static final int MAX_POR_PERSONA = 50;
    private final Map<Integer, List<Map<String, Object>>> porPersona = new HashMap<>();
    private long secuencia = 0;

    public synchronized void avisar(int personaId, String mensaje) {
        List<Map<String, Object>> lista = porPersona.computeIfAbsent(personaId, k -> new ArrayList<>());
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("id", ++secuencia);
        n.put("mensaje", mensaje);
        n.put("instante", Instant.now().toString());
        lista.add(n);
        if (lista.size() > MAX_POR_PERSONA) lista.remove(0);
    }

    public synchronized List<Map<String, Object>> desde(int personaId, long ultimoId) {
        List<Map<String, Object>> r = new ArrayList<>();
        for (Map<String, Object> n : porPersona.getOrDefault(personaId, new ArrayList<>())) {
            if (((Long) n.get("id")) > ultimoId) r.add(n);
        }
        return r;
    }
}
