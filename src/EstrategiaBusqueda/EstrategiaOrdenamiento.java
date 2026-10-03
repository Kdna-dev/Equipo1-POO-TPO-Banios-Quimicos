package EstrategiaBusqueda;

import Dominio.Banio;
import Dominio.Ubicacion;

import java.util.List;

public interface EstrategiaOrdenamiento {
    List<Banio> ordenar(List<Banio> banios, Ubicacion origen);
}