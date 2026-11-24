package Modelo.Aplicacion;

/** Se pidió un baño o una persona que no existe. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) { super(mensaje); }
}
