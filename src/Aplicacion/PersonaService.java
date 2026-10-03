package Aplicacion;

import Dominio.Persona;
import Repository.IPersonaRepository;

import java.util.List;

public class PersonaService {

    private final IPersonaRepository personaRepository;

    public PersonaService(IPersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    public Persona registrarPersona(String nombre, String apellido, int edad, String email) {
        if (nombre == null || nombre.isBlank() || apellido == null || apellido.isBlank()) {
            throw new IllegalArgumentException("Nombre y apellido son obligatorios.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
        Persona persona = new Persona(nombre, apellido, edad, email);
        personaRepository.guardar(persona);
        return persona;
    }

    public void actualizarPersona(Persona persona) {
        if (persona == null || persona.getId() <= 0) {
            throw new IllegalArgumentException("Persona inválida para actualizar.");
        }
        personaRepository.actualizar(persona);
    }

    public void eliminarPersona(int id) {
        personaRepository.eliminar(id);
    }

    public Persona buscarPorId(int id) {
        return personaRepository.buscar(id);
    }

    public List<Persona> obtenerTodas() {
        return personaRepository.obtenerTodos();
    }
}
