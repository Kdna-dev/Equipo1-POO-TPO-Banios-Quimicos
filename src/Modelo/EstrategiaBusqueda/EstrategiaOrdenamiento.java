package Modelo.EstrategiaBusqueda;

import Modelo.Dominio.Banio;
import Modelo.Dominio.Ubicacion;

import java.util.List;

public interface EstrategiaOrdenamiento {
    List<Banio> ordenar(List<Banio> banios, Ubicacion origen);
}