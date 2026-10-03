import Aplicacion.ColaService;
import Dominio.*;
import Dominio.BanioState.*;
import EstrategiaBusqueda.*;
import Repository.DB.BanioRepositoryDB;
import Repository.DB.DatabaseConnection;
import Repository.IBanioRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println(" SISTEMA DE GESTIÓN DE BAÑOS QUÍMICOS - EVENTOS MASIVOS");
        System.out.println("=========================================================\n");

        probarDominioStateStrategyYObserver();

        probarServiciosYReglaColaUnica();

        System.out.println("\n---------------------------------------------------------");
        System.out.println(" PRUEBA DE CONEXIÓN A BASE DE DATOS (SUPABASE / POSTGRES)");
        System.out.println("---------------------------------------------------------");
        probarConexionYRepositorio();
    }

    private static void probarDominioStateStrategyYObserver() {
        System.out.println(">>> 1. Configurando Baño con Observador...");
        Ubicacion plaza = new Ubicacion("Plaza de Mayo", -34.6083, -58.3712);
        // Capacidad máxima de cola: 2 personas. Funcionalidades: Movilidad reducida (1) + Lavamanos (2) = 3
        Banio banio = new Banio(101, "Baño VIP", plaza, 2, 3);

        // Suscribimos un observador a la cola virtual (Patrón Observer)
        banio.getColaVirtual().agregarObserver(new NotificadorConsolaObserver(banio.getNombre()));

        System.out.println("Estado inicial: " + banio.getTipoEstado()); // LIBRE

        System.out.println("\n>>> 2. Usuario entra directamente al baño (Patrón State)...");
        banio.entrar();
        System.out.println("Estado luego de entrar: " + banio.getTipoEstado()); // OCUPADO

        System.out.println("\n>>> 3. Nuevos usuarios se suman a la cola virtual...");
        Persona p1 = new Persona(1, "Lionel", "Messi", 36, "lio@afa.com");
        Persona p2 = new Persona(2, "Angel", "Di Maria", 36, "fideo@afa.com");

        banio.ingresarACola(p1);
        banio.ingresarACola(p2); // Alcanza capacidad máxima de cola (2)

        System.out.println("Estado luego de saturar cola: " + banio.getTipoEstado()); // SATURADO
        System.out.println("¿Permite más ingresos?: " + banio.permiteIngreso()); // false

        System.out.println("\n>>> 4. Intento de ingreso con baño saturado...");
        try {
            banio.ingresarACola(new Persona(3, "Rodrigo", "De Paul", 30, "rodri@afa.com"));
        } catch (IllegalStateException e) {
            System.out.println("Operación rechazada correctamente por State: " + e.getMessage());
        }

        System.out.println("\n>>> 5. Atendiendo al siguiente en cola...");
        Persona atendido = banio.atenderSiguiente();
        System.out.println("Persona atendida: " + atendido.getNombre());
        System.out.println("Estado actual luego de desaturar: " + banio.getTipoEstado()); // OCUPADO

        System.out.println("\n>>> 6. Probando Estrategias de Búsqueda (Patrón Strategy)...");
        Ubicacion usuario = new Ubicacion("Usuario", -34.6080, -58.3710);
        Banio banioCercanoConGente = banio; // a ~40m pero ocupado y con 1 en cola
        Banio banioLejanoVacio = new Banio(102, "Baño Lejano", new Ubicacion("Lejos", -34.6150, -58.3800), 10, 1);

        List<Banio> listaBanios = List.of(banioCercanoConGente, banioLejanoVacio);

        EstrategiaOrdenamiento porProx = new PorProximidad();
        System.out.println("Por Proximidad primero: " + porProx.ordenar(listaBanios, usuario).get(0).getNombre());

        EstrategiaOrdenamiento porEspera = new PorCantidadEspera();
        System.out.println("Por Menor Espera primero: " + porEspera.ordenar(listaBanios, usuario).get(0).getNombre());

        EstrategiaOrdenamiento mejorOpcion = new MejorOpcion();
        System.out.println("Por Mejor Opción (Caminata + Espera): " + mejorOpcion.ordenar(listaBanios, usuario).get(0).getNombre());
    }

    private static void probarServiciosYReglaColaUnica() {
        System.out.println("\n---------------------------------------------------------");
        System.out.println(" PRUEBA DE SERVICIOS Y REGLA DE COLA ÚNICA (SECCIÓN 4)");
        System.out.println("---------------------------------------------------------");

        ColaService colaService = new ColaService();

        Banio banioA = new Banio(1, "Baño Zona A", new Ubicacion("A", -34.60, -58.38), 5, 3);
        Banio banioB = new Banio(2, "Baño Zona B", new Ubicacion("B", -34.61, -58.39), 5, 1);

        banioA.getColaVirtual().agregarObserver(new NotificadorConsolaObserver(banioA.getNombre()));
        banioB.getColaVirtual().agregarObserver(new NotificadorConsolaObserver(banioB.getNombre()));

        Persona persona = new Persona(10, "Emiliano", "Martinez", 31, "dibu@afa.com");

        System.out.println(">>> 1. Emiliano Martínez ingresa a la cola del Baño Zona A...");
        colaService.ingresarACola(banioA, persona);
        System.out.println("¿Está en alguna cola?: " + colaService.estaEnAlgunaCola(persona));

        System.out.println("\n>>> 2. Emiliano Martínez intenta ingresar a la cola del Baño Zona B...");
        try {
            colaService.ingresarACola(banioB, persona);
            System.err.println("ERROR: No debería haber permitido unirse a una segunda cola!");
        } catch (IllegalStateException e) {
            System.out.println("Regla de negocio validada correctamente: " + e.getMessage());
        }

        System.out.println("\n>>> 3. Se atiende a Emiliano en el Baño Zona A...");
        colaService.atenderSiguiente(banioA);
        System.out.println("¿Sigue en alguna cola?: " + colaService.estaEnAlgunaCola(persona));

        System.out.println("\n>>> 4. Ahora que ya no está en cola, intenta unirse al Baño Zona B...");
        colaService.ingresarACola(banioB, persona);
        System.out.println("Ingreso exitoso a la nueva cola. Baño asignado ID: " + colaService.obtenerBanioIdDePersona(persona));
    }

    private static void probarConexionYRepositorio() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            System.out.println("¡Conexión a la base de datos establecida exitosamente!");

            IBanioRepository banioRepo = new BanioRepositoryDB(conn);

            System.out.println("\nConsultando baños con movilidad reducida (funcionalidades & 1 <> 0)...");
            List<Banio> adaptados = banioRepo.buscarPorFuncionalidades(1);
            System.out.println("Total encontrados en BD: " + adaptados.size());
            for (Banio b : adaptados) {
                System.out.printf("- [%d] %s | Estado: %s | Ubicación: %s%n",
                        b.getId(), b.getNombre(), b.getTipoEstado(),
                        b.getUbicacion() != null ? b.getUbicacion().getNombre() : "S/D");
            }

        } catch (SQLException e) {
            System.out.println("Aviso de conexión: " + e.getMessage());
            System.out.println("Para conectar con tu Supabase real:");
            System.out.println("1. Creá las tablas ejecutando el script de db.md en el SQL Editor de Supabase.");
            System.out.println("2. Configurá tu DB_URL, DB_USER y DB_PASSWORD en el archivo .env.");
            System.out.println("3. Asegurate de incluir el driver postgresql.jar si ejecutás fuera de tu IDE.");
        }
    }
}