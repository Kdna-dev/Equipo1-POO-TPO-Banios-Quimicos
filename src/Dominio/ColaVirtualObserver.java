package Dominio;

public interface ColaVirtualObserver {

    default void alIngresarPersona(ColaVirtual cola, Persona persona) {}

    default void alSalirPersona(ColaVirtual cola, Persona persona) {}

    default void alAtenderSiguiente(ColaVirtual cola, Persona personaAtendida) {}

    default void alCambiarPosicionOTiempo(ColaVirtual cola) {}
}
