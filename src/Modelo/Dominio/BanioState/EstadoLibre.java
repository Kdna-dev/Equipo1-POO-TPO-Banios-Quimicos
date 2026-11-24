package Modelo.Dominio.BanioState;

import Modelo.Dominio.Banio;
import Modelo.Dominio.TipoEstadoBanio;

public class EstadoLibre implements EstadoBanio {

    @Override
    public TipoEstadoBanio getTipo() {
        return TipoEstadoBanio.LIBRE;
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
    public void entrar(Banio banio) {
        banio.setEstado(new EstadoOcupado());
    }

    @Override
    public void saturar(Banio banio) {
        banio.setEstado(new EstadoSaturado());
    }
}
