package Dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Evento {

    private int id;
    private LocalDate fecha;
    private String nombre;
    private String descripcion;
    private LocalDateTime inicio;
    private LocalDateTime fin;

    private List<Banio> banios;

    public Evento(int id, LocalDate fecha, String nombre, String descripcion, LocalDateTime inicio, LocalDateTime fin) {
        this.id = id;
        this.fecha = fecha;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.inicio = inicio;
        this.fin = fin;
        this.banios = new ArrayList<>();
    }

    public Evento(int id, LocalDate fecha, String nombre, String descripcion) {
        this(id, fecha, nombre, descripcion, null, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }

    public List<Banio> getBanios() {
        return banios;
    }

    public void setBanios(List<Banio> banios) {
        this.banios = (banios != null) ? banios : new ArrayList<>();
    }

    public void addBanio(Banio b) {
        if (this.banios == null) {
            this.banios = new ArrayList<>();
        }
        this.banios.add(b);
    }
}