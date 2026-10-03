package Repository.DB;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseConnection {

    private static Connection connection;
    private static final Map<String, String> env = new HashMap<>();

    static {
        cargarVariablesEnv();
    }

    private static void cargarVariablesEnv() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("TPO/.env");
        }

        if (envFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int equalsIdx = line.indexOf('=');
                    if (equalsIdx > 0) {
                        String key = line.substring(0, equalsIdx).trim();
                        String val = line.substring(equalsIdx + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        env.put(key, val);
                    }
                }
            } catch (IOException e) {
                System.err.println("Advertencia: No se pudo leer el archivo .env: " + e.getMessage());
            }
        }
    }

    public static String get(String key, String defaultValue) {
        if (env.containsKey(key)) {
            return env.get(key);
        }
        String systemEnv = System.getenv(key);
        return systemEnv != null ? systemEnv : defaultValue;
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = get("DB_URL", "jdbc:postgresql://localhost:5432/postgres");
            String user = get("DB_USER", "postgres");
            String password = get("DB_PASSWORD", "");

            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                System.err.println("ADVERTENCIA: Driver org.postgresql.Driver no encontrado en el classpath.");
            }

            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    public static void cerrarConexion() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexión: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
