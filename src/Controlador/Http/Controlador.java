package Controlador.Http;

/** Un controlador atiende un grupo de rutas de la API. */
public interface Controlador {

    /**
     * Atiende la petición y devuelve el modelo de respuesta (Map/List) que se serializa a JSON,
     * o null si la ruta no le corresponde a este controlador.
     */
    Object atender(Peticion p);
}
