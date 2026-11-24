package Vista;

import Modelo.Dominio.Banio;
import Modelo.Dominio.TipoEstadoBanio;
import Modelo.Dominio.Ubicacion;

import java.util.LinkedHashMap;
import java.util.Map;

/** Representación de un baño tal como la consume el frontend. */
public final class BanioVista {
    private BanioVista() { }

    public static Map<String, Object> detalle(Banio b, Ubicacion origen) {
        Map<String, Object> d = new LinkedHashMap<>();
        Ubicacion u = b.getUbicacion();
        d.put("id", b.getId());
        d.put("nombre", b.getNombre());
        d.put("direccion", u == null ? "" : u.getNombre());
        d.put("latitud", u == null ? null : u.getLatitud());
        d.put("longitud", u == null ? null : u.getLongitud());
        d.put("distanciaMetros", origen == null || u == null ? null : Math.round(b.calcularDistanciaA(origen, null)));
        d.put("estado", estado(b));
        d.put("capacidad", b.getCapacidad());
        d.put("funcionalidades", FuncionalidadesVista.nombres(b.getFuncionalidades()));
        d.put("personasEsperando", b.getCantidadEnEspera());
        d.put("tiempoEstimadoMin", (long) Math.ceil(b.getTiempoEstimadoEspera()));
        d.put("puedeIngresar", b.permiteIngreso());
        d.put("mantenimientoRestanteSeg", null);   // el dominio del equipo no modela cooldown de mantenimiento
        d.put("motivoMantenimiento", null);
        return d;
    }

    /** Nombre del estado que usa el frontend (difiere del enum del dominio en FUERA_SERVICIO). */
    public static String estado(Banio b) {
        return b.getTipoEstado() == TipoEstadoBanio.FUERA_SERVICIO ? "FUERA_DE_SERVICIO" : b.getTipoEstado().name();
    }
}
