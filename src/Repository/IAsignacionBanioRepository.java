package Repository;

import Dominio.AsignacionBanio;
import java.util.List;

public interface IAsignacionBanioRepository {
    void guardar(AsignacionBanio asignacion);
    void actualizar(AsignacionBanio asignacion);
    void eliminar(int id);
    AsignacionBanio buscar(int id);
    List<AsignacionBanio> obtenerPorEvento(int eventoId);
    List<AsignacionBanio> obtenerTodos();
}
