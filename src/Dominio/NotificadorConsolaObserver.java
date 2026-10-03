package Dominio;

public class NotificadorConsolaObserver implements ColaVirtualObserver {

    private final String nombreBanio;

    public NotificadorConsolaObserver(String nombreBanio) {
        this.nombreBanio = nombreBanio;
    }

    @Override
    public void alIngresarPersona(ColaVirtual cola, Persona persona) {
        System.out.printf("[OBSERVER - %s] Se incorporó %s %s a la cola. Posición: %d. Total esperando: %d%n",
                nombreBanio, persona.getNombre(), persona.getApellido(), cola.getPosicion(persona), cola.getCantidadPersonas());
    }

    @Override
    public void alSalirPersona(ColaVirtual cola, Persona persona) {
        System.out.printf("[OBSERVER - %s] %s %s abandonó la cola. Total esperando: %d%n",
                nombreBanio, persona.getNombre(), persona.getApellido(), cola.getCantidadPersonas());
    }

    @Override
    public void alAtenderSiguiente(ColaVirtual cola, Persona personaAtendida) {
        System.out.printf("[OBSERVER - %s] ¡Turno de %s %s! Ingresa al baño. Restantes en cola: %d%n",
                nombreBanio, personaAtendida.getNombre(), personaAtendida.getApellido(), cola.getCantidadPersonas());
    }

    @Override
    public void alCambiarPosicionOTiempo(ColaVirtual cola) {
        System.out.printf("[OBSERVER - %s] Cola actualizada. Total en espera: %d personas.%n",
                nombreBanio, cola.getCantidadPersonas());
    }
}
