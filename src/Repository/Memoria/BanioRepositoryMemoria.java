package Repository.Memoria;

import Dominio.Banio;
import Repository.IBanioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Repositorio en memoria: permite correr el sistema sin Supabase (demo / desarrollo). */
public class BanioRepositoryMemoria implements IBanioRepository {

    private final Map<Integer, Banio> datos = new ConcurrentHashMap<>();
    private final AtomicInteger secuencia = new AtomicInteger(0);

    @Override
    public void guardar(Banio banio) {
        if (banio.getId() <= 0) {
            banio.setId(secuencia.incrementAndGet());
        } else {
            secuencia.accumulateAndGet(banio.getId(), Math::max);
        }
        datos.put(banio.getId(), banio);
    }

    @Override
    public void actualizar(Banio banio) {
        datos.put(banio.getId(), banio);
    }

    @Override
    public void eliminar(int id) {
        datos.remove(id);
    }

    @Override
    public Banio buscar(int id) {
        return datos.get(id);
    }

    @Override
    public List<Banio> obtenerTodos() {
        List<Banio> lista = new ArrayList<>(datos.values());
        lista.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        return lista;
    }

    @Override
    public List<Banio> buscarPorFuncionalidades(int mascara) {
        List<Banio> r = new ArrayList<>();
        for (Banio b : obtenerTodos()) {
            if ((b.getFuncionalidades() & mascara) != 0) r.add(b);
        }
        return r;
    }
}
