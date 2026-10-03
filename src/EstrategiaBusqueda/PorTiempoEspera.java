package EstrategiaBusqueda;

import Dominio.Banio;
import Dominio.Ubicacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PorTiempoEspera implements EstrategiaOrdenamiento {

    @Override
    public List<Banio> ordenar(List<Banio> banios, Ubicacion origen) {
        if (banios == null) {
            return new ArrayList<>();
        }

        List<Banio> resultado = new ArrayList<>(banios);
        resultado.sort(Comparator.comparingDouble(Banio::getTiempoEstimadoEspera));
        return resultado;
    }
}