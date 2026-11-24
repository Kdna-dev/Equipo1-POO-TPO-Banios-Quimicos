package Vista;

import Modelo.Dominio.FuncionalidadesBanio;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Traducción entre los nombres de funcionalidad del frontend y la máscara de bits del dominio. */
public final class FuncionalidadesVista {
    private FuncionalidadesVista() { }

    private static String nombre(FuncionalidadesBanio f) {
        return f == FuncionalidadesBanio.ACCESIBILIDAD_MOVILIDAD_REDUCIDA ? "MOVILIDAD_REDUCIDA" : f.name();
    }

    public static List<String> nombres(int mascara) {
        List<String> r = new ArrayList<>();
        for (FuncionalidadesBanio f : FuncionalidadesBanio.fromMask(mascara)) r.add(nombre(f));
        return r;
    }

    /** Nombres de funcionalidad (CSV) -> bitmask del dominio. El frontend nunca ve la máscara. */
    public static int mascara(String csv) {
        EnumSet<FuncionalidadesBanio> set = EnumSet.noneOf(FuncionalidadesBanio.class);
        if (csv != null && !csv.trim().isEmpty()) for (String s : csv.split(",")) {
            String buscado = s.trim().toUpperCase();
            if (buscado.isEmpty()) continue;
            boolean encontrada = false;
            for (FuncionalidadesBanio f : FuncionalidadesBanio.values()) {
                if (nombre(f).equals(buscado)) { set.add(f); encontrada = true; }
            }
            if (!encontrada) throw new IllegalArgumentException("Funcionalidad desconocida: " + s);
        }
        return FuncionalidadesBanio.toMask(set);
    }
}
