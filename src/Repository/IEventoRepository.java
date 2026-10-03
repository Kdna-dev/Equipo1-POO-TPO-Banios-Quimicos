package Repository;

import Dominio.Evento;
import java.util.List;

public interface IEventoRepository {
    void guardar(Evento evento);
    void actualizar(Evento evento);
    void eliminar(int id);
    Evento buscar(int id);
    List<Evento> obtenerTodos();
}