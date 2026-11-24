package Modelo.Dominio.BanioState;

import Modelo.Dominio.Banio;
import Modelo.Dominio.TipoEstadoBanio;

public class EstadoFueraDeServicio implements EstadoBanio {

    @Override
    public TipoEstadoBanio getTipo() {
        return TipoEstadoBanio.FUERA_SERVICIO;
    }

    @Override
    public boolean permiteIngreso() {
        return false;
    }

    @Override
    public boolean esVisible() {
        return false;
    }

    @Override
    public boolean contabilizaFuncionalidades() {
        return false;
    }

    @Override
    public void iniciarMantenimiento(Banio banio) {
        banio.setEstado(new EstadoMantenimiento());
    }

    @Override
    public void descomponer(Banio banio) {
        // Ya se encuentra fuera de servicio
    }
}
