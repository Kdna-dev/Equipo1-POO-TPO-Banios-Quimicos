package Controlador.Http;

import Modelo.Aplicacion.RecursoNoEncontradoException;
import Vista.Json;
import Vista.RespuestaVista;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Front controller de la app: recibe cada request de /api/, la pasa al primer controlador
 * que la atienda, traduce las excepciones a códigos HTTP y serializa la vista a JSON.
 * El resto de las rutas se sirven como archivos estáticos del frontend.
 */
public class ServidorHttp {

    private final List<Controlador> controladores;
    private final ArchivosEstaticos estaticos;
    private final Object candado;

    /**
     * @param candado objeto sobre el que se serializan las requests de la API
     *                (el modelo guarda estado en memoria y no es thread-safe).
     */
    public ServidorHttp(List<Controlador> controladores, ArchivosEstaticos estaticos, Object candado) {
        this.controladores = controladores;
        this.estaticos = estaticos;
        this.candado = candado;
    }

    public HttpServer iniciar(int puerto) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.createContext("/api/", this::manejarApi);
        server.createContext("/", estaticos::manejar);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        return server;
    }

    private void manejarApi(HttpExchange ex) throws IOException {
        int status = 200;
        Object cuerpo;
        try {
            ex.getRequestBody().readAllBytes();
            Peticion p = new Peticion(ex.getRequestMethod(), ex.getRequestURI().getPath(), ex.getRequestURI().getRawQuery());
            synchronized (candado) {
                cuerpo = despachar(p);
            }
        } catch (RutaNoEncontradaException | RecursoNoEncontradoException e) {
            status = 404; cuerpo = RespuestaVista.error(e.getMessage());
        } catch (IllegalStateException e) {
            status = 409; cuerpo = RespuestaVista.error(e.getMessage());
        } catch (IllegalArgumentException e) {
            status = 400; cuerpo = RespuestaVista.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            status = 500; cuerpo = RespuestaVista.error("Error interno");
        }
        byte[] bytes = Json.escribir(cuerpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private Object despachar(Peticion p) {
        for (Controlador c : controladores) {
            Object r = c.atender(p);
            if (r != null) return r;
        }
        throw new RutaNoEncontradaException("Ruta desconocida: " + p.metodo() + " " + p.ruta());
    }
}
