package Vista;

import Modelo.Aplicacion.Seguimiento;
import Modelo.Dominio.Banio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Seguimiento de la cola de una persona, para la pantalla "Mi turno". */
public final class SeguimientoVista {
    private SeguimientoVista() { }

    public static Map<String, Object> de(Seguimiento s) {
        if (s == null) return Collections.singletonMap("enCola", false);
        Banio b = s.banio();
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("enCola", true);
        d.put("banioId", b.getId());
        d.put("nombreBanio", b.getNombre());
        d.put("direccion", b.getUbicacion() == null ? "" : b.getUbicacion().getNombre());
        d.put("estado", BanioVista.estado(b));
        d.put("usandoBanio", s.usandoBanio());
        d.put("posicion", s.posicion());
        d.put("personasDelante", s.personasDelante());
        d.put("tiempoEstimadoMin", s.tiempoEstimadoMin());
        d.put("esMiTurno", false);   // al llegar el turno el dominio ya la mete al baño (usandoBanio = true)
        d.put("mantenimientoRestanteSeg", null);
        d.put("motivoMantenimiento", null);
        return d;
    }
}
