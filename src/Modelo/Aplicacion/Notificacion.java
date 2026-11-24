package Modelo.Aplicacion;

import java.time.Instant;

/** Aviso para una persona (por ejemplo, "es tu turno"). */
public record Notificacion(long id, String mensaje, Instant instante) { }
