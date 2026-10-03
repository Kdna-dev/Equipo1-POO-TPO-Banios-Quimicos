package EstrategiaBusqueda;

import Dominio.Banio;
import Dominio.CalculadorDistancia;
import Dominio.CalculadorDistanciaHaversine;
import Dominio.Ubicacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MejorOpcion implements EstrategiaOrdenamiento {

    private final CalculadorDistancia calculadorDistancia;
    // Velocidad estimada de caminata: 80 metros por minuto (~4.8 km/h)
    private static final double METROS_POR_MINUTO_CAMINATA = 80.0;

    public MejorOpcion(CalculadorDistancia calculadorDistancia) {
        this.calculadorDistancia = calculadorDistancia;
    }

    public MejorOpcion() {
        this(new CalculadorDistanciaHaversine());
    }

    @Override
    public List<Banio> ordenar(List<Banio> banios, Ubicacion origen) {
        if (banios == null) {
            return new ArrayList<>();
        }

        List<Banio> resultado = new ArrayList<>(banios);
        resultado.sort(Comparator.comparingDouble(b -> calcularCostoTotal(b, origen)));
        return resultado;
    }

    private double calcularCostoTotal(Banio banio, Ubicacion origen) {
        double tiempoEspera = banio.getTiempoEstimadoEspera();
        if (origen == null || banio.getUbicacion() == null) {
            return tiempoEspera;
        }
        double distanciaMetros = banio.calcularDistanciaA(origen, calculadorDistancia);
        double tiempoCaminata = distanciaMetros / METROS_POR_MINUTO_CAMINATA;
        return tiempoEspera + tiempoCaminata;
    }
}
