package Dominio.BanioState;

import Dominio.Banio;
import Dominio.TipoEstadoBanio;

public class EstadoSaturado implements EstadoBanio {

    @Override
    public TipoEstadoBanio getTipo() {
        return TipoEstadoBanio.SATURADO;
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
        return true;
    }

    @Override
    public void desaturar(Banio banio) {
        if (banio.tieneOcupante()) {
            banio.setEstado(new EstadoOcupado());
        } else {
            banio.setEstado(new EstadoLibre());
        }
    }
}
