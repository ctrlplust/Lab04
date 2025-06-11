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
    private Set<String> idsExcedidos = new HashSet<>(); // Para evitar duplicados
    private Map<String, Long> tiemposAtencion = new HashMap<>(); // almacenar tiempos de atención

    private Map<Integer, Integer> tiemposMaximos = Map.of(
        1, 10 * 60,  // 10 minutos
        2, 20 * 60,
        3, 30 * 60,
        4, 60 * 60,
        5, 120 * 60
    );

    public SimuladorUrgencia(List<Paciente> pacientesDia) {
        this.hospital = new Hospital(true);
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

            // Revisa pacientes que excedieron su tiempo de espera
            for (Paciente p : hospital.getColaAtencion())  {
                int maxEspera = tiemposMaximos.getOrDefault(p.getCategoria(), 120 * 60);
                long espera = minutoActual * 60 - p.getTiempoLlegada();
                if (espera > maxEspera && !idsExcedidos.contains(p.getId())) {
                    pacientesExcedidos.add(p);
                    idsExcedidos.add(p.getId());
                }
            }

            // Frecuencia de atencion
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

            hospital.setTiempoActual(tiempoActual);

            minutoActual++;
        }

        mostrarEstadisticas();

        // Guardar los tiempos de atención en archivo
        new java.io.File("Simulaciones").mkdirs();
        guardarTiemposAtencion(tiemposAtencion, "Simulaciones/tiempos_atencion.txt");
    }

    private void atenderPacientePrioritario(int tiempoActual) {
        Paciente pacienteAtendido = null;

        //1. Buscar pacientes que han esperado más de 90 minutos (5400 segundos)
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
            pacienteAtendido.setTiempoAtencion(tiempoActual);
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

    public void mostrarPromedioEsperaPorCategoria() {
        Map<Integer, List<Paciente>> atendidosPorCat = new HashMap<>();
        for (Paciente p : hospital.getPacientesAtendidos()) {
            atendidosPorCat.computeIfAbsent(p.getCategoria(), k -> new ArrayList<>()).add(p);
        }
        for (int cat = 1; cat <= 5; cat++) {
            List<Paciente> lista = atendidosPorCat.getOrDefault(cat, new ArrayList<>());
            double prom = lista.stream().mapToLong(Paciente::getTiempoEspera).average().orElse(0);
            System.out.printf("Categoría %d: %.2f segundos\n", cat, prom);
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
    public static void main(String[] args) {
        boolean modoPromedio = false;
        if (args.length > 0 && args[0].equalsIgnoreCase("--average")) { // average mode es para las 15 simulaciones
            modoPromedio = true;
        }

        if (modoPromedio) {
            // MODO PROMEDIO (15 SIMULACIONES)
            System.out.println("--- Ejecutando prueba de promedios (15 simulaciones) ---");
            int repeticiones = 15;
            Map<Integer, Long> sumaPorCategoria = new HashMap<>();
            Map<Integer, Integer> cantidadPorCategoria = new HashMap<>();

            for (int i = 0; i < repeticiones; i++) {
                System.out.printf("Iniciando simulación %d de %d...\n", i + 1, repeticiones);
                List<Paciente> pacientes = GeneradorPacientes.generarPacientes(200, 0);
                SimuladorUrgencia simulador = new SimuladorUrgencia(pacientes);
                simulador.simular(pacientes.size());

                for (int cat = 1; cat <= 5; cat++) {
                    long suma = simulador.sumaTiemposEsperaPorCategoria.getOrDefault(cat, 0L);
                    int cantidad = simulador.cantidadPorCategoria.getOrDefault(cat, 0);
                    sumaPorCategoria.put(cat, sumaPorCategoria.getOrDefault(cat, 0L) + suma);
                    cantidadPorCategoria.put(cat, cantidadPorCategoria.getOrDefault(cat, 0) + cantidad);
                }
            }

            System.out.println("\n===== Promedio de espera por categoría en " + repeticiones + " simulaciones =====");
            for (int cat = 1; cat <= 5; cat++) {
                int cantidadTotal = cantidadPorCategoria.getOrDefault(cat, 0);
                long sumaTotal = sumaPorCategoria.getOrDefault(cat, 0L);
                double promedio = cantidadTotal > 0 ? (double) sumaTotal / cantidadTotal : 0;
                System.out.printf("  - Categoría %d: %.2f segundos (aprox. %.1f minutos)\n", cat, promedio, promedio / 60.0);
            }

        } else {
            // MODO ANÁLISIS ÚNICO
            System.out.println("--- Ejecutando análisis de una simulación de 24h ---");
            List<Paciente> pacientesDelDia = GeneradorPacientes.generarPacientes(200, 0);
            SimuladorUrgencia simulador = new SimuladorUrgencia(pacientesDelDia);
            simulador.simular(pacientesDelDia.size());
            simulador.mostrarEstadisticas(); 
        }
    }
}
