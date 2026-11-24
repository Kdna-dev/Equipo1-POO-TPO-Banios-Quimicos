package Controlador.Http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Sirve el frontend (HTML/CSS/JS de la carpeta web/). */
public class ArchivosEstaticos {

    private final Path raizWeb;

    public ArchivosEstaticos(Path raizWeb) {
        this.raizWeb = raizWeb.toAbsolutePath().normalize();
    }

    public void manejar(HttpExchange ex) throws IOException {
        String ruta = ex.getRequestURI().getPath();
        if (ruta.equals("/")) ruta = "/index.html";
        Path archivo = raizWeb.resolve(ruta.substring(1)).normalize();
        if (!archivo.startsWith(raizWeb) || !Files.isRegularFile(archivo)) {
            byte[] msg = "No encontrado".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, msg.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(msg); }
            return;
        }
        byte[] bytes = Files.readAllBytes(archivo);
        ex.getResponseHeaders().set("Content-Type", tipoContenido(archivo.getFileName().toString()));
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String tipoContenido(String nombre) {
        if (nombre.endsWith(".html")) return "text/html; charset=utf-8";
        if (nombre.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (nombre.endsWith(".css")) return "text/css; charset=utf-8";
        if (nombre.endsWith(".json") || nombre.endsWith(".webmanifest")) return "application/json; charset=utf-8";
        if (nombre.endsWith(".svg")) return "image/svg+xml";
        if (nombre.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
