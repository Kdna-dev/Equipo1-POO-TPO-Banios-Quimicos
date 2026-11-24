package Modelo.Dominio.BanioState;

import Modelo.Dominio.Banio;
import Modelo.Dominio.TipoEstadoBanio;

public class EstadoOcupado implements EstadoBanio {

    @Override
    public TipoEstadoBanio getTipo() {
        return TipoEstadoBanio.OCUPADO;
    }

    @Override
    public boolean permiteIngreso() {
        return true;
    }

    @Override
    public boolean esVisible() {
        return true;
    }

    @Override
    public boolean contabilizaFuncionalidades() {
        return true;
    }

    @Override
    public void salir(Banio banio) {
        banio.setEstado(new EstadoLibre());
    }

    @Override
    public void saturar(Banio banio) {
        banio.setEstado(new EstadoSaturado());
    }
}
