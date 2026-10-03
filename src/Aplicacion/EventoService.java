package Aplicacion;

import Dominio.AsignacionBanio;
import Dominio.Banio;
import Dominio.Evento;
import Dominio.Ubicacion;
import Repository.IAsignacionBanioRepository;
import Repository.IBanioRepository;
import Repository.IEventoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventoService {

    private final IEventoRepository eventoRepository;
    private final IAsignacionBanioRepository asignacionRepository;
    private final IBanioRepository banioRepository;

    public EventoService(IEventoRepository eventoRepository,
                         IAsignacionBanioRepository asignacionRepository,
                         IBanioRepository banioRepository) {
        this.eventoRepository = eventoRepository;
        this.asignacionRepository = asignacionRepository;
        this.banioRepository = banioRepository;
    }

    public Evento crearEvento(Evento evento) {
        if (evento == null) {
            throw new IllegalArgumentException("El evento no puede ser nulo.");
        }
        eventoRepository.guardar(evento);
        return evento;
    }

    public void actualizarEvento(Evento evento) {
        if (evento == null || evento.getId() <= 0) {
            throw new IllegalArgumentException("Evento inválido para actualizar.");
        }
        eventoRepository.actualizar(evento);
    }

    public void eliminarEvento(int id) {
        eventoRepository.eliminar(id);
    }

    public Evento buscarPorId(int id) {
        return eventoRepository.buscar(id);
    }

    public List<Evento> obtenerTodos() {
        return eventoRepository.obtenerTodos();
    }

    // Gestión de Asignación Temporal de Baños a Eventos (Sección 5 de Arquitectura)
    public AsignacionBanio asignarBanio(int eventoId, int banioId, LocalDateTime fechaInicio, LocalDateTime fechaFin, Ubicacion ubicacion) {
        Evento evento = eventoRepository.buscar(eventoId);
        if (evento == null) {
            throw new IllegalArgumentException("No se encontró el evento con ID " + eventoId);
        }

        Banio banio = banioRepository.buscar(banioId);
        if (banio == null) {
            throw new IllegalArgumentException("No se encontró el baño con ID " + banioId);
        }

        if (ubicacion == null) {
            ubicacion = banio.getUbicacion();
        }

        AsignacionBanio asignacion = new AsignacionBanio(0, banio, evento, fechaInicio, fechaFin, ubicacion);
        asignacionRepository.guardar(asignacion);
        evento.addBanio(banio);

        return asignacion;
    }

    public void desasignarBanio(int asignacionId) {
        asignacionRepository.eliminar(asignacionId);
    }

    public List<AsignacionBanio> obtenerAsignaciones(int eventoId) {
        return asignacionRepository.obtenerPorEvento(eventoId);
    }

    public List<Banio> obtenerBaniosDeEvento(int eventoId) {
        List<AsignacionBanio> asignaciones = asignacionRepository.obtenerPorEvento(eventoId);
        List<Banio> banios = new ArrayList<>();
        for (AsignacionBanio asig : asignaciones) {
            if (asig.getBanio() != null) {
                banios.add(asig.getBanio());
            }
        }
        return banios;
    }
}
