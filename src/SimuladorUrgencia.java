package src;

import java.util.*;
import java.io.*;
import src.GeneradorPacientes;

public class SimuladorUrgencia {

    private Hospital hospital;
    private List<Paciente> pacientesDia;
    private Map<Integer, Integer> pacientesAtendidosPorCategoria = new HashMap<>();
    private Map<Integer, Long> sumaTiemposEsperaPorCategoria = new HashMap<>();
    private Map<Integer, Integer> cantidadPorCategoria = new HashMap<>();
    private List<Paciente> pacientesExcedidos = new ArrayList<>();
    private Set<String> idsExcedidos = new HashSet<>();
    private Map<String, Long> tiemposAtencion = new HashMap<>();

    private final Map<Integer, Integer> tiemposMaximos = Map.of(
        1, 10 * 60,  // 10 minutos
        2, 20 * 60,  // 20 minutos
        3, 30 * 60,  // 30 minutos
        4, 60 * 60,  // 60 minutos
        5, 120 * 60 // 120 minutos
    );

    public SimuladorUrgencia(List<Paciente> pacientesDia) {
        this.hospital = new Hospital(true);
        this.pacientesDia = pacientesDia;
    }

    public void simular(int pacientesPorDia) {
        int minutoActual = 0;
        int pacientesIngresados = 0;
        /*
        int nuevosPacientesRecientes = 0; // Funciona con el que se atienden en ráfaga.
        */
        Queue<Paciente> colaPacientes = new LinkedList<>(pacientesDia);

        while (minutoActual < 24 * 60) {
            long tiempoSimulacionSegundos = (long) minutoActual * 60;

            // 1. LLEGADA DE PACIENTES
            // Un nuevo paciente llega cada 10 minutos.
            if (minutoActual % 10 == 0 && pacientesIngresados < pacientesPorDia && !colaPacientes.isEmpty()) {
                Paciente nuevo = colaPacientes.poll();
                hospital.registrarPaciente(nuevo);
                pacientesIngresados++;
                /*
                nuevosPacientesRecientes++; // atienden en ráfaga.
                */
            }
            
            // === 2. ATENCIÓN EN RÁFAGA SI HAY 3 NUEVOS PACIENTES ===
            // Esta sección implementa un comportamiento poco realista en el contexto de un sistema de urgencias:
            // cada vez que llegan 3 pacientes nuevos, se los atiende inmediatamente en una "ráfaga".
            // Este enfoque no considera la disponibilidad de médicos, tiempos de espera previos, ni prioridades reales.
            // Por esa razón, he dejado esta parte comentada. Si quieres probar cómo funciona, puedes activarla,
            // pero personalmente no recomiendo su uso ya que simula un hospital utópico con atención instantánea.

            /*
            if (nuevosPacientesRecientes >= 3) {
            for (int i = 0; i < 2; i++) {
            atenderPacientePrioritario(tiempoSimulacionSegundos);
            }
            nuevosPacientesRecientes = 0;
        }
        */
            // 3. REVISIÓN DE EMERGENCIAS Y TIEMPOS EXCEDIDOS (se ejecuta cada minuto)
            Iterator<Paciente> it = hospital.getColaAtencion().iterator();
            while (it.hasNext()) {
                Paciente p = it.next();
                long espera = tiempoSimulacionSegundos - p.getTiempoLlegada();
                int maxEspera = tiemposMaximos.getOrDefault(p.getCategoria(), 120 * 60);

                if (espera > maxEspera && !idsExcedidos.contains(p.getId())) {
                    // ATENCIÓN DE EMERGENCIA: Si un C1 excede su tiempo, se atiende INMEDIATAMENTE.
                    if (p.getCategoria() == 1) {
                        System.out.println("¡ALERTA! Paciente C1 (" + p.getId() + ") excediendo tiempo. Atención inmediata.");
                        registrarAtencion(p, tiempoSimulacionSegundos);
                        it.remove(); // Elimina al paciente de la cola de espera de forma segura.
                    } else {
                        // REGISTRO DE EXCEDIDOS: Para C2-C5, solo se registran en la lista para el informe.
                        pacientesExcedidos.add(p);
                        idsExcedidos.add(p.getId());
                    }
                }
            }

            // 4. ATENCIÓN REGULAR DEL HOSPITAL
            // Se establece una tasa de atención base y predecible. Cada 15 minutos,
            // el sistema atenderá a UN solo paciente: el más prioritario en ese momento.
            // Esta cadencia (llegada cada 10 min vs atención cada 15 min) garantiza
            // que la cola crezca, forzando al sistema a usar su lógica de priorización.
            if (minutoActual % 15 == 0) {
                atenderPacientePrioritario(tiempoSimulacionSegundos);
            }
            
            minutoActual++;
        }

        mostrarEstadisticas();
        guardarTiemposAtencion("Simulaciones/tiempos_atencion.txt");
    }

    // Atiende al siguiente paciente de la cola principal.
    private void atenderPacientePrioritario(long tiempoActual) {
        Paciente pacienteAtendido = hospital.atenderSiguiente(tiempoActual);
        if (pacienteAtendido != null) {
            registrarAtencion(pacienteAtendido, tiempoActual);
        }
    }
    
    // Método de ayuda para no duplicar el código de registro de estadísticas.
    private void registrarAtencion(Paciente p, long tiempoDeAtencion) {
        long espera = tiempoDeAtencion - p.getTiempoLlegada();
        if (espera < 0) espera = 0;

        int cat = p.getCategoria();
        cantidadPorCategoria.put(cat, cantidadPorCategoria.getOrDefault(cat, 0) + 1);
        sumaTiemposEsperaPorCategoria.put(cat, sumaTiemposEsperaPorCategoria.getOrDefault(cat, 0L) + espera);

        p.setEstado("atendido");
        p.setTiempoAtencion(tiempoDeAtencion);
        tiemposAtencion.put(p.getId(), espera);
    }

    private void mostrarEstadisticas() {
        System.out.println("\n===== Estadísticas Finales de la Simulación =====");
        System.out.println("\n1. Total de Pacientes Atendidos por Categoría:");
        for (int cat = 1; cat <= 5; cat++) {
            System.out.println("  - Categoría " + cat + ": " + cantidadPorCategoria.getOrDefault(cat, 0));
        }

        System.out.println("\n2. Tiempo de Espera Promedio por Categoría:");
        for (int cat = 1; cat <= 5; cat++) {
            int cantidad = cantidadPorCategoria.getOrDefault(cat, 0);
            long suma = sumaTiemposEsperaPorCategoria.getOrDefault(cat, 0L);
            double promedio = cantidad > 0 ? (double) suma / cantidad : 0;
            System.out.printf("  - Categoría %d: %.2f segundos (%.1f minutos)\n", cat, promedio, promedio / 60.0);
        }

        System.out.println("\n3. Pacientes que Excedieron el Tiempo Máximo de Espera (no C1):");
        if (pacientesExcedidos.isEmpty()) {
            System.out.println("  - Ningún paciente de categoría 2-5 excedió su tiempo máximo.");
        } else {
            for (Paciente p : pacientesExcedidos) {
                System.out.printf("  - ID: %s (%s %s), Cat: %d\n", p.getId(), p.getNombre(), p.getApellido(), p.getCategoria());
            }
        }
    }

    public void guardarTiemposAtencion(String archivo) {
        new java.io.File("Simulaciones").mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            writer.write("ID,Nombre,Apellido,Categoría,TiempoLlegada,TiempoEsperaSegundos\n");
            for (Paciente p : hospital.getPacientesAtendidos()) {
                if(tiemposAtencion.containsKey(p.getId())) {
                    long espera = tiemposAtencion.get(p.getId());
                    writer.write(String.format("%s,%s,%s,%d,%d,%d\n",
                        p.getId(), p.getNombre(), p.getApellido(), p.getCategoria(), p.getTiempoLlegada(), espera));
                }
            }
        } catch (IOException e) {
            System.err.println("Error al guardar los tiempos de atención: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        boolean modoPromedio = false;
        if (args.length > 0 && args[0].equalsIgnoreCase("--average")) {
            modoPromedio = true;
        }

        int numeroDePacientes = 400;

        if (modoPromedio) {
            System.out.println("--- Ejecutando prueba de promedios (15 simulaciones con " + numeroDePacientes + " pacientes) ---");
            int repeticiones = 15;
            Map<Integer, Long> sumaTotalPorCategoria = new HashMap<>();
            Map<Integer, Integer> cantidadTotalPorCategoria = new HashMap<>();

            for (int i = 0; i < repeticiones; i++) {
                System.out.printf("Iniciando simulación %d de %d...\n", i + 1, repeticiones);
                List<Paciente> pacientes = GeneradorPacientes.generarPacientes(numeroDePacientes, 0);
                SimuladorUrgencia simulador = new SimuladorUrgencia(pacientes);
                simulador.simular(pacientes.size());

                for (int cat = 1; cat <= 5; cat++) {
                    long suma = simulador.sumaTiemposEsperaPorCategoria.getOrDefault(cat, 0L);
                    int cantidad = simulador.cantidadPorCategoria.getOrDefault(cat, 0);
                    sumaTotalPorCategoria.put(cat, sumaTotalPorCategoria.getOrDefault(cat, 0L) + suma);
                    cantidadTotalPorCategoria.put(cat, cantidadTotalPorCategoria.getOrDefault(cat, 0) + cantidad);
                }
            }

            System.out.println("\n===== Promedio de espera final en " + repeticiones + " simulaciones =====");
            for (int cat = 1; cat <= 5; cat++) {
                int cantidadTotal = cantidadTotalPorCategoria.getOrDefault(cat, 0);
                long sumaTotal = sumaTotalPorCategoria.getOrDefault(cat, 0L);
                double promedio = cantidadTotal > 0 ? (double) sumaTotal / cantidadTotal : 0;
                System.out.printf("  - Categoría %d: %.2f segundos (aprox. %.1f minutos)\n", cat, promedio, promedio / 60.0);
            }
        } else {
            System.out.println("--- Ejecutando análisis de una simulación de 24h con " + numeroDePacientes + " pacientes ---");
            List<Paciente> pacientesDelDia = GeneradorPacientes.generarPacientes(numeroDePacientes, 0);
            SimuladorUrgencia simulador = new SimuladorUrgencia(pacientesDelDia);
            simulador.simular(pacientesDelDia.size());
        }
    }
}
