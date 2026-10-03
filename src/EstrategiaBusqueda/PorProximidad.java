package EstrategiaBusqueda;

import Dominio.Banio;
import Dominio.CalculadorDistancia;
import Dominio.CalculadorDistanciaHaversine;
import Dominio.Ubicacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PorProximidad implements EstrategiaOrdenamiento {

    private final CalculadorDistancia calculadorDistancia;

    public PorProximidad(CalculadorDistancia calculadorDistancia) {
        this.calculadorDistancia = calculadorDistancia;
    }

    public PorProximidad() {
        this(new CalculadorDistanciaHaversine());
    }

    @Override
    public List<Banio> ordenar(List<Banio> banios, Ubicacion origen) {
        if (banios == null || origen == null) {
            return (banios != null) ? new ArrayList<>(banios) : new ArrayList<>();
        }

        List<Banio> resultado = new ArrayList<>(banios);
        resultado.sort(Comparator.comparingDouble(b -> b.calcularDistanciaA(origen, calculadorDistancia)));
        return resultado;
    }
}
