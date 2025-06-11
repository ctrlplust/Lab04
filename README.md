# Lab04
# Eliminar el directorio bin y todo su contenido
```
rm -rf bin
```

# Compilar Clases
Antes crear bin, Para poder compilar todos los .class en el bin
<pre> mkdir bin </pre>

<pre> javac -d bin src/*.java </pre>

# Ejecutar Clases

<pre> java -cp bin src.AreaAtencion </pre>

<pre> java -cp bin src.GeneradorPacientes </pre>

<pre> java -cp bin src.Hospital </pre>

<pre> java -cp bin src.Paciente </pre>

<pre> java -cp bin src.SimuladorUrgencia 
# Salida esperada
```
--- Ejecutando análisis de una simulación de 24h ---
=========================================
      ANÁLISIS DE LA SIMULACIÓN
=========================================

1. Pacientes atendidos en total: 106
...
... (y el resto del análisis detallado)
  
```
</pre>

