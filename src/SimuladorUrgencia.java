package src;

import java.util.*;
import java.io.*;

public class SimuladorUrgencia {

    private Hospital hospital;
    private List<Paciente> pacientesDia;
    private Map<Integer, Integer> pacientesAtendidosPorCategoria = new HashMap<>();
    private Map<Integer, Long> sumaTiemposEsperaPorCategoria = new HashMap<>();
    private Map<Integer, Integer> cantidadPorCategoria = new HashMap<>();
    private List<Paciente> pacientesExcedidos = new ArrayList<>();
    private Set<String> idsExcedidos = new HashSet<>(); // Para evitar duplicados
    private Map<String, Long> tiemposAtencion = new HashMap<>(); // Nuevo: almacenar tiempos de atención

    private Map<Integer, Integer> tiemposMaximos = Map.of(
        1, 10 * 60,  // 10 minutos
        2, 20 * 60,
        3, 30 * 60,
        4, 60 * 60,
        5, 120 * 60
    );

    public SimuladorUrgencia(List<Paciente> pacientesDia) {
        this.hospital = new Hospital();
        this.pacientesDia = pacientesDia;
    }

    public void simular(int pacientesPorDia) {
        int minutoActual = 0;
        int pacientesIngresados = 0;
        int nuevosPacientes = 0;
        Queue<Paciente> colaPacientes = new LinkedList<>(pacientesDia);

        while (minutoActual < 24 * 60) {
            int tiempoActual = minutoActual * 60;

            // Cambia la frecuencia de llegada
            if (minutoActual % 10 == 0 && pacientesIngresados < pacientesPorDia && !colaPacientes.isEmpty()) {
                Paciente nuevo = colaPacientes.poll();
                hospital.registrarPaciente(nuevo);
                pacientesIngresados++;
                nuevosPacientes++;
            }

            // Revisar pacientes que excedieron su tiempo de espera
            for (Paciente p : hospital.getColaAtencion())  {
                int maxEspera = tiemposMaximos.getOrDefault(p.getCategoria(), 120 * 60);
                long espera = minutoActual * 60 - p.getTiempoLlegada();
                if (espera > maxEspera && !idsExcedidos.contains(p.getId())) {
                    pacientesExcedidos.add(p);
                    idsExcedidos.add(p.getId());
                }
            }

            // Cambia la frecuencia de atención
            if (minutoActual % 15 == 0) {
                atenderPacientePrioritario(tiempoActual);
            }

            // Si se acumulan 3 nuevos pacientes, se atienden 2 de inmediato
            if (nuevosPacientes >= 3) {
                for (int i = 0; i < 2; i++) {
                    atenderPacientePrioritario(tiempoActual);
                }
                nuevosPacientes = 0;
            }

            minutoActual++;
        }

        mostrarEstadisticas();

        // Guardar los tiempos de atención en archivo
        new java.io.File("Simulaciones").mkdirs();
        guardarTiemposAtencion(tiemposAtencion, "Simulaciones/tiempos_atencion.txt");
    }

    private void atenderPacientePrioritario(int tiempoActual) {
        Paciente pacienteAtendido = null;

        // 1. Buscar pacientes que han esperado más de 90 minutos (5400 segundos)
        Paciente pacienteEsperandoMucho = null;
        long maxEspera = 0;
        for (Paciente p : hospital.getColaAtencion()) {
            long espera = tiempoActual - p.getTiempoLlegada();
            if (espera >= 5400 && espera > maxEspera) { // 90 minutos
                pacienteEsperandoMucho = p;
                maxEspera = espera;
            }
        }
        if (pacienteEsperandoMucho != null) {
            pacienteAtendido = pacienteEsperandoMucho;
            hospital.eliminarDeCola(pacienteAtendido);
        }

        // 2. Si no hay pacientes esperando mucho, atender excedidos
        if (pacienteAtendido == null) {
            Iterator<Paciente> it = hospital.getColaAtencion().iterator();
            while (it.hasNext()) {
                Paciente p = it.next();
                if (idsExcedidos.contains(p.getId()) && p.getTiempoLlegada() <= tiempoActual) {
                    pacienteAtendido = p;
                    hospital.eliminarDeCola(p);
                    break;
                }
            }
        }

        // 3. Si no, atender normalmente por prioridad
        if (pacienteAtendido == null) {
            pacienteAtendido = hospital.atenderSiguiente();
            if (pacienteAtendido != null && pacienteAtendido.getTiempoLlegada() > tiempoActual) {
                hospital.registrarPaciente(pacienteAtendido);
                pacienteAtendido = null;
            }
        }

        if (pacienteAtendido != null) {
            long espera = tiempoActual - pacienteAtendido.getTiempoLlegada();
            if (espera < 0) espera = 0;

            int cat = pacienteAtendido.getCategoria();
            pacientesAtendidosPorCategoria.put(cat, pacientesAtendidosPorCategoria.getOrDefault(cat, 0) + 1);
            sumaTiemposEsperaPorCategoria.put(cat, sumaTiemposEsperaPorCategoria.getOrDefault(cat, 0L) + espera);
            cantidadPorCategoria.put(cat, cantidadPorCategoria.getOrDefault(cat, 0) + 1);

            pacienteAtendido.setEstado("atendido");
            tiemposAtencion.put(pacienteAtendido.getId(), espera);
        }
    }

    private void mostrarEstadisticas() {
        System.out.println("===== Estadísticas Finales =====");
        System.out.println("\n1. Pacientes atendidos por categoría:");
        for (int cat = 1; cat <= 5; cat++) {
            System.out.println("  - Categoría " + cat + ": " + pacientesAtendidosPorCategoria.getOrDefault(cat, 0));
        }

        System.out.println("\n2. Promedio de espera por categoría:");
        for (int cat = 1; cat <= 5; cat++) {
            int cantidad = cantidadPorCategoria.getOrDefault(cat, 0);
            long suma = sumaTiemposEsperaPorCategoria.getOrDefault(cat, 0L);
            double promedio = cantidad > 0 ? (double) suma / cantidad : 0;
            System.out.printf("  - Categoría %d: %.2f segundos%n", cat, promedio);
        }

        System.out.println("\n3. Pacientes que excedieron el tiempo máximo de espera:");
        for (Paciente p : pacientesExcedidos) {
            System.out.printf("  - %s: %s %s (Cat. %d)%n", p.getId(), p.getNombre(), p.getApellido(), p.getCategoria());
        }
    }

    public static List<Paciente> cargarPacientes(String archivo) throws IOException {
        List<Paciente> lista = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length >= 7) {
                    Paciente p = new Paciente(
                        partes[1], // nombre
                        partes[2], // apellido
                        partes[0], // id
                        Integer.parseInt(partes[3]), // categoría
                        Long.parseLong(partes[5]), // tiempoLlegada
                        partes[4] // área
                    );
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    public void guardarTiemposAtencion(Map<String, Long> tiemposAtencion, String archivo) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            writer.write("ID,Nombre,Apellido,Categoría,TiempoLlegada,TiempoAtencion\n");
            for (Paciente p : hospital.getPacientesAtendidos()) {
                long tiempoAtencion = tiemposAtencion.getOrDefault(p.getId(), 0L);
                writer.write(p.getId() + "," + p.getNombre() + "," + p.getApellido() + "," +
                             p.getCategoria() + "," + p.getTiempoLlegada() + "," + tiempoAtencion + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error al guardar los tiempos de atención: " + e.getMessage());
        }
    }

    public static List<Paciente> generarPacientes(int cantidad) {
        List<Paciente> lista = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < cantidad; i++) {
            String id = String.format("PX%04d", i+1);
            String nombre = "Nombre" + i;
            String apellido = "Apellido" + i;
            int categoria = 1 + rand.nextInt(5); // 1 a 5
            long tiempoLlegada = i * 300; // cada 5 minutos
            String area = "sapu";
            lista.add(new Paciente(nombre, apellido, id, categoria, tiempoLlegada, area));
        }
        return lista;
    }

    public static void main(String[] args) throws IOException {
        List<Paciente> pacientes = cargarPacientes("Simulaciones/Pacientes_24h.txt");

        // Acumuladores para promedios
        Map<Integer, Long> sumaPorCategoria = new HashMap<>();
        Map<Integer, Integer> cantidadPorCategoria = new HashMap<>();

        for (int i = 0; i < 15; i++) {
            // Clonar la lista para cada simulación (evita efectos colaterales)
            List<Paciente> copiaPacientes = new ArrayList<>();
            for (Paciente p : pacientes) {
                copiaPacientes.add(new Paciente(
                    p.getNombre(), p.getApellido(), p.getId(), p.getCategoria(), p.getTiempoLlegada(), p.getArea()
                ));
            }
            SimuladorUrgencia simulador = new SimuladorUrgencia(copiaPacientes);
            simulador.simular(copiaPacientes.size());

            // Sumar resultados de esta simulación
            for (int cat = 1; cat <= 5; cat++) {
                long suma = simulador.sumaTiemposEsperaPorCategoria.getOrDefault(cat, 0L);
                int cantidad = simulador.cantidadPorCategoria.getOrDefault(cat, 0);
                sumaPorCategoria.put(cat, sumaPorCategoria.getOrDefault(cat, 0L) + suma);
                cantidadPorCategoria.put(cat, cantidadPorCategoria.getOrDefault(cat, 0) + cantidad);
            }
        }

        System.out.println("\n===== Promedio de espera por categoría en 15 simulaciones =====");
        for (int cat = 1; cat <= 5; cat++) {
            int cantidad = cantidadPorCategoria.getOrDefault(cat, 0);
            long suma = sumaPorCategoria.getOrDefault(cat, 0L);
            double promedio = cantidad > 0 ? (double) suma / cantidad : 0;
            System.out.printf("  - Categoría %d: %.2f segundos%n", cat, promedio);
        }

        // Para saturación:
        List<Paciente> pacientesSaturados = generarPacientes(200);
        SimuladorUrgencia simuladorSaturado = new SimuladorUrgencia(pacientesSaturados);
        simuladorSaturado.simular(pacientesSaturados.size());
        // Puedes imprimir aquí los promedios por categoría:
        System.out.println("\n===== Saturación: Promedio de espera por categoría =====");
        for (int cat = 1; cat <= 5; cat++) {
            int cantidad = simuladorSaturado.cantidadPorCategoria.getOrDefault(cat, 0);
            long suma = simuladorSaturado.sumaTiemposEsperaPorCategoria.getOrDefault(cat, 0L);
            double promedio = cantidad > 0 ? (double) suma / cantidad : 0;
            System.out.printf("  - Categoría %d: %.2f segundos%n", cat, promedio);
        }

        // Simulación de cambio de categoría
        List<Paciente> pacientesCambio = generarPacientes(10);
        Paciente malCategorizado = pacientesCambio.get(3); // Por ejemplo, el 4to paciente
        System.out.println("\nAntes del cambio: " + malCategorizado.getNombre() + " categoría: " + malCategorizado.getCategoria());
        malCategorizado.cambiarCategoria(1); // Cambia a C1
        System.out.println("Después del cambio: " + malCategorizado.getNombre() + " categoría: " + malCategorizado.getCategoria());
        System.out.println("Historial de categorías: " + malCategorizado.getHistorialCategorias());

        // ===== SEGUIMIENTO INDIVIDUAL PACIENTE C4 =====
        Paciente pacienteC4 = null;
        for (Paciente p : pacientes) {
            if (p.getCategoria() == 4) {
                pacienteC4 = p;
                break;
            }
        }

        if (pacienteC4 != null) {
            System.out.println("\n===== Seguimiento individual: Paciente de Categoría 4 =====");
            System.out.println("Seleccionado: " + pacienteC4.getNombre() + " " + pacienteC4.getApellido() + " (ID: " + pacienteC4.getId() + ")");

            List<Paciente> copiaPacientes = new ArrayList<>();
            for (Paciente p : pacientes) {
                copiaPacientes.add(new Paciente(
                    p.getNombre(), p.getApellido(), p.getId(), p.getCategoria(), p.getTiempoLlegada(), p.getArea()
                ));
            }

            SimuladorUrgencia simulador = new SimuladorUrgencia(copiaPacientes);
            simulador.simular(copiaPacientes.size());

            Long tiempoEspera = simulador.tiemposAtencion.get(pacienteC4.getId());
            if (tiempoEspera != null) {
                System.out.printf("→ Tiempo de espera hasta ser atendido: %.2f minutos (%d segundos)\n", tiempoEspera / 60.0, tiempoEspera);
            } else {
                System.out.println("→ El paciente C4 no fue atendido durante la simulación.");
            }
        } else {
            System.out.println("\n[AVISO] No se encontró paciente de categoría 4 en la muestra.");
        }
    }
}
