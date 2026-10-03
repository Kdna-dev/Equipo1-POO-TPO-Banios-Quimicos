package Repository.DB;

import Dominio.*;
import Dominio.BanioState.*;
import Repository.IBanioRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BanioRepositoryDB implements IBanioRepository {

    private final Connection connection;

    public BanioRepositoryDB(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void guardar(Banio banio) {
        boolean tieneId = banio.getId() > 0;
        String sql = tieneId
                ? """
                  INSERT INTO banio
                  (id, nombre, latitud, longitud, nombre_ubicacion, estado, capacidad, funcionalidades, tiempo_promedio)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                  """
                : """
                  INSERT INTO banio
                  (nombre, latitud, longitud, nombre_ubicacion, estado, capacidad, funcionalidades, tiempo_promedio)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                  """;

        try (PreparedStatement stmt = tieneId
                ? connection.prepareStatement(sql)
                : connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int idx = 1;
            if (tieneId) {
                stmt.setInt(idx++, banio.getId());
            }

            stmt.setString(idx++, banio.getNombre());

            Ubicacion u = banio.getUbicacion();
            if (u != null) {
                stmt.setDouble(idx++, u.getLatitud());
                stmt.setDouble(idx++, u.getLongitud());
                stmt.setString(idx++, u.getNombre());
            } else {
                stmt.setNull(idx++, Types.DOUBLE);
                stmt.setNull(idx++, Types.DOUBLE);
                stmt.setNull(idx++, Types.VARCHAR);
            }

            stmt.setString(idx++, banio.getTipoEstado().name());
            stmt.setInt(idx++, banio.getCapacidad());
            stmt.setInt(idx++, banio.getFuncionalidades());
            stmt.setDouble(idx++, banio.getTiempoPromedioAtencionMinutos());

            stmt.executeUpdate();

            if (!tieneId) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        banio.setId(generatedKeys.getInt(1));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar baño", e);
        }
    }

    @Override
    public void actualizar(Banio banio) {
        String sql = """
                UPDATE banio
                SET nombre = ?,
                    latitud = ?,
                    longitud = ?,
                    nombre_ubicacion = ?,
                    estado = ?,
                    capacidad = ?,
                    funcionalidades = ?,
                    tiempo_promedio = ?
                WHERE id = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, banio.getNombre());

            Ubicacion u = banio.getUbicacion();
            if (u != null) {
                stmt.setDouble(2, u.getLatitud());
                stmt.setDouble(3, u.getLongitud());
                stmt.setString(4, u.getNombre());
            } else {
                stmt.setNull(2, Types.DOUBLE);
                stmt.setNull(3, Types.DOUBLE);
                stmt.setNull(4, Types.VARCHAR);
            }

            stmt.setString(5, banio.getTipoEstado().name());
            stmt.setInt(6, banio.getCapacidad());
            stmt.setInt(7, banio.getFuncionalidades());
            stmt.setDouble(8, banio.getTiempoPromedioAtencionMinutos());
            stmt.setInt(9, banio.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar baño", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM banio WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar baño", e);
        }
    }

    @Override
    public Banio buscar(int id) {
        String sql = """
                SELECT id, nombre, latitud, longitud, nombre_ubicacion, estado, capacidad, funcionalidades, tiempo_promedio
                FROM banio
                WHERE id = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return resultSetToBanio(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar baño con id " + id, e);
        }
        return null;
    }

    @Override
    public List<Banio> obtenerTodos() {
        List<Banio> lista = new ArrayList<>();
        String sql = """
                SELECT id, nombre, latitud, longitud, nombre_ubicacion, estado, capacidad, funcionalidades, tiempo_promedio
                FROM banio
                ORDER BY id
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(resultSetToBanio(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener todos los baños", e);
        }
        return lista;
    }

    @Override
    public List<Banio> buscarPorFuncionalidades(int mascara) {
        List<Banio> lista = new ArrayList<>();
        String sql = """
                SELECT id, nombre, latitud, longitud, nombre_ubicacion, estado, capacidad, funcionalidades, tiempo_promedio
                FROM banio
                WHERE (funcionalidades & ?) <> 0
                ORDER BY id
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, mascara);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(resultSetToBanio(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar baños por funcionalidades con máscara " + mascara, e);
        }
        return lista;
    }

    private Banio resultSetToBanio(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nombre = rs.getString("nombre");
        double lat = rs.getDouble("latitud");
        double lon = rs.getDouble("longitud");
        String nombreUbicacion = rs.getString("nombre_ubicacion");

        Ubicacion ubicacion = null;
        if (!rs.wasNull()) {
            ubicacion = new Ubicacion(nombreUbicacion, lat, lon);
        }

        int capacidad = rs.getInt("capacidad");
        int funcionalidades = rs.getInt("funcionalidades");
        double tiempoPromedio = rs.getDouble("tiempo_promedio");
        String estadoStr = rs.getString("estado");

        Banio banio = new Banio(id, nombre, ubicacion, capacidad, funcionalidades);
        banio.setTiempoPromedioAtencionMinutos(tiempoPromedio);

        EstadoBanio estado = switch (TipoEstadoBanio.valueOf(estadoStr)) {
            case LIBRE -> new EstadoLibre();
            case OCUPADO -> new EstadoOcupado();
            case SATURADO -> new EstadoSaturado();
            case MANTENIMIENTO -> new EstadoMantenimiento();
            case FUERA_SERVICIO -> new EstadoFueraDeServicio();
        };
        banio.setEstado(estado);

        return banio;
    }
}
