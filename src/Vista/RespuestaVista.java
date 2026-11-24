package Vista;

import java.util.Collections;
import java.util.Map;

/** Respuestas genéricas de la API. */
public final class RespuestaVista {
    private RespuestaVista() { }

    public static Map<String, Object> ok() { return Collections.singletonMap("ok", true); }

    public static Map<String, Object> error(String mensaje) { return Collections.singletonMap("error", mensaje); }

    public static Map<String, Object> campo(String clave, Object valor) { return Collections.singletonMap(clave, valor); }
}
