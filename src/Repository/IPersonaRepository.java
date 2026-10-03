package Repository;

import Dominio.Persona;
import java.util.List;

public interface IPersonaRepository {
    void guardar(Persona persona);
    void actualizar(Persona persona);
    void eliminar(int id);
    void eliminar(Persona persona);
    Persona buscar(int id);
    List<Persona> obtenerTodos();
}
