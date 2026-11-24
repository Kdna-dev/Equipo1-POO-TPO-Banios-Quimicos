package Controlador;

import Controlador.Http.Controlador;
import Controlador.Http.Peticion;
import Modelo.Aplicacion.AtencionService;
import Vista.EventoVista;

/** GET /api/evento */
public class EventoController implements Controlador {

    private final AtencionService atencion;
    private final String nombreEvento;

    public EventoController(AtencionService atencion, String nombreEvento) {
        this.atencion = atencion;
        this.nombreEvento = nombreEvento;
    }

    @Override
    public Object atender(Peticion p) {
        if (p.esGet() && p.cantidadSegmentos() == 1 && p.segmento(0).equals("evento")) {
            return EventoVista.de(nombreEvento, atencion.resumen());
        }
        return null;
    }
}
