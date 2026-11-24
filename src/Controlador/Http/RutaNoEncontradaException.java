package Controlador.Http;

/** Ninguna ruta de la API coincide con la petición. */
public class RutaNoEncontradaException extends RuntimeException {
    public RutaNoEncontradaException(String mensaje) { super(mensaje); }
}
