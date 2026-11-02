package Web;


import java.util.Map;

/** Serializador JSON mínimo (el MVP no usa librerías externas). */
public final class Json {
    private Json() { }

    public static String escribir(Object o) {
        StringBuilder sb = new StringBuilder();
        escribir(sb, o);
        return sb.toString();
    }

    private static void escribir(StringBuilder sb, Object o) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof Boolean) {
            sb.append(o);
        } else if (o instanceof Number) {
            double d = ((Number) o).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) sb.append("null"); else sb.append(o);
        } else if (o instanceof Map) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!primero) sb.append(',');
                primero = false;
                texto(sb, String.valueOf(e.getKey()));
                sb.append(':');
                escribir(sb, e.getValue());
            }
            sb.append('}');
        } else if (o instanceof Iterable) {
            sb.append('[');
            boolean primero = true;
            for (Object x : (Iterable<?>) o) {
                if (!primero) sb.append(',');
                primero = false;
                escribir(sb, x);
            }
            sb.append(']');
        } else {
            texto(sb, String.valueOf(o));   // String, enums, fechas
        }
    }

    private static void texto(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
            }
        }
        sb.append('"');
    }
}
