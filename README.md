# Buenos Aires Street Network - Route Planner

## Descripción General

Sistema avanzado de planificación de rutas para la red de calles de Buenos Aires que utiliza algoritmos de grafos optimizados para encontrar las rutas más cortas entre intersecciones. El sistema incluye una interfaz gráfica interactiva desarrollada en Java Swing que permite visualizar el mapa completo, hacer zoom extremo, y calcular rutas de manera eficiente.

## 🚀 Evolución hacia Customizable Contraction Hierarchies (CCH)

### Limitaciones Actuales de CH Clásico
El sistema actual implementa **Contraction Hierarchies (CH)** estándar, que funciona excelentemente para un solo tipo de vehículo con pesos fijos. Sin embargo, para implementar **rutas personalizadas por tipo de vehículo** (bicicletas, autos, transporte público), se requiere evolucionar hacia **Customizable Contraction Hierarchies (CCH)**.

### ¿Qué son las Customizable Contraction Hierarchies?
Las CCH separan la **topología del grafo** (orden de contracción) de los **pesos de las aristas** (costos específicos por vehículo), permitiendo:

#### Ventajas de CCH sobre CH Tradicional:
- **Múltiples Perfiles de Vehículo**: Una sola jerarquía para autos, bicicletas, peatones
- **Personalización en Tiempo Real**: Cambiar pesos sin recalcular toda la jerarquía
- **Eficiencia de Memoria**: Reutilizar la topología contraída
- **Escalabilidad**: Agregar nuevos tipos de vehículo sin preprocesamiento completo

#### Casos de Uso para Buenos Aires:
1. **Rutas para Autos**:
   - Peso = distancia + factor de tráfico
   - Evitar calles peatonales
   - Preferir avenidas principales

2. **Rutas para Bicicletas**:
   - Peso = distancia + pendiente + seguridad
   - Preferir ciclovías (campo `bicisenda`)
   - Evitar avenidas de alto tráfico

3. **Rutas Peatonales**:
   - Peso = distancia + comodidad
   - Permitir calles peatonales
   - Considerar cruces seguros

#### Implementación Futura CCH:
```java
// Fase 1: Preprocesamiento (una sola vez)
CCH.preprocess(graph);  // Crea jerarquía reutilizable

// Fase 2: Customización por vehículo (rápida)
CCH.customize(vehicleProfile);  // Aplica pesos específicos

// Fase 3: Consultas (ultra-rápidas)
CCH.query(source, target, vehicleType);
```

## Marco Teórico

### 1. **Teoría de Grafos**

#### Representación del Problema
- **Nodos (Vértices)**: Representan intersecciones de calles en Buenos Aires
- **Aristas (Edges)**: Representan segmentos de calles que conectan intersecciones
- **Pesos**: Distancia en metros de cada segmento de calle
- **Direccionalidad**: Soporte para calles unidireccionales y bidireccionales

#### Estructura del Grafo
```
Nodo i → [Lista de aristas salientes]
Nodo i ← [Lista de aristas entrantes]
```

### 2. **Contraction Hierarchies (CH)**

#### ¿Qué son las Contraction Hierarchies?
Las Contraction Hierarchies son una técnica de **preprocesamiento de grafos** que permite acelerar dramáticamente las consultas de rutas más cortas mediante la creación de una jerarquía de nodos.

#### Algoritmo de Contracción
1. **Cálculo de Importancia**: Cada nodo recibe un valor de importancia basado en:
   - Diferencia de aristas (Edge Difference)
   - Número de atajos creados (Shortcut Count)
   - Número de vecinos ya contraídos

2. **Orden de Contracción**: Los nodos se contraen en orden ascendente de importancia

3. **Creación de Atajos**: Al contraer un nodo, se crean "shortcuts" que preservan las distancias más cortas

#### Ventajas de CH
- **Preprocesamiento**: Una sola vez por grafo
- **Consultas Rápidas**: O(log n) en lugar de O(n²)
- **Escalabilidad**: Funciona eficientemente en grafos grandes (18,000+ nodos)

### 3. **Búsqueda Bidireccional de Dijkstra**

#### Algoritmo Clásico de Dijkstra
- **Complejidad**: O((V + E) log V)
- **Garantía**: Encuentra la ruta más corta desde un origen a todos los destinos
- **Limitación**: Lento para grafos grandes

#### Mejora: Búsqueda Bidireccional
La búsqueda bidireccional ejecuta simultáneamente:
1. **Búsqueda Forward**: Desde el origen hacia el destino
2. **Búsqueda Backward**: Desde el destino hacia el origen

#### Criterio de Parada
- Las búsquedas se detienen cuando se encuentran en un nodo común
- **Nodo de Encuentro (Meeting Node)**: Punto donde ambas búsquedas convergen

#### Ventajas de la Búsqueda Bidireccional
- **Espacio de Búsqueda Reducido**: Aproximadamente √(V) en lugar de V
- **Velocidad**: Hasta 2x más rápido que Dijkstra unidireccional
- **Optimalidad**: Mantiene la garantía de ruta óptima

### 4. **Optimizaciones Implementadas**

#### A. Eliminación de queue.remove()
```java
// PROBLEMÁTICO (lento):
queue.remove(node);  // O(n) operation

// OPTIMIZADO (rápido):
// Usar tracking de nodos sin remover explícitamente
```

#### B. Arrays en lugar de HashMap
```java
// Para mejores tiempos de acceso en operaciones críticas
long[] distances = new long[graph.length];  // O(1) access
```

#### C. Límites Dinámicos
```java
// Adapta los límites de búsqueda según conectividad del nodo
int maxShortcuts = Math.min(100, Math.max(10, 150 - nodeConnectivity * 3));
```

## 🗃️ Estructura de Datos

### Clase `Node`
```java
class Node {
    int id;                          // Identificador único
    ArrayList<Edge> outEdges;        // Aristas salientes
    ArrayList<Edge> inEdges;         // Aristas entrantes
    int level;                       // Nivel en la jerarquía CH
    boolean contracted;              // Estado de contracción
    long importance;                 // Valor de importancia
    Distance distance;               // Para algoritmos de búsqueda
}
```

### Clase `Edge`
```java
class Edge {
    int from, to;                    // Nodos origen y destino
    long weight;                     // Peso (distancia en metros)
    String streetName;               // Nombre de la calle
}
```

### Clase `Distance`
```java
class Distance {
    long forwardDist, backwardDist;  // Distancias bidireccionales
    int forwardQueryId, backwardQueryId;  // IDs de consulta
    int forwardPredecessor, backwardPredecessor;  // Para reconstrucción
}
```

## 🔄 Flujo de Ejecución

### 1. **Carga de Datos (main)**
```
CSV Reader → Parsing → Node/Edge Creation → Graph Construction
```

### 2. **Preprocesamiento (Contraction Hierarchies)**
```
Importance Calculation → Node Ordering → Contraction → Shortcut Creation
```

### 3. **Consulta de Rutas**
```
User Selection → Bidirectional Search → Path Reconstruction → Visualization
```

### 4. **Visualización Interactiva**
```
Map Rendering → Node Display → Route Highlighting → User Interaction
```

## 📊 Análisis de Rendimiento

### Datos del Grafo de Buenos Aires
- **Nodos**: 18,158 intersecciones
- **Aristas**: 38,082 segmentos de calle
- **Componentes Conectados**: 6 (componente principal: 18,025 nodos)
- **Tiempo de Preprocesamiento**: ~4-5 segundos
- **Tiempo de Consulta**: < 10ms por ruta

### Direccionalidad de Calles
- **Bidireccionales**: 19.4% (6,178 rutas)
- **Unidireccionales**: 80.6% (25,726 rutas)

## 🖥️ Interfaz Gráfica

### Características de la GUI
- **Framework**: Java Swing
- **Renderizado**: Graphics2D con antialiasing
- **Zoom Range**: 50x - 200,000x
- **Visualización de Nodos**: Círculos blancos para todas las intersecciones disponibles
- **Interactividad**: 
  - Pan/Drag para navegación
  - Zoom centrado en mouse
  - Click para selección de nodos
  - Visualización de rutas calculadas

### Controles de Usuario
- **Mouse Wheel**: Zoom in/out
- **Click + Drag**: Pan por el mapa
- **Click en Nodo**: Selección de origen/destino
- **Botones**: Find Route, Clear Selection, Show Console

## 🛠️ Algoritmos Auxiliares

### 1. **Análisis de Conectividad**
- **DFS Iterativo**: Para encontrar componentes conectados
- **Prevención de Stack Overflow**: Usando pila explícita en lugar de recursión

### 2. **Búsqueda de Nodos Más Cercanos**
```java
public static int findNearestNode(String targetCoord, ...)
```
- Búsqueda exacta primero, luego por proximidad euclidiana

### 3. **Reconstrucción de Rutas**
- **Forward Path**: Desde origen hasta meeting node
- **Backward Path**: Desde meeting node hasta destino
- **Path Merging**: Evita duplicación del meeting node

## Ventajas del Sistema

### Escalabilidad
- Maneja eficientemente 18,000+ nodos
- Preprocesamiento una sola vez
- Consultas sub-segundo

### Precisión
- Considera direccionalidad real de calles
- Usa distancias reales en metros
- Garantiza rutas óptimas

### Usabilidad
- Interfaz gráfica intuitiva
- Visualización completa del mapa
- Zoom extremo para detalles
- Feedback visual inmediato

## 🔧 Compilación y Ejecución

### Requisitos
- Java 8 o superior
- Archivo de datos: `main/rutas.csv`

### Comandos
```bash
# Compilación
javac TestV2.java

# Ejecución
java TestV2
```

### Flujo de Ejecución
1. Carga y parsing del CSV
2. Construcción del grafo
3. Preprocesamiento con Contraction Hierarchies
4. Análisis de conectividad
5. Consultas opcionales por consola
6. Lanzamiento automático de la GUI

## Referencias Teóricas

### Contraction Hierarchies
- **Paper Original**: "Contraction Hierarchies: Faster and Simpler Hierarchical Routing in Road Networks" (Geisberger et al., 2008)
- **Complejidad Temporal**: O(log n) para consultas
- **Complejidad Espacial**: O(n log n) para preprocesamiento

### Dijkstra Bidireccional
- **Fundamento**: Algorithmische Graphentheorie
- **Complejidad Mejorada**: O(√V) vs O(V) exploración
- **Aplicación**: Redes de transporte y routing

### Optimización de Grafos
- **Cache-friendly Data Structures**: Arrays sobre HashMaps
- **Early Termination**: Límites dinámicos basados en conectividad
- **Memory Management**: Reuso de estructuras entre consultas

---

**Desarrollado para el análisis y navegación eficiente de la red vial de Buenos Aires**

*Proyecto académico - Universidad Nacional de San Agustín*

## Estructura de Datos CSV - Buenos Aires

### SIGNIFICADO EXACTO DE CADA CAMPO

#### Identificacion
- **[0] id**: Identificador único secuencial de cada segmento de vía (1, 2, 3...)
- **[1] codigo**: Código oficial del gobierno de la ciudad para la calle (ej: 3054, 12152)

#### Nombres y Direcciones
- **[2] nomoficial**: Nombre oficial completo de la vía (con comillas si tiene caracteres especiales)
- **[3-6] alt_izqini/fin, alt_derini/fin**: Sistema de numeración de Buenos Aires
  - alt_izqini: Altura inicial lado izquierdo (números impares)
  - alt_izqfin: Altura final lado izquierdo
  - alt_derini: Altura inicial lado derecho (números pares)
  - alt_derfin: Altura final lado derecho
- **[7] nomanter**: Nombre anterior de la calle (cambios históricos)
- **[8] nom_mapa**: Nombre simplificado para mostrar en mapas

#### Características de Tráfico
- **[9] tipo_c**: Tipo de vía
  - CALLE = Calle común
  - AVENIDA = Avenida principal
- **[10] long**: Longitud exacta del tramo en metros
- **[11] sentido**: Dirección del tráfico
  - CRECIENTE = Una dirección (números crecientes)
  - DECRECIENTE = Dirección opuesta (números decrecientes)
  - DOBLE = Bidireccional (ambas direcciones)
  - PJE. PRIVADO = Pasaje privado

#### Información Adicional
- **[12] observa**: Observaciones oficiales, cambios de nombre, resoluciones
- **[13] bicisenda**: Información sobre ciclovías (mayormente vacío)
- **[14] red_jerarq**: Jerarquía vial
  - VÍA TRONCAL = Vías principales (alta capacidad)
  - VÍA DISTRIBUIDORA PRINCIPAL = Arterias importantes
  - VÍA LOCAL = Calles del barrio

#### Transporte
- **[15] tipo_ffcc**: Interacción con ferrocarril
  - Sin Iteracción = No hay cruce con trenes

#### Ubicación Administrativa
- **[16] comuna**: Número de comuna (1.0-15.0)
- **[17] com_par**: Comuna del lado de números pares
- **[18] com_impar**: Comuna del lado de números impares
- **[19] barrio**: Barrio principal
- **[20] barrio_par**: Barrio del lado par
- **[21] barrio_imp**: Barrio del lado impar

#### Coordenadas
- **[22] geometry**: Coordenadas GPS precisas en formato LINESTRING
  - Contiene todos los puntos que forman la calle
  - Formato: LINESTRING (lng lat, lng lat, ...)

### CAMPOS MÁS IMPORTANTES PARA LA APLICACIÓN:
- **[8] nom_mapa** → Nombre a mostrar al usuario
- **[10] long** → Distancia real para cálculos
- **[11] sentido** → Direccionalidad (DOBLE = bidireccional)
- **[22] geometry** → Coordenadas para el mapa
- **[14] red_jerarq** → Prioridad de la vía
- **[19] barrio** → Localización

### DATOS CONSISTENTES:
- 100% de las líneas tienen exactamente 23 campos
- Todas las coordenadas están en Buenos Aires
- Sistema de alturas funciona con números pares/impares
