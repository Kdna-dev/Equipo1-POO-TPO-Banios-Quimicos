package Modelo.EstrategiaBusqueda;

import Modelo.Dominio.Banio;
import Modelo.Dominio.Ubicacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PorCantidadEspera implements EstrategiaOrdenamiento {

    @Override
    public List<Banio> ordenar(List<Banio> banios, Ubicacion origen) {
        if (banios == null) {
            return new ArrayList<>();
        }

        List<Banio> resultado = new ArrayList<>(banios);
        resultado.sort(Comparator.comparingInt(Banio::getCantidadEnEspera));
        return resultado;
    }
}