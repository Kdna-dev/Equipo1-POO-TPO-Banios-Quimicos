package Modelo.Repository;

import Modelo.Dominio.Banio;
import java.util.List;

public interface IBanioRepository {
    void guardar(Banio banio);
    void actualizar(Banio banio);
    void eliminar(int id);
    Banio buscar(int id);
    List<Banio> obtenerTodos();
    List<Banio> buscarPorFuncionalidades(int mascara);
}
