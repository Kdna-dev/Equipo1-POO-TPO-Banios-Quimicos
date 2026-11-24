import Modelo.Aplicacion.BanioService;
import Modelo.Aplicacion.ColaService;
import Modelo.Aplicacion.PersonaService;
import Modelo.Repository.DB.BanioRepositoryDB;
import Modelo.Repository.DB.DatabaseConnection;
import Modelo.Repository.DB.PersonaRepositoryDB;
import Modelo.Repository.IBanioRepository;
import Modelo.Repository.IPersonaRepository;
import Modelo.Repository.Memoria.BanioRepositoryMemoria;
import Modelo.Repository.Memoria.PersonaRepositoryMemoria;
import Controlador.AdminController;
import Controlador.BanioController;
import Controlador.EventoController;
import Controlador.PersonaController;
import Controlador.Http.ArchivosEstaticos;
import Controlador.Http.ServidorHttp;
import Modelo.Aplicacion.AtencionService;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

/**
 * Arranque web: levanta la API + el frontend (carpeta web/) sobre los servicios del equipo.
 * Arma las tres capas MVC: Modelo (servicios + repositorios), Controlador (rutas de la API) y Vista (JSON / web/).
 *
 *   Sin .env / DB_URL  -> repositorios en memoria con datos de ejemplo (demo).
 *   Con DB_URL         -> repositorios Postgres/Supabase (tablas de db.md).
 *
 * Uso: java -cp out MainWeb [puerto] [carpetaWeb]
 */
public class MainWeb {

    public static void main(String[] args) throws Exception {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path web = Path.of(args.length > 1 ? args[1] : "web");

        IBanioRepository banioRepo;
        IPersonaRepository personaRepo;
        boolean demo = DatabaseConnection.get("DB_URL", null) == null;
        if (demo) {
            banioRepo = new BanioRepositoryMemoria();
            personaRepo = new PersonaRepositoryMemoria();
        } else {
            Connection conn = DatabaseConnection.getConnection();
            banioRepo = new BanioRepositoryDB(conn);
            personaRepo = new PersonaRepositoryDB(conn);
        }

        BanioService banios = new BanioService(banioRepo, new ColaService());
        PersonaService personas = new PersonaService(personaRepo);
        AtencionService atencion = new AtencionService(banios, personas);

        if (demo) sembrar(atencion);

        ServidorHttp servidor = new ServidorHttp(List.of(
                new EventoController(atencion, "Visita del Papa a Buenos Aires"),
                new BanioController(atencion),
                new PersonaController(atencion),
                new AdminController(atencion)
        ), new ArchivosEstaticos(web), atencion);
        servidor.iniciar(puerto);
        System.out.println("Baños químicos (" + (demo ? "datos de ejemplo en memoria" : "base de datos") + ")");
        System.out.println("  App:   http://localhost:" + puerto + "/");
        System.out.println("  Admin: http://localhost:" + puerto + "/admin.html");
    }

    /** Recorrido de ejemplo entre la Catedral y el Obelisco (coordenadas aproximadas). */
    private static void sembrar(AtencionService atencion) {
        double[][] puntos = { {-34.6083, -58.3731}, {-34.6074, -58.3748}, {-34.6066, -58.3765},
                {-34.6058, -58.3782}, {-34.6049, -58.3799}, {-34.6037, -58.3816} };
        int[] capacidad = { 6, 5, 5, 6, 4, 6 };
        int[] funciones = {
                7, 2, 3, 4, 1 | 8, 2 | 16 };
        String[] lugares = { "Plaza de Mayo (Catedral)", "Diagonal Norte", "Florida y Perón", "Av. 9 de Julio", "Corrientes y Cerrito", "Obelisco" };
        for (int i = 0; i < puntos.length; i++) {
            atencion.altaBanio(lugares[i], puntos[i][0], puntos[i][1], capacidad[i], funciones[i], 3.0);
        }
    }
}
