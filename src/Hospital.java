package src;

import java.util.*;

public class Hospital {
    private Map<String, Paciente> pacientesTotales;
    private PriorityQueue<Paciente> colaAtencion;
    private Map<String, AreaAtencion> areasAtencion;
    private List<Paciente> pacientesAtendidos;
    private int tiempoActual = 0; // Debes actualizarlo desde el simulador

    // Comparador para la cola general
    private static class PacienteComparator implements Comparator<Paciente> {
        @Override
        public int compare(Paciente p1, Paciente p2) {
            if (p1.getCategoria() != p2.getCategoria()) {
                return Integer.compare(p1.getCategoria(), p2.getCategoria());
            }
            return Long.compare(p1.getTiempoLlegada(), p2.getTiempoLlegada());
        }
    }

    // Comparador dinámico (prioridad aumenta con espera)
    private class ComparadorDinamico implements Comparator<Paciente> {
        @Override
        public int compare(Paciente a, Paciente b) {
            long espera1 = tiempoActual - a.getTiempoLlegada();
            long espera2 = tiempoActual - b.getTiempoLlegada();
            double prioridad1 = a.getCategoria() - (espera1 / 60.0 / 30.0); // cada 30 min baja 1 punto
            double prioridad2 = b.getCategoria() - (espera2 / 60.0 / 30.0);
            if (prioridad1 != prioridad2)
                return Double.compare(prioridad1, prioridad2);
            return Long.compare(a.getTiempoLlegada(), b.getTiempoLlegada());
        }
    }

    // Constructor
    public Hospital(boolean prioridadDinamica) {
        this.pacientesTotales = new HashMap<>();
        if (prioridadDinamica) {
            this.colaAtencion = new PriorityQueue<>(new ComparadorDinamico());
        } else {
            this.colaAtencion = new PriorityQueue<>(new PacienteComparator());
        }
        this.areasAtencion = new HashMap<>();
        this.pacientesAtendidos = new ArrayList<>();

        // Crear áreas por defecto
        areasAtencion.put("sapu", new AreaAtencion("sapu", 100));
        areasAtencion.put("urgencia_adulto", new AreaAtencion("urgencia_adulto", 100));
        areasAtencion.put("infantil", new AreaAtencion("infantil", 100));
    }

    public void registrarPaciente(Paciente p) {
        pacientesTotales.put(p.getId(), p);
        colaAtencion.add(p);
        String area = p.getArea().toLowerCase();

        if (!areasAtencion.containsKey(area)) {
            areasAtencion.put(area, new AreaAtencion(area, 100)); // valor por defecto
        }

        areasAtencion.get(area).ingresarPaciente(p);
    }

    public void reasignarCategoria(String id, int nuevaCategoria) {
        Paciente p = pacientesTotales.get(id);
        if (p != null) {
            p.setCategoria(nuevaCategoria);
            p.registrarCambio("Reasignado a categoría " + nuevaCategoria);
            // Reordenar en la cola
            colaAtencion.remove(p);
            colaAtencion.add(p);
        }
    }

    public Paciente atenderSiguiente() {
        Paciente siguiente = colaAtencion.poll();
        if (siguiente != null) {
            siguiente.setEstado("atendido");
            siguiente.registrarCambio("Paciente atendido");
            pacientesAtendidos.add(siguiente);
        }
        return siguiente;
    }

    public List<Paciente> obtenerPacientesPorCategoria(int categoria) {
        List<Paciente> resultado = new ArrayList<>();
        for (Paciente p : colaAtencion) {
            if (p.getCategoria() == categoria) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    public AreaAtencion obtenerArea(String nombre) {
        return areasAtencion.get(nombre.toLowerCase());
    }

    public Collection<Paciente> getColaAtencion() {
        return new ArrayList<>(colaAtencion);
    }

    public List<Paciente> getPacientesAtendidos() {
        return pacientesAtendidos;
    }

    public boolean eliminarDeCola(Paciente p) {
        return colaAtencion.remove(p);
    }

    // Método para actualizar el tiempo actual desde el simulador
    public void setTiempoActual(int tiempoActual) {
        this.tiempoActual = tiempoActual;
    }

    public static void main(String[] args) {
        Hospital hospital = new Hospital(true);

        Paciente p1 = new Paciente("Juan", "Pérez", "001", 2, System.currentTimeMillis() / 1000L, "urgencia_adulto");
        Paciente p2 = new Paciente("Ana", "Gómez", "002", 1, System.currentTimeMillis() / 1000L, "infantil");
        Paciente p3 = new Paciente("Luis", "Martínez", "003", 3, System.currentTimeMillis() / 1000L, "urgencia_adulto");

        hospital.registrarPaciente(p1);
        hospital.registrarPaciente(p2);
        hospital.registrarPaciente(p3);

        hospital.reasignarCategoria("003", 1);

        Paciente atendido = hospital.atenderSiguiente();
        System.out.println("Paciente atendido: " + atendido.getNombre());

        List<Paciente> cat1 = hospital.obtenerPacientesPorCategoria(1);
        System.out.println("Pacientes en categoría 1:");
        for (Paciente p : cat1) {
            System.out.println(p.getNombre());
        }

        AreaAtencion area = hospital.obtenerArea("urgencia_adulto");
        if (area != null) {
            System.out.println("Área encontrada: " + area.getNombre());
        }
    }
}
