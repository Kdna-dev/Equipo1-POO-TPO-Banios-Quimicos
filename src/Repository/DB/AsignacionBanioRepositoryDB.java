package Repository.DB;

import Dominio.*;
import Repository.IAsignacionBanioRepository;
import Repository.IBanioRepository;
import Repository.IEventoRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AsignacionBanioRepositoryDB implements IAsignacionBanioRepository {

    private final Connection connection;
    private final IBanioRepository banioRepository;
    private final IEventoRepository eventoRepository;

    public AsignacionBanioRepositoryDB(Connection connection, IBanioRepository banioRepository, IEventoRepository eventoRepository) {
        this.connection = connection;
        this.banioRepository = banioRepository;
        this.eventoRepository = eventoRepository;
    }

    public AsignacionBanioRepositoryDB(Connection connection) {
        this(connection, new BanioRepositoryDB(connection), new EventoRepositoryDB(connection));
    }

    @Override
    public void guardar(AsignacionBanio asignacion) {
        boolean tieneId = asignacion.getId() > 0;
        String sql = tieneId
                ? """
                  INSERT INTO asignacion_banio
                  (id, banio_id, evento_id, fecha_asignacion, fecha_desasignacion, latitud, longitud, nombre_ubicacion)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                  """
                : """
                  INSERT INTO asignacion_banio
                  (banio_id, evento_id, fecha_asignacion, fecha_desasignacion, latitud, longitud, nombre_ubicacion)
                  VALUES (?, ?, ?, ?, ?, ?, ?)
                  """;

        try (PreparedStatement stmt = tieneId
                ? connection.prepareStatement(sql)
                : connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int idx = 1;
            if (tieneId) {
                stmt.setInt(idx++, asignacion.getId());
            }

            stmt.setInt(idx++, asignacion.getBanio() != null ? asignacion.getBanio().getId() : 0);
            stmt.setInt(idx++, asignacion.getEvento() != null ? asignacion.getEvento().getId() : 0);
            stmt.setTimestamp(idx++, asignacion.getFechaAsignacion() != null ? Timestamp.valueOf(asignacion.getFechaAsignacion()) : null);
            stmt.setTimestamp(idx++, asignacion.getFechaDesasignacion() != null ? Timestamp.valueOf(asignacion.getFechaDesasignacion()) : null);

            Ubicacion u = asignacion.getUbicacion();
            if (u != null) {
                stmt.setDouble(idx++, u.getLatitud());
                stmt.setDouble(idx++, u.getLongitud());
                stmt.setString(idx++, u.getNombre());
            } else {
                stmt.setNull(idx++, Types.DOUBLE);
                stmt.setNull(idx++, Types.DOUBLE);
                stmt.setNull(idx++, Types.VARCHAR);
            }

            stmt.executeUpdate();

            if (!tieneId) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        asignacion.setId(rs.getInt(1));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar asignación de baño", e);
        }
    }

    @Override
    public void actualizar(AsignacionBanio asignacion) {
        String sql = """
                UPDATE asignacion_banio
                SET banio_id = ?,
                    evento_id = ?,
                    fecha_asignacion = ?,
                    fecha_desasignacion = ?,
                    latitud = ?,
                    longitud = ?,
                    nombre_ubicacion = ?
                WHERE id = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, asignacion.getBanio() != null ? asignacion.getBanio().getId() : 0);
            stmt.setInt(2, asignacion.getEvento() != null ? asignacion.getEvento().getId() : 0);
            stmt.setTimestamp(3, asignacion.getFechaAsignacion() != null ? Timestamp.valueOf(asignacion.getFechaAsignacion()) : null);
            stmt.setTimestamp(4, asignacion.getFechaDesasignacion() != null ? Timestamp.valueOf(asignacion.getFechaDesasignacion()) : null);

            Ubicacion u = asignacion.getUbicacion();
            if (u != null) {
                stmt.setDouble(5, u.getLatitud());
                stmt.setDouble(6, u.getLongitud());
                stmt.setString(7, u.getNombre());
            } else {
                stmt.setNull(5, Types.DOUBLE);
                stmt.setNull(6, Types.DOUBLE);
                stmt.setNull(7, Types.VARCHAR);
            }
            stmt.setInt(8, asignacion.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar asignación de baño", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM asignacion_banio WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar asignación de baño", e);
        }
    }

    @Override
    public AsignacionBanio buscar(int id) {
        String sql = """
                SELECT id, banio_id, evento_id, fecha_asignacion, fecha_desasignacion, latitud, longitud, nombre_ubicacion
                FROM asignacion_banio
                WHERE id = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return resultSetToAsignacion(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar asignación con id " + id, e);
        }
        return null;
    }

    @Override
    public List<AsignacionBanio> obtenerPorEvento(int eventoId) {
        List<AsignacionBanio> lista = new ArrayList<>();
        String sql = """
                SELECT id, banio_id, evento_id, fecha_asignacion, fecha_desasignacion, latitud, longitud, nombre_ubicacion
                FROM asignacion_banio
                WHERE evento_id = ?
                ORDER BY fecha_asignacion
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(resultSetToAsignacion(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener asignaciones para evento " + eventoId, e);
        }
        return lista;
    }

    @Override
    public List<AsignacionBanio> obtenerTodos() {
        List<AsignacionBanio> lista = new ArrayList<>();
        String sql = """
                SELECT id, banio_id, evento_id, fecha_asignacion, fecha_desasignacion, latitud, longitud, nombre_ubicacion
                FROM asignacion_banio
                ORDER BY id
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(resultSetToAsignacion(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener todas las asignaciones", e);
        }
        return lista;
    }

    private AsignacionBanio resultSetToAsignacion(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int banioId = rs.getInt("banio_id");
        int eventoId = rs.getInt("evento_id");
        Timestamp fa = rs.getTimestamp("fecha_asignacion");
        Timestamp fd = rs.getTimestamp("fecha_desasignacion");
        double lat = rs.getDouble("latitud");
        double lon = rs.getDouble("longitud");
        String nombreUbicacion = rs.getString("nombre_ubicacion");

        Ubicacion ubicacion = null;
        if (!rs.wasNull()) {
            ubicacion = new Ubicacion(nombreUbicacion, lat, lon);
        }

        Banio banio = banioRepository.buscar(banioId);
        Evento evento = eventoRepository.buscar(eventoId);

        return new AsignacionBanio(
                id,
                banio,
                evento,
                fa != null ? fa.toLocalDateTime() : null,
                fd != null ? fd.toLocalDateTime() : null,
                ubicacion
        );
    }
}
