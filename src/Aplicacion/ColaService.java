package Aplicacion;

import Dominio.Banio;
import Dominio.Persona;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ColaService {

    // Registro de pertenencia activa: personaId -> banioId
    private final Map<Integer, Integer> colasActivas = new ConcurrentHashMap<>();

    public void ingresarACola(Banio banio, Persona persona) {
        if (banio == null || persona == null) {
            throw new IllegalArgumentException("El baño y la persona no pueden ser nulos.");
        }

        int personaId = persona.getId();
        if (estaEnAlgunaCola(persona)) {
            int banioActualId = colasActivas.get(personaId);
            throw new IllegalStateException(String.format(
                    "La persona %s %s ya se encuentra en la cola del baño con ID %d. Una persona no puede estar en dos colas a la vez.",
                    persona.getNombre(), persona.getApellido(), banioActualId
            ));
        }

        // Delegación en el patrón State del baño
        banio.ingresarACola(persona);

        // Registro de la cola activa para la persona
        colasActivas.put(personaId, banio.getId());
    }

    public boolean salirDeCola(Banio banio, Persona persona) {
        if (banio == null || persona == null) {
            return false;
        }

        boolean removido = banio.retirarDeCola(persona);
        if (removido) {
            colasActivas.remove(persona.getId());
        }
        return removido;
    }

    public Persona atenderSiguiente(Banio banio) {
        if (banio == null) {
            return null;
        }

        Persona atendida = banio.atenderSiguiente();
        if (atendida != null) {
            colasActivas.remove(atendida.getId());
        }
        return atendida;
    }

    public boolean estaEnAlgunaCola(Persona persona) {
        if (persona == null) {
            return false;
        }
        return colasActivas.containsKey(persona.getId());
    }

    public Integer obtenerBanioIdDePersona(Persona persona) {
        if (persona == null) {
            return null;
        }
        return colasActivas.get(persona.getId());
    }

    public int obtenerPosicion(Banio banio, Persona persona) {
        if (banio == null || persona == null) {
            return -1;
        }
        return banio.getColaVirtual().getPosicion(persona);
    }
}
