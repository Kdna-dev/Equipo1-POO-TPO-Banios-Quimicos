package Controlador;

import Controlador.Http.Controlador;
import Controlador.Http.Peticion;
import Modelo.Aplicacion.AtencionService;
import Modelo.Dominio.Banio;
import Modelo.Dominio.Ubicacion;
import Modelo.EstrategiaBusqueda.EstrategiaOrdenamiento;
import Modelo.EstrategiaBusqueda.MejorOpcion;
import Modelo.EstrategiaBusqueda.PorCantidadEspera;
import Modelo.EstrategiaBusqueda.PorProximidad;
import Modelo.EstrategiaBusqueda.PorTiempoEspera;
import Vista.BanioVista;
import Vista.FuncionalidadesVista;
import Vista.RespuestaVista;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Consulta de baños y cola desde la app del visitante.
 *   GET  /api/banios?lat&lng[&criterio][&funcionalidades][&estados][&distanciaMax]
 *   GET  /api/banios/{id}[?lat&lng]
 *   POST /api/banios/{id}/cola?personaId
 */
public class BanioController implements Controlador {

    private final AtencionService atencion;

    public BanioController(AtencionService atencion) {
        this.atencion = atencion;
    }

    @Override
    public Object atender(Peticion p) {
        if (!p.segmento(0).equals("banios")) return null;
        int n = p.cantidadSegmentos();
        if (p.esGet() && n == 1) return consultar(p);
        if (p.esGet() && n == 2) {
            Banio b = banio(p.segmento(1));
            return BanioVista.detalle(b, p.tiene("lat") ? origen(p) : null);
        }
        if (p.esPost() && n == 3 && p.segmento(2).equals("cola")) {
            Banio b = banio(p.segmento(1));
            atencion.ingresar(b, PersonaController.visitante(atencion, p.requerido("personaId")));
            return RespuestaVista.ok();
        }
        return null;
    }

    private Object consultar(Peticion p) {
        Ubicacion origen = origen(p);
        EstrategiaOrdenamiento estrategia = estrategia(p.parametro("criterio"));
        int mascara = FuncionalidadesVista.mascara(p.parametro("funcionalidades"));
        Set<String> estados = new HashSet<>();
        if (!Peticion.vacio(p.parametro("estados"))) for (String s : p.parametro("estados").split(",")) estados.add(s.trim().toUpperCase());
        Double max = Peticion.vacio(p.parametro("distanciaMax")) ? null : Double.parseDouble(p.parametro("distanciaMax"));

        List<Object> r = new ArrayList<>();
        for (Banio b : atencion.consultar(origen, estrategia, mascara, max)) {
            if (!estados.isEmpty() && !estados.contains(BanioVista.estado(b))) continue;
            r.add(BanioVista.detalle(b, origen));
        }
        return r;
    }

    /** Criterio de ordenamiento pedido por el frontend -> Strategy del modelo. */
    static EstrategiaOrdenamiento estrategia(String codigo) {
        switch (Peticion.vacio(codigo) ? "CERCANIA" : codigo.toUpperCase()) {
            case "CERCANIA": return new PorProximidad();
            case "CANTIDAD_ESPERA": return new PorCantidadEspera();
            case "TIEMPO_ESPERA": return new PorTiempoEspera();
            case "MEJOR_OPCION": return new MejorOpcion();
            default: throw new IllegalArgumentException("Criterio desconocido: " + codigo);
        }
    }

    private Banio banio(String id) {
        return atencion.buscarBanio(Integer.parseInt(id));
    }

    private static Ubicacion origen(Peticion p) {
        return new Ubicacion("Origen", Double.parseDouble(p.requerido("lat")), Double.parseDouble(p.requerido("lng")));
    }
}
