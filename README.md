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

```
java -cp bin src.SimuladorUrgencia
```
```
java -cp bin src.SimuladorUrgencia --average
```
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

#Salida Esperada (--average)

```
===== Promedio de espera por categoría en 15 simulaciones =====
  - Categoría 1: 90.63 segundos (aprox. 1.5 minutos)
  - Categoría 2: 96.46 segundos (aprox. 1.6 minutos)
  - Categoría 3: 104.13 segundos (aprox. 1.7 minutos)
  - Categoría 4: 108.66 segundos (aprox. 1.8 minutos)
  - Categoría 5: 94.47 segundos (aprox. 1.6 minutos)
```




