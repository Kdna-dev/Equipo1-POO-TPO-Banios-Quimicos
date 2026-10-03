package Dominio.BanioState;

import Dominio.Banio;
import Dominio.Persona;
import Dominio.TipoEstadoBanio;

public interface EstadoBanio {
    TipoEstadoBanio getTipo();
    boolean permiteIngreso();
    boolean esVisible();
    boolean contabilizaFuncionalidades();

    default void ingresarACola(Banio banio, Persona persona) {
        if (!permiteIngreso()) {
            throw new IllegalStateException("El baño en estado " + getTipo() + " no permite ingresar a la cola.");
        }
        banio.getColaVirtual().agregarPersona(persona);
        if (banio.getCapacidad() > 0 && banio.getCantidadEnEspera() >= banio.getCapacidad()) {
            banio.saturar();
        }
    }

    default void entrar(Banio banio) {
        throw new IllegalStateException("No se puede ingresar al baño en estado " + getTipo());
    }

    default void salir(Banio banio) {
        throw new IllegalStateException("No se puede registrar salida en estado " + getTipo());
    }

    default void saturar(Banio banio) {
        throw new IllegalStateException("No se puede saturar el baño en estado " + getTipo());
    }

    default void desaturar(Banio banio) {
        throw new IllegalStateException("No se puede desaturar el baño en estado " + getTipo());
    }

    default void iniciarMantenimiento(Banio banio) {
        throw new IllegalStateException("No se puede iniciar mantenimiento en estado " + getTipo());
    }

    default void finalizarMantenimiento(Banio banio) {
        throw new IllegalStateException("No se puede finalizar mantenimiento en estado " + getTipo());
    }

    default void descomponer(Banio banio) {
        banio.setEstado(new EstadoFueraDeServicio());
    }
}
