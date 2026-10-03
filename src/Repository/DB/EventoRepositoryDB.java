package Repository.DB;

import Dominio.Evento;
import Repository.IEventoRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventoRepositoryDB implements IEventoRepository {

    private final Connection connection;

    public EventoRepositoryDB(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void guardar(Evento evento) {
        boolean tieneId = evento.getId() > 0;
        String sql = tieneId
                ? """
                  INSERT INTO evento (id, fecha, nombre, descripcion, inicio, fin)
                  VALUES (?, ?, ?, ?, ?, ?)
                  """
                : """
                  INSERT INTO evento (fecha, nombre, descripcion, inicio, fin)
                  VALUES (?, ?, ?, ?, ?)
                  """;

        try (PreparedStatement stmt = tieneId
                ? connection.prepareStatement(sql)
                : connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int idx = 1;
            if (tieneId) {
                stmt.setInt(idx++, evento.getId());
            }

            stmt.setDate(idx++, evento.getFecha() != null ? Date.valueOf(evento.getFecha()) : null);
            stmt.setString(idx++, evento.getNombre());
            stmt.setString(idx++, evento.getDescripcion());
            stmt.setTimestamp(idx++, evento.getInicio() != null ? Timestamp.valueOf(evento.getInicio()) : null);
            stmt.setTimestamp(idx++, evento.getFin() != null ? Timestamp.valueOf(evento.getFin()) : null);

            stmt.executeUpdate();

            if (!tieneId) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        evento.setId(generatedKeys.getInt(1));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar evento", e);
        }
    }

    @Override
    public void actualizar(Evento evento) {
        String sql = """
                UPDATE evento
                SET fecha = ?,
                    nombre = ?,
                    descripcion = ?,
                    inicio = ?,
                    fin = ?
                WHERE id = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, evento.getFecha() != null ? Date.valueOf(evento.getFecha()) : null);
            stmt.setString(2, evento.getNombre());
            stmt.setString(3, evento.getDescripcion());
            stmt.setTimestamp(4, evento.getInicio() != null ? Timestamp.valueOf(evento.getInicio()) : null);
            stmt.setTimestamp(5, evento.getFin() != null ? Timestamp.valueOf(evento.getFin()) : null);
            stmt.setInt(6, evento.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar evento", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM evento WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar evento", e);
        }
    }

    @Override
    public Evento buscar(int id) {
        String sql = """
                SELECT id, fecha, nombre, descripcion, inicio, fin
                FROM evento
                WHERE id = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return resultSetToEvento(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar evento con id " + id, e);
        }
        return null;
    }

    @Override
    public List<Evento> obtenerTodos() {
        List<Evento> eventos = new ArrayList<>();
        String sql = """
                SELECT id, fecha, nombre, descripcion, inicio, fin
                FROM evento
                ORDER BY id
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                eventos.add(resultSetToEvento(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener todos los eventos", e);
        }
        return eventos;
    }

    private Evento resultSetToEvento(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Date fechaDate = rs.getDate("fecha");
        String nombre = rs.getString("nombre");
        String descripcion = rs.getString("descripcion");
        Timestamp inicioTs = rs.getTimestamp("inicio");
        Timestamp finTs = rs.getTimestamp("fin");

        return new Evento(
                id,
                fechaDate != null ? fechaDate.toLocalDate() : null,
                nombre,
                descripcion,
                inicioTs != null ? inicioTs.toLocalDateTime() : null,
                finTs != null ? finTs.toLocalDateTime() : null
        );
    }
}