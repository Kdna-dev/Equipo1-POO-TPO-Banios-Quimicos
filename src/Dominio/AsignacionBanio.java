package Dominio;

import java.time.LocalDateTime;

public class AsignacionBanio {

    private int id;
    private Banio banio;
    private Evento evento;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaDesasignacion;
    private Ubicacion ubicacion;

    public AsignacionBanio(int id, Banio banio, Evento evento, LocalDateTime fechaAsignacion, LocalDateTime fechaDesasignacion, Ubicacion ubicacion) {
        this.id = id;
        this.banio = banio;
        this.evento = evento;
        this.fechaAsignacion = fechaAsignacion;
        this.fechaDesasignacion = fechaDesasignacion;
        this.ubicacion = ubicacion;
    }

    public AsignacionBanio(Banio banio, Evento evento, LocalDateTime fechaAsignacion, Ubicacion ubicacion) {
        this(0, banio, evento, fechaAsignacion, null, ubicacion);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Banio getBanio() {
        return banio;
    }

    public void setBanio(Banio banio) {
        this.banio = banio;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public LocalDateTime getFechaDesasignacion() {
        return fechaDesasignacion;
    }

    public void setFechaDesasignacion(LocalDateTime fechaDesasignacion) {
        this.fechaDesasignacion = fechaDesasignacion;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(Ubicacion ubicacion) {
        this.ubicacion = ubicacion;
    }
}