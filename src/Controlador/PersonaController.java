package Controlador;

import Controlador.Http.Controlador;
import Controlador.Http.Peticion;
import Modelo.Aplicacion.AtencionService;
import Modelo.Dominio.Persona;
import Vista.NotificacionVista;
import Vista.PersonaVista;
import Vista.RespuestaVista;
import Vista.SeguimientoVista;

/**
 * Visitantes: registro, seguimiento de su turno y salida.
 *   POST /api/personas
 *   GET  /api/personas/{id}/cola
 *   GET  /api/personas/{id}/notificaciones[?desde]
 *   POST /api/cola/salir?personaId
 *   POST /api/uso/salir?personaId[&higiene=mala]
 */
public class PersonaController implements Controlador {

    private final AtencionService atencion;

    public PersonaController(AtencionService atencion) {
        this.atencion = atencion;
    }

    @Override
    public Object atender(Peticion p) {
        int n = p.cantidadSegmentos();
        String r0 = p.segmento(0);

        if (p.esPost() && n == 1 && r0.equals("personas")) {
            return PersonaVista.visitante(atencion.registrarVisitante());
        }
        if (p.esGet() && n == 3 && r0.equals("personas") && p.segmento(2).equals("cola")) {
            return SeguimientoVista.de(atencion.seguimiento(visitante(atencion, p.segmento(1))));
        }
        if (p.esGet() && n == 3 && r0.equals("personas") && p.segmento(2).equals("notificaciones")) {
            Persona persona = visitante(atencion, p.segmento(1));
            long desde = Peticion.vacio(p.parametro("desde")) ? 0 : Long.parseLong(p.parametro("desde"));
            return NotificacionVista.lista(atencion.notificacionesDesde(persona, desde));
        }
        if (p.esPost() && n == 2 && r0.equals("cola") && p.segmento(1).equals("salir")) {
            atencion.salirDeCola(visitante(atencion, p.requerido("personaId")));
            return RespuestaVista.ok();
        }
        if (p.esPost() && n == 2 && r0.equals("uso") && p.segmento(1).equals("salir")) {
            atencion.salirDelBanio(visitante(atencion, p.requerido("personaId")), "mala".equalsIgnoreCase(p.parametro("higiene")));
            return RespuestaVista.ok();
        }
        return null;
    }

    static Persona visitante(AtencionService atencion, String id) {
        return atencion.buscarVisitante(Integer.parseInt(id));
    }
}
