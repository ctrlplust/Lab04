package src;

import java.util.*;
import java.io.*;

public class GeneradorPacientes {

    private static final String[] Nombres = {
        "Juan", "María", "Pedro", "Ana", "Luis", "Laura", "Carlos", "Marta",
        "Javier", "Sofía", "Diego", "Isabel", "Andrés", "Clara", "Fernando"
    };

    private static final String[] Apellidos = {
        "Gómez", "Pérez", "López", "Martínez", "Sánchez", "Rodríguez", "Fernández",
        "García", "Díaz", "Moreno", "Romero", "Torres", "Vázquez", "Jiménez"
    };

    private static final String[] Areas = {
        "urgencia_adulto", "infantil", "sapu"
    };

    public static List<Paciente> generarPacientes(int n, long timestampInicio) {
        List<Paciente> pacientes = new ArrayList<>();
        Random rand = new Random();
        int idCounter = 1;

        for (int i = 0; i < n; i++) {
            String nombre = Nombres[rand.nextInt(Nombres.length)];
            String apellido = Apellidos[rand.nextInt(Apellidos.length)];
            String id = String.format("P%04d", idCounter++);
            int categoria = generarCategoria(rand.nextInt(100) + 1);
            long tiempoLlegada = timestampInicio + (i * 600); // cada 10 minutos
            String area = Areas[rand.nextInt(Areas.length)];

            Paciente p = new Paciente(nombre, apellido, id, categoria, tiempoLlegada, area);
            pacientes.add(p);
        }

        return pacientes;
    }

    private static int generarCategoria(int probabilidad) {
        if (probabilidad <= 10) return 1;
        else if (probabilidad <= 25) return 2;
        else if (probabilidad <= 43) return 3;
        else if (probabilidad <= 70) return 4;
        else return 5;
    }

public static void guardarPacientes(List<Paciente> pacientes, String archivo) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
        for (Paciente p : pacientes) {
            writer.write(
                p.getId() + "," +
                p.getNombre() + "," +
                p.getApellido() + "," +
                p.getCategoria() + "," +
                p.getArea() + "," +
                p.getTiempoLlegada() + "," +
                "en_espera," + // Estado inicial
                ""             // Historial vacío
            );
            writer.newLine();
        }
    } catch (IOException e) {
        System.err.println("Error al guardar los pacientes: " + e.getMessage());
    }
}

    public static void main(String[] args) {
        int N = 144; // 24 horas * 6 pacientes por hora
        long timestampInicio = 0L; // inicio del día simulado
        List<Paciente> pacientes = generarPacientes(N, timestampInicio);

        // Crear la carpeta antes de guardar
        new java.io.File("Simulaciones").mkdirs();

        guardarPacientes(pacientes, "Simulaciones/Pacientes_24h.txt");
    }
}
