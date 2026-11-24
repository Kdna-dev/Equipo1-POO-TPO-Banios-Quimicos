package Modelo.Aplicacion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Buzón de avisos por persona. Lo llena el observer de la cola; el frontend lo consulta por polling. */
public class Notificaciones {

    private static final int MAX_POR_PERSONA = 50;
    private final Map<Integer, List<Notificacion>> porPersona = new HashMap<>();
    private long secuencia = 0;

    public synchronized void avisar(int personaId, String mensaje) {
        List<Notificacion> lista = porPersona.computeIfAbsent(personaId, k -> new ArrayList<>());
        lista.add(new Notificacion(++secuencia, mensaje, Instant.now()));
        if (lista.size() > MAX_POR_PERSONA) lista.remove(0);
    }

    public synchronized List<Notificacion> desde(int personaId, long ultimoId) {
        List<Notificacion> r = new ArrayList<>();
        for (Notificacion n : porPersona.getOrDefault(personaId, new ArrayList<>())) {
            if (n.id() > ultimoId) r.add(n);
        }
        return r;
    }
}
