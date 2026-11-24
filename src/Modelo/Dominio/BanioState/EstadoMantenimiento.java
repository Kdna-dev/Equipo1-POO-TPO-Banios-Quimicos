package Modelo.Dominio.BanioState;

import Modelo.Dominio.Banio;
import Modelo.Dominio.TipoEstadoBanio;

public class EstadoMantenimiento implements EstadoBanio {

    @Override
    public TipoEstadoBanio getTipo() {
        return TipoEstadoBanio.MANTENIMIENTO;
    }

    @Override
    public boolean permiteIngreso() {
        return false;
    }

    @Override
    public boolean esVisible() {
        return true;
    }

    @Override
    public boolean contabilizaFuncionalidades() {
        return false;
    }

    @Override
    public void finalizarMantenimiento(Banio banio) {
        banio.setEstado(new EstadoLibre());
    }
}
