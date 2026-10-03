package Repository.DB;

import Dominio.Persona;
import Repository.IPersonaRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonaRepositoryDB implements IPersonaRepository {

    private final Connection connection;

    public PersonaRepositoryDB(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void guardar(Persona persona) {
        boolean tieneId = persona.getId() > 0;
        String sql = tieneId
                ? """
                  INSERT INTO persona (id, nombre, apellido, edad, email)
                  VALUES (?, ?, ?, ?, ?)
                  """
                : """
                  INSERT INTO persona (nombre, apellido, edad, email)
                  VALUES (?, ?, ?, ?)
                  """;

        try (PreparedStatement statement = tieneId
                ? connection.prepareStatement(sql)
                : connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int idx = 1;
            if (tieneId) {
                statement.setInt(idx++, persona.getId());
            }

            statement.setString(idx++, persona.getNombre());
            statement.setString(idx++, persona.getApellido());
            statement.setInt(idx++, persona.getEdad());
            statement.setString(idx++, persona.getEmail());

            statement.executeUpdate();

            if (!tieneId) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        persona.setId(generatedKeys.getInt(1));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar persona", e);
        }
    }

    @Override
    public void actualizar(Persona persona) {
        String sql = """
                UPDATE persona
                SET nombre = ?,
                    apellido = ?,
                    edad = ?,
                    email = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, persona.getNombre());
            statement.setString(2, persona.getApellido());
            statement.setInt(3, persona.getEdad());
            statement.setString(4, persona.getEmail());
            statement.setInt(5, persona.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar persona", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM persona WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar persona", e);
        }
    }

    @Override
    public void eliminar(Persona persona) {
        if (persona != null) {
            eliminar(persona.getId());
        }
    }

    @Override
    public Persona buscar(int id) {
        String sql = """
                SELECT id, nombre, apellido, edad, email
                FROM persona
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return resultSetToPersona(result);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar persona", e);
        }
        return null;
    }

    @Override
    public List<Persona> obtenerTodos() {
        List<Persona> personas = new ArrayList<>();
        String sql = """
                SELECT id, nombre, apellido, edad, email
                FROM persona
                ORDER BY id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                personas.add(resultSetToPersona(result));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener personas", e);
        }
        return personas;
    }

    private Persona resultSetToPersona(ResultSet result) throws SQLException {
        return new Persona(
                result.getInt("id"),
                result.getString("nombre"),
                result.getString("apellido"),
                result.getInt("edad"),
                result.getString("email")
        );
    }
}