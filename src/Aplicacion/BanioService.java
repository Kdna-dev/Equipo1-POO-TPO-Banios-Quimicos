package Aplicacion;

import Dominio.*;
import Dominio.BanioState.EstadoBanio;
import EstrategiaBusqueda.EstrategiaOrdenamiento;
import EstrategiaBusqueda.PorProximidad;
import Repository.IBanioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BanioService {

    private final IBanioRepository banioRepository;
    private final ColaService colaService;
    // Registro en memoria de baños activos para preservar sus colas virtuales y observadores
    private final Map<Integer, Banio> baniosActivos = new ConcurrentHashMap<>();

    public BanioService(IBanioRepository banioRepository, ColaService colaService) {
        this.banioRepository = banioRepository;
        this.colaService = colaService;
    }

    public BanioService(IBanioRepository banioRepository) {
        this(banioRepository, new ColaService());
    }

    public ColaService getColaService() {
        return colaService;
    }

    // Operaciones Administrativas
    public Banio crearBanio(Banio banio) {
        if (banio == null) {
            throw new IllegalArgumentException("El baño no puede ser nulo.");
        }
        banioRepository.guardar(banio);
        baniosActivos.put(banio.getId(), banio);
        return banio;
    }

    public void actualizarBanio(Banio banio) {
        if (banio == null || banio.getId() <= 0) {
            throw new IllegalArgumentException("Baño inválido para actualizar.");
        }
        banioRepository.actualizar(banio);
        baniosActivos.put(banio.getId(), banio);
    }

    public void eliminarBanio(int id) {
        banioRepository.eliminar(id);
        baniosActivos.remove(id);
    }

    public Banio buscarPorId(int id) {
        if (baniosActivos.containsKey(id)) {
            return baniosActivos.get(id);
        }
        Banio banio = banioRepository.buscar(id);
        if (banio != null) {
            baniosActivos.put(id, banio);
        }
        return banio;
    }

    public List<Banio> obtenerTodos() {
        List<Banio> desdeBd = banioRepository.obtenerTodos();
        List<Banio> resultado = new ArrayList<>();
        for (Banio b : desdeBd) {
            if (baniosActivos.containsKey(b.getId())) {
                resultado.add(baniosActivos.get(b.getId()));
            } else {
                baniosActivos.put(b.getId(), b);
                resultado.add(b);
            }
        }
        return resultado;
    }

    public void cambiarEstado(int banioId, EstadoBanio nuevoEstado) {
        Banio banio = buscarPorId(banioId);
        if (banio == null) {
            throw new IllegalArgumentException("No se encontró el baño con ID " + banioId);
        }
        banio.setEstado(nuevoEstado);
        banioRepository.actualizar(banio);
    }

    public void descomponerBanio(int banioId) {
        Banio banio = buscarPorId(banioId);
        if (banio != null) {
            banio.descomponer();
            banioRepository.actualizar(banio);
        }
    }

    public void iniciarMantenimiento(int banioId) {
        Banio banio = buscarPorId(banioId);
        if (banio != null) {
            banio.iniciarMantenimiento();
            banioRepository.actualizar(banio);
        }
    }

    public void finalizarMantenimiento(int banioId) {
        Banio banio = buscarPorId(banioId);
        if (banio != null) {
            banio.finalizarMantenimiento();
            banioRepository.actualizar(banio);
        }
    }

    // Operaciones de Consulta y Ordenamiento con Strategy
    public List<Banio> consultarBanios(Ubicacion origen, EstrategiaOrdenamiento estrategia, Integer mascaraFuncionalidades, boolean soloVisibles) {
        List<Banio> todos = obtenerTodos();
        List<Banio> filtrados = new ArrayList<>();

        for (Banio b : todos) {
            if (soloVisibles && !b.esVisible()) {
                continue;
            }
            if (mascaraFuncionalidades != null && mascaraFuncionalidades > 0) {
                if ((b.getFuncionalidades() & mascaraFuncionalidades) != mascaraFuncionalidades) {
                    continue;
                }
            }
            filtrados.add(b);
        }

        if (estrategia == null) {
            estrategia = new PorProximidad();
        }

        return estrategia.ordenar(filtrados, origen);
    }

    public List<Banio> consultarBanios(Ubicacion origen, EstrategiaOrdenamiento estrategia) {
        return consultarBanios(origen, estrategia, null, true);
    }

    // Operaciones sobre la Cola Virtual delegadas en ColaService
    public void ingresarACola(int banioId, Persona persona) {
        Banio banio = buscarPorId(banioId);
        if (banio == null) {
            throw new IllegalArgumentException("No existe el baño con ID " + banioId);
        }
        colaService.ingresarACola(banio, persona);
        banioRepository.actualizar(banio);
    }

    public boolean salirDeCola(int banioId, Persona persona) {
        Banio banio = buscarPorId(banioId);
        if (banio == null) {
            return false;
        }
        boolean exito = colaService.salirDeCola(banio, persona);
        if (exito) {
            banioRepository.actualizar(banio);
        }
        return exito;
    }

    public Persona atenderSiguiente(int banioId) {
        Banio banio = buscarPorId(banioId);
        if (banio == null) {
            return null;
        }
        Persona atendida = colaService.atenderSiguiente(banio);
        banioRepository.actualizar(banio);
        return atendida;
    }

    public void suscribirObservador(int banioId, ColaVirtualObserver observer) {
        Banio banio = buscarPorId(banioId);
        if (banio != null) {
            banio.getColaVirtual().agregarObserver(observer);
        }
    }
}
