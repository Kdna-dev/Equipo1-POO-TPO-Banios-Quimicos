package Controlador;

import Controlador.Http.Controlador;
import Controlador.Http.Peticion;
import Controlador.Http.RutaNoEncontradaException;
import Modelo.Aplicacion.AccionBanio;
import Modelo.Aplicacion.AtencionService;
import Modelo.Dominio.Banio;
import Vista.BanioVista;
import Vista.FuncionalidadesVista;
import Vista.RespuestaVista;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de administración.
 *   GET  /api/admin/banios
 *   POST /api/admin/banios?direccion&lat&lng[&capacidad][&funcionalidades][&tiempoPromedio]
 *   POST /api/admin/banios/{id}/baja
 *   POST /api/admin/banios/{id}/accion?tipo
 *   POST /api/admin/banios/{id}/simular[?cantidad]
 */
public class AdminController implements Controlador {

    private final AtencionService atencion;

    public AdminController(AtencionService atencion) {
        this.atencion = atencion;
    }

    @Override
    public Object atender(Peticion p) {
        if (!p.segmento(0).equals("admin")) return null;
        int n = p.cantidadSegmentos();

        if (p.esGet() && n == 2 && p.segmento(1).equals("banios")) {
            List<Object> r = new ArrayList<>();
            for (Banio b : atencion.todosLosBanios()) r.add(BanioVista.detalle(b, null));
            return r;
        }
        if (p.esPost() && n == 2 && p.segmento(1).equals("banios")) {
            String direccion = p.parametro("direccion", "");
            double lat = Double.parseDouble(p.requerido("lat"));
            double lng = Double.parseDouble(p.requerido("lng"));
            int capacidad = Integer.parseInt(p.parametro("capacidad", "5"));
            int mascara = FuncionalidadesVista.mascara(p.parametro("funcionalidades"));
            double tiempoPromedio = Double.parseDouble(p.parametro("tiempoPromedio", "3"));
            Banio b = atencion.altaBanio(direccion, lat, lng, capacidad, mascara, tiempoPromedio);
            return RespuestaVista.campo("id", b.getId());
        }
        if (p.esPost() && n == 4 && p.segmento(1).equals("banios")) {
            Banio b = atencion.buscarBanio(Integer.parseInt(p.segmento(2)));
            switch (p.segmento(3)) {
                case "baja":
                    atencion.darDeBaja(b);
                    return RespuestaVista.ok();
                case "accion":
                    atencion.ejecutarAccion(b, AccionBanio.desde(p.requerido("tipo")));
                    return RespuestaVista.ok();
                case "simular":
                    int agregadas = atencion.simularVisitantes(b, Integer.parseInt(p.parametro("cantidad", "3")));
                    return RespuestaVista.campo("agregadas", agregadas);
                default:
                    break;
            }
        }
        throw new RutaNoEncontradaException("Ruta de administración desconocida");
    }
}
