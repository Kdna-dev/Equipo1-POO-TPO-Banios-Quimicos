package Dominio;

import java.util.EnumSet;

public enum FuncionalidadesBanio {
    ACCESIBILIDAD_MOVILIDAD_REDUCIDA(1),
    LAVAMANOS(2),
    DISPENSER_ALCOHOL(4),
    CAMBIADOR_BEBES(8),
    ESPEJO(16);

    private final int flag;

    FuncionalidadesBanio(int flag) {
        this.flag = flag;
    }

    public static EnumSet<FuncionalidadesBanio> fromMask(int mask) {
        EnumSet<FuncionalidadesBanio> result = EnumSet.noneOf(FuncionalidadesBanio.class);

        for (FuncionalidadesBanio caracteristica : values()) {
            if ((mask & caracteristica.flag) != 0) {
                result.add(caracteristica);
            }
        }

        return result;
    }

    public static int toMask(EnumSet<FuncionalidadesBanio> caracteristicas) {
        int result = 0;

        for (FuncionalidadesBanio caracteristica : caracteristicas) {
            result |= caracteristica.flag;
        }

        return result;
    }
}
