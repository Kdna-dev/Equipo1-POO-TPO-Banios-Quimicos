package Web;

import Dominio.ColaVirtual;
import Dominio.ColaVirtualObserver;
import Dominio.Persona;

import java.util.HashMap;
import java.util.Map;

/**
 * Observer de la cola de un baño (patrón Observer del dominio).
 * Traduce los eventos de la cola a avisos para el usuario y registra quién está usando el baño.
 */
public class ObservadorWeb implements ColaVirtualObserver {

    private final int banioId;
    private final String nombreBanio;
    private final Notificaciones notificaciones;
    private final Map<Integer, Integer> ocupantes;            // personaId -> banioId (compartido)
    private final Map<Integer, Integer> ultimasPosiciones = new HashMap<>();

    public ObservadorWeb(int banioId, String nombreBanio, Notificaciones notificaciones, Map<Integer, Integer> ocupantes) {
        this.banioId = banioId;
        this.nombreBanio = nombreBanio;
        this.notificaciones = notificaciones;
        this.ocupantes = ocupantes;
    }

    @Override
    public void alSalirPersona(ColaVirtual cola, Persona persona) {
        ultimasPosiciones.remove(persona.getId());
    }

    @Override
    public void alAtenderSiguiente(ColaVirtual cola, Persona persona) {
        ultimasPosiciones.remove(persona.getId());
        ocupantes.put(persona.getId(), banioId);
        notificaciones.avisar(persona.getId(), "¡Es tu turno! Pasá al " + nombreBanio);
    }

    @Override
    public void alCambiarPosicionOTiempo(ColaVirtual cola) {
        int pos = 0;
        for (Persona p : cola.getPersonas()) {
            pos++;
            Integer anterior = ultimasPosiciones.put(p.getId(), pos);
            if (anterior != null && pos < anterior) {
                notificaciones.avisar(p.getId(), pos == 1 ? "Sos el siguiente en el " + nombreBanio
                        : "Avanzaste a la posición " + pos + " en el " + nombreBanio);
            }
        }
    }
}
