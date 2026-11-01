package Repository.Memoria;

import Dominio.Persona;
import Repository.IPersonaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Repositorio en memoria: permite correr el sistema sin Supabase (demo / desarrollo). */
public class PersonaRepositoryMemoria implements IPersonaRepository {

    private final Map<Integer, Persona> datos = new ConcurrentHashMap<>();
    private final AtomicInteger secuencia = new AtomicInteger(0);

    @Override
    public void guardar(Persona persona) {
        if (persona.getId() <= 0) {
            persona.setId(secuencia.incrementAndGet());
        } else {
            secuencia.accumulateAndGet(persona.getId(), Math::max);
        }
        datos.put(persona.getId(), persona);
    }

    @Override
    public void actualizar(Persona persona) {
        datos.put(persona.getId(), persona);
    }

    @Override
    public void eliminar(int id) {
        datos.remove(id);
    }

    @Override
    public void eliminar(Persona persona) {
        if (persona != null) datos.remove(persona.getId());
    }

    @Override
    public Persona buscar(int id) {
        return datos.get(id);
    }

    @Override
    public List<Persona> obtenerTodos() {
        return new ArrayList<>(datos.values());
    }
}
