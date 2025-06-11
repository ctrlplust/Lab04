package src;

import java.util.Stack;

public class Paciente {
    private String nombre;
    private String apellido;
    private String id;
    private int categoria; // 1:Vital, 2:Urgencia, 3:Mediana, 4:Baja, 5:Atencion general
    private long tiempoLlegada;
    private String estado; // "en_espera", "en_atencion", "atendido"
    private String area; // "SAPU", "urgencia_adulto", "infantil"
    private Stack<String> historialCambios;
    private Stack<String> historial = new Stack<>();
    private long tiempoAtencion = -1; // -1 si no ha sido atendido

    // Constructor
    public Paciente(String nombre, String apellido, String id, int categoria, long tiempoLlegada, String area) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.id = id;
        this.categoria = categoria;
        this.tiempoLlegada = tiempoLlegada;
        this.estado = "en_espera"; // Valor por defecto
        this.area = area;
        this.historialCambios = new Stack<>();
    }

    // Getters y Setters
    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getId() {
        return id;
    }

    public int getCategoria() {
        return categoria;
    }

    public long getTiempoLlegada() {
        return tiempoLlegada;
    }

    public String getEstado() {
        return estado;
    }

    public String getArea() {
        return area;
    }

    public Stack<String> getHistorialCambios() {
        return historialCambios;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public void setCategoria(int categoria) {
        this.categoria = categoria;
    }

    public void setTiempoAtencion(long tiempoAtencion) {
        this.tiempoAtencion = tiempoAtencion;
    }

    public long getTiempoAtencion() {
        return tiempoAtencion;
    }

    // Métodos
    public long tiempoEsperaActual() {
        long ahora = System.currentTimeMillis() / 1000L; // en segundos
        return (ahora - this.tiempoLlegada) / 60; // en minutos
    }

    public void registrarCambio(String descripcion) {
        historialCambios.push(descripcion);
    }

    public String obtenerUltimoCambio() {
        if (!historialCambios.isEmpty()) {
            return historialCambios.peek();
        }
        return "No hay cambios registrados.";
    }

    public void cambiarCategoria(int nuevaCategoria) {
        historial.push("Cambio de " + this.categoria + " a " + nuevaCategoria + " en " + System.currentTimeMillis());
        this.categoria = nuevaCategoria;
    }

    public Stack<String> getHistorialCategorias() {
        return historial;
    }

    public long getTiempoEspera() {
        if (tiempoAtencion < 0) return -1;
        return tiempoAtencion - tiempoLlegada;
    }
}
