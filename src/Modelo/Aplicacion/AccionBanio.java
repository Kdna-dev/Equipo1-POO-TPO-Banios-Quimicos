package Modelo.Aplicacion;

/** Acciones administrativas sobre el estado operativo de un baño. */
public enum AccionBanio {
    DESCOMPONER,
    INICIAR_MANTENIMIENTO,
    FINALIZAR_MANTENIMIENTO,
    REPARAR;

    /** Convierte el código recibido (sensible a mayúsculas) en una acción. */
    public static AccionBanio desde(String codigo) {
        for (AccionBanio a : values()) if (a.name().equals(codigo)) return a;
        throw new IllegalArgumentException("Acción desconocida: " + codigo);
    }
}
