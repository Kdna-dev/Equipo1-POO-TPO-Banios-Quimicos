package Dominio;

import Dominio.BanioState.EstadoBanio;
import Dominio.BanioState.EstadoLibre;

public class Banio {

    private int id;
    private String nombre;
    private Ubicacion ubicacion;
    private EstadoBanio estado;
    private int capacidad;
    private int funcionalidades;
    private final ColaVirtual colaVirtual;
    private double tiempoPromedioAtencionMinutos;
    private boolean tieneOcupante;

    public Banio(int id, String nombre, Ubicacion ubicacion, int capacidad, int funcionalidades) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.capacidad = capacidad;
        this.funcionalidades = funcionalidades;
        this.estado = new EstadoLibre();
        this.colaVirtual = new ColaVirtual();
        this.tiempoPromedioAtencionMinutos = 3.0;
        this.tieneOcupante = false;
    }

    public Banio(int id, String nombre, Ubicacion ubicacion) {
        this(id, nombre, ubicacion, 10, 0);
    }

    public Banio(int id, String nombre) {
        this(id, nombre, null, 10, 0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(Ubicacion ubicacion) {
        this.ubicacion = ubicacion;
    }

    public EstadoBanio getEstado() {
        return estado;
    }

    public void setEstado(EstadoBanio estado) {
        this.estado = estado;
    }

    public TipoEstadoBanio getTipoEstado() {
        return estado.getTipo();
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public int getFuncionalidades() {
        return funcionalidades;
    }

    public void setFuncionalidades(int funcionalidades) {
        this.funcionalidades = funcionalidades;
    }

    public ColaVirtual getColaVirtual() {
        return colaVirtual;
    }

    public double getTiempoPromedioAtencionMinutos() {
        return tiempoPromedioAtencionMinutos;
    }

    public void setTiempoPromedioAtencionMinutos(double tiempoPromedioAtencionMinutos) {
        this.tiempoPromedioAtencionMinutos = tiempoPromedioAtencionMinutos;
    }

    public boolean tieneOcupante() {
        return tieneOcupante;
    }

    public void setTieneOcupante(boolean tieneOcupante) {
        this.tieneOcupante = tieneOcupante;
    }

    public int getCantidadEnEspera() {
        return colaVirtual.getCantidadPersonas();
    }

    public double getTiempoEstimadoEspera() {
        int personas = getCantidadEnEspera();
        int ocupanteActual = tieneOcupante ? 1 : 0;
        return (personas + ocupanteActual) * tiempoPromedioAtencionMinutos;
    }

    public double calcularDistanciaA(Ubicacion origen, CalculadorDistancia calculador) {
        if (calculador == null) {
            calculador = new CalculadorDistanciaHaversine();
        }
        return calculador.calcularDistancia(this.ubicacion, origen);
    }

    // Delegaciones en el patrón State
    public boolean permiteIngreso() {
        return estado.permiteIngreso();
    }

    public boolean esVisible() {
        return estado.esVisible();
    }

    public boolean contabilizaFuncionalidades() {
        return estado.contabilizaFuncionalidades();
    }

    public void ingresarACola(Persona persona) {
        estado.ingresarACola(this, persona);
    }

    public void entrar() {
        this.tieneOcupante = true;
        estado.entrar(this);
    }

    public void salir() {
        this.tieneOcupante = false;
        estado.salir(this);
    }

    public void saturar() {
        estado.saturar(this);
    }

    public void desaturar() {
        estado.desaturar(this);
    }

    public void iniciarMantenimiento() {
        estado.iniciarMantenimiento(this);
    }

    public void finalizarMantenimiento() {
        estado.finalizarMantenimiento(this);
    }

    public void descomponer() {
        estado.descomponer(this);
    }

    public boolean retirarDeCola(Persona persona) {
        boolean removido = colaVirtual.retirarPersona(persona);
        if (removido && getTipoEstado() == TipoEstadoBanio.SATURADO && getCantidadEnEspera() < capacidad) {
            desaturar();
        }
        return removido;
    }

    public Persona atenderSiguiente() {
        Persona siguiente = colaVirtual.atenderSiguiente();
        if (siguiente != null) {
            this.tieneOcupante = true;
            if (getTipoEstado() == TipoEstadoBanio.SATURADO && getCantidadEnEspera() < capacidad) {
                desaturar();
            } else if (getTipoEstado() == TipoEstadoBanio.LIBRE) {
                entrar();
            }
        } else {
            salir();
        }
        return siguiente;
    }
}