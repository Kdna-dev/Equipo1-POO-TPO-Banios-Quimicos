package Dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ColaVirtual {

    private final List<Persona> cola = new ArrayList<>();
    private final List<ColaVirtualObserver> observadores = new ArrayList<>();

    public void agregarObserver(ColaVirtualObserver observer) {
        if (observer != null && !observadores.contains(observer)) {
            observadores.add(observer);
        }
    }

    public void removerObserver(ColaVirtualObserver observer) {
        observadores.remove(observer);
    }

    public void agregarPersona(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser nula.");
        }
        if (cola.contains(persona)) {
            throw new IllegalStateException("La persona ya se encuentra en esta cola.");
        }
        cola.add(persona);
        notificarIngreso(persona);
        notificarCambioPosicionOTiempo();
    }

    public boolean retirarPersona(Persona persona) {
        boolean removido = cola.remove(persona);
        if (removido) {
            notificarSalida(persona);
            notificarCambioPosicionOTiempo();
        }
        return removido;
    }

    public Persona atenderSiguiente() {
        if (cola.isEmpty()) {
            return null;
        }
        Persona atendida = cola.remove(0);
        notificarAtencion(atendida);
        notificarCambioPosicionOTiempo();
        return atendida;
    }

    public int getCantidadPersonas() {
        return cola.size();
    }

    public int getPosicion(Persona persona) {
        int index = cola.indexOf(persona);
        return (index >= 0) ? index + 1 : -1;
    }

    public List<Persona> getPersonas() {
        return Collections.unmodifiableList(cola);
    }

    public boolean contiene(Persona persona) {
        return cola.contains(persona);
    }

    public boolean estaVacia() {
        return cola.isEmpty();
    }

    // Notificaciones a observers
    private void notificarIngreso(Persona persona) {
        for (ColaVirtualObserver obs : new ArrayList<>(observadores)) {
            obs.alIngresarPersona(this, persona);
        }
    }

    private void notificarSalida(Persona persona) {
        for (ColaVirtualObserver obs : new ArrayList<>(observadores)) {
            obs.alSalirPersona(this, persona);
        }
    }

    private void notificarAtencion(Persona persona) {
        for (ColaVirtualObserver obs : new ArrayList<>(observadores)) {
            obs.alAtenderSiguiente(this, persona);
        }
    }

    private void notificarCambioPosicionOTiempo() {
        for (ColaVirtualObserver obs : new ArrayList<>(observadores)) {
            obs.alCambiarPosicionOTiempo(this);
        }
    }
}
