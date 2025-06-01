package src;

import java.util.*;

public class AreaAtencion {
    private String nombre;
    private PriorityQueue<Paciente> pacientesHeap; // Cola de prioridad, primero por categoría, luego por tiempo de llegada
    private int capacidadMaxima;

    // Comparador para orden prioritario
    private static class PacienteComparator implements Comparator<Paciente> {
        @Override
        public int compare(Paciente p1, Paciente p2) {
            if (p1.getCategoria() != p2.getCategoria()) {
                return Integer.compare(p1.getCategoria(), p2.getCategoria());
            }
            // Si misma categoría, priorizar por el que llegó antes
            return Long.compare(p1.getTiempoLlegada(), p2.getTiempoLlegada());
        }
    }

    // Constructor
    public AreaAtencion(String nombre, int capacidadMaxima) {
        this.nombre = nombre.toLowerCase(); // homogeniza el formato
        this.capacidadMaxima = capacidadMaxima;
        this.pacientesHeap = new PriorityQueue<>(new PacienteComparator());
    }

    // Getters
    public String getNombre() {
        return nombre;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public int getCantidadPacientes() {
        return pacientesHeap.size();
    }

    // Métodos principales
    public void ingresarPaciente(Paciente p) {
        if (p != null && !estaSaturada()) {
            pacientesHeap.offer(p);
        }
    }

    public Paciente atenderPaciente() {
        return pacientesHeap.poll();
    }

    public boolean estaSaturada() {
        return pacientesHeap.size() >= capacidadMaxima;
    }

    public List<Paciente> obtenerPacientesPorHeapSort() {
        List<Paciente> copia = new ArrayList<>(pacientesHeap);
        copia.sort(new PacienteComparator()); // mantiene orden por prioridad
        return copia;
    }

    // Test de funcionamiento
    public static void main(String[] args) {
        AreaAtencion area = new AreaAtencion("urgencia_adulto", 3);

        Paciente p1 = new Paciente("Ana", "Gomez", "111", 2, System.currentTimeMillis() / 1000L - 300, "urgencia_adulto");
        Paciente p2 = new Paciente("Luis", "Martinez", "222", 1, System.currentTimeMillis() / 1000L - 100, "urgencia_adulto");
        Paciente p3 = new Paciente("Maria", "Lopez", "333", 3, System.currentTimeMillis() / 1000L - 200, "urgencia_adulto");
        Paciente p4 = new Paciente("Pedro", "Soto", "444", 4, System.currentTimeMillis() / 1000L - 50, "urgencia_adulto");

        area.ingresarPaciente(p1);
        area.ingresarPaciente(p2);
        area.ingresarPaciente(p3);

        System.out.println("¿Área saturada después de 3 pacientes?: " + area.estaSaturada());

        // Intentar ingresar un cuarto paciente
        area.ingresarPaciente(p4);
        System.out.println("Cantidad de pacientes tras intentar ingresar un cuarto: " + area.getCantidadPacientes());
        System.out.println("¿Área saturada?: " + area.estaSaturada());

        // Mostrar los nombres de los pacientes en orden de prioridad
        System.out.println("Pacientes en el área ordenados por prioridad:");
        for (Paciente p : area.obtenerPacientesPorHeapSort()) {
            System.out.println(p.getNombre() + " - Categoría: " + p.getCategoria());
        }
    }
}
