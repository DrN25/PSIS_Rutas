package main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

// Swing imports for GUI
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import models.*;
import algorithms.*;
import utils.*;

public class Test {
    
    // GUI Components
    private JFrame mainFrame;
    private MapPanel mapPanel;
    private JLabel statusLabel;
    private JButton findRouteButton;
    private JButton clearButton;
    private JButton showGuiButton;
    private JTextArea infoArea;
    private JScrollPane mapScrollPane;
    
    // Map data and state
    private static Node[] graphData;
    private static Map<Integer, String> idToCoordData;
    private static Map<String, String> streetNameMapData;
    private static BidirectionalSearch bidirectionalSearchData;
    
    // Map visualization state
    private double offsetX = 0;
    private double offsetY = 0;
    private double scale = 1.0;
    private double lastMouseX, lastMouseY;
    private boolean isDragging = false;
    
    // Route selection state
    private Integer selectedOrigin = null;
    private Integer selectedDestination = null;
    private List<Integer> currentRoute = new ArrayList<>();
    
    // Map bounds
    private double minLat, maxLat, minLon, maxLon;
    private double mapWidth = 1200;
    private double mapHeight = 800;
    
    // Colors
    private static final Color STREET_COLOR = Color.LIGHT_GRAY;
    private static final Color SELECTED_ROUTE_COLOR = Color.RED;
    private static final Color ORIGIN_COLOR = Color.GREEN;
    private static final Color DESTINATION_COLOR = Color.BLUE;
    private static final Color NODE_COLOR = Color.DARK_GRAY;
    private static final Color BACKGROUND_COLOR = Color.WHITE;

    
    // Función especial para mostrar recorrido detallado entre nodos específicos
    public static void showDetailedRoute(Node[] graph, Map<Integer, String> idToCoord, 
                                       Map<String, String> streetNameMap, int sourceId, int targetId, 
                                       String sourceName, String targetName) {
        System.out.println("\n[MAP] RECORRIDO DETALLADO:");
        System.out.println("==========================================");
        System.out.println("Origen:  Nodo " + sourceId + " - " + sourceName);
        System.out.println("         Coordenadas: " + idToCoord.get(sourceId));
        System.out.println("Destino: Nodo " + targetId + " - " + targetName);
        System.out.println("         Coordenadas: " + idToCoord.get(targetId));
        System.out.println("==========================================");
        
        // Probar con Dijkstra simple primero
        long simpleDistance = GraphUtils.simpleDijkstra(graph, sourceId, targetId);
        System.out.println("[SEARCH] Dijkstra Simple: " + (simpleDistance == -1 ? "No route found" : simpleDistance + " metros"));
        
        // Probar con búsqueda bidireccional
        BidirectionalSearch bidirectionalSearch = new BidirectionalSearch(graph);
        
        // Reset distance data
        for (Node node : graph) {
            node.distance = new Distance();
        }
        
        BidirectionalSearch.PathResult result = bidirectionalSearch.computeShortestPath(sourceId, targetId, 999);
        System.out.println("[SEARCH] Busqueda Bidireccional: " + (result.distance == -1 ? "No route found" : result.distance + " metros"));
        
        if (result.distance != -1) {
            List<Integer> path = bidirectionalSearch.reconstructPath(sourceId, targetId, result.meetingNode);
            
            if (path.size() > 1) {
                System.out.println("\n[ROUTE] RUTA ENCONTRADA (" + path.size() + " nodos):");
                long totalDistance = 0;
                
                for (int i = 0; i < path.size() - 1; i++) {
                    int from = path.get(i);
                    int to = path.get(i + 1);
                    
                    // Buscar distancia real entre nodos consecutivos
                    long segmentDistance = 0;
                    String streetName = "Unknown street";
                    
                    // Buscar en las aristas del grafo
                    for (Edge edge : graph[from].outEdges) {
                        if (edge.to == to) {
                            segmentDistance = edge.weight;
                            streetName = edge.streetName;
                            break;
                        }
                    }
                    
                    // Fallback: buscar en el mapa de calles
                    if (streetName.equals("Unknown street")) {
                        streetName = streetNameMap.get(from + "_" + to);
                        if (streetName == null) streetName = "Unknown street";
                    }
                    
                    totalDistance += segmentDistance;
                    
                    System.out.printf("  %d. Nodo %d → Nodo %d | %s | %d metros\n", 
                                     (i + 1), from, to, streetName, segmentDistance);
                    System.out.printf("     Coords: %s → %s\n", 
                                     idToCoord.get(from), idToCoord.get(to));
                }
                
                System.out.println("\n[SUMMARY] RESUMEN:");
                System.out.println("   Total distance: " + totalDistance + " metros");
                System.out.println("   Segments: " + (path.size() - 1));
                System.out.println("   Meeting node: " + result.meetingNode);
            } else {
                System.out.println("[ERROR] No se pudo reconstruir la ruta completa");
            }
        } else {
            System.out.println("[ERROR] No existe ruta entre estos nodos");
            
            // Verificar si están en el mismo componente
            System.out.println("\n[ANALYSIS] ANALISIS DE CONECTIVIDAD:");
            System.out.println("Verificando si los nodos están en el mismo componente...");
        }
        
        System.out.println("==========================================\n");
    }
    
    // Función para analizar la conectividad local de un nodo
    public static void analyzeLocalConnectivity(Node[] graph, Map<Integer, String> idToCoord, 
                                              Map<String, String> streetNameMap, int nodeId, String nodeName) {
        System.out.println("\n[CONNECTIVITY] ANALISIS LOCAL DEL NODO " + nodeId + " (" + nodeName + "):");
        System.out.println("Coordenadas: " + idToCoord.get(nodeId));
        
        Node node = graph[nodeId];
        System.out.println("Aristas salientes: " + node.outEdges.size());
        System.out.println("Aristas entrantes: " + node.inEdges.size());
        
        if (node.outEdges.size() > 0) {
            System.out.println("\nConexiones SALIENTES:");
            for (int i = 0; i < Math.min(node.outEdges.size(), 5); i++) {
                Edge edge = node.outEdges.get(i);
                System.out.printf("  -> Nodo %d | %s | %d metros\n", 
                                 edge.to, edge.streetName, edge.weight);
                System.out.printf("     Coords destino: %s\n", idToCoord.get(edge.to));
            }
            if (node.outEdges.size() > 5) {
                System.out.println("  ... y " + (node.outEdges.size() - 5) + " conexiones más");
            }
        }
        
        if (node.inEdges.size() > 0) {
            System.out.println("\nConexiones ENTRANTES:");
            for (int i = 0; i < Math.min(node.inEdges.size(), 5); i++) {
                Edge edge = node.inEdges.get(i);
                System.out.printf("  <- Nodo %d | %s | %d metros\n", 
                                 edge.from, edge.streetName, edge.weight);
                System.out.printf("     Coords origen: %s\n", idToCoord.get(edge.from));
            }
            if (node.inEdges.size() > 5) {
                System.out.println("  ... y " + (node.inEdges.size() - 5) + " conexiones más");
            }
        }
        
        if (node.outEdges.size() == 0 && node.inEdges.size() == 0) {
            System.out.println("[WARNING] Este nodo está AISLADO - no tiene conexiones!");
        }
        
        System.out.println("==========================================");
    }
    
    // Función para encontrar pares de nodos conectados para pruebas
    public static void findConnectedPairs(Node[] graph, Map<Integer, String> idToCoord, 
                                        Map<String, String> streetNameMap, int maxTests) {
        System.out.println("\n[TESTING] BUSCANDO PARES DE NODOS CONECTADOS:");
        System.out.println("==========================================");
        
        int testsFound = 0;
        BidirectionalSearch bidirectionalSearch = new BidirectionalSearch(graph);
        
        // Probar con nodos que tengan conexiones salientes
        for (int source = 0; source < Math.min(graph.length, 50) && testsFound < maxTests; source++) {
            if (graph[source].outEdges.size() > 0) {
                // Probar con algunos de sus vecinos directos
                for (Edge edge : graph[source].outEdges) {
                    if (testsFound >= maxTests) break;
                    
                    int target = edge.to;
                    
                    // Reset distance data
                    for (Node node : graph) {
                        node.distance = new Distance();
                    }
                    
                    // Probar la búsqueda
                    BidirectionalSearch.PathResult result = bidirectionalSearch.computeShortestPath(source, target, 999);
                    
                    if (result.distance != -1) {
                        testsFound++;
                        System.out.printf("[TEST %d] RUTA ENCONTRADA:\n", testsFound);
                        System.out.printf("  Nodo %d -> Nodo %d | Distancia: %d metros\n", 
                                         source, target, result.distance);
                        System.out.printf("  Coords: %s -> %s\n", 
                                         idToCoord.get(source), idToCoord.get(target));
                        System.out.printf("  Via: %s\n", edge.streetName);
                        
                        // Mostrar la ruta completa
                        List<Integer> path = bidirectionalSearch.reconstructPath(source, target, result.meetingNode);
                        if (path.size() > 1) {
                            System.out.printf("  Ruta completa (%d nodos): ", path.size());
                            for (int i = 0; i < Math.min(path.size(), 5); i++) {
                                System.out.print(path.get(i));
                                if (i < Math.min(path.size(), 5) - 1) System.out.print(" -> ");
                            }
                            if (path.size() > 5) System.out.print(" ... -> " + path.get(path.size() - 1));
                            System.out.println();
                        }
                        System.out.println();
                    }
                }
            }
        }
        
        if (testsFound == 0) {
            System.out.println("[WARNING] No se encontraron rutas exitosas en los primeros 50 nodos");
        }
        
        System.out.println("==========================================");
    }      
    public static void main(String[] args) throws IOException {
        String dirpath = "main/rutas.csv";
        
        // Lectura con UTF-8
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dirpath), StandardCharsets.UTF_8));
        
        String line;
        List<Route> routes = new ArrayList<>();
        Map<String, Integer> nodeIndex = new HashMap<>();
        Map<Integer, String> idToCoord = new HashMap<>();
        AtomicInteger currentId = new AtomicInteger(0);
        
        br.readLine(); // Skip header
        int lineNumber = 1; // Para tracking de líneas (empezando desde 1 después del header)
        
        while ((line = br.readLine()) != null) {
            lineNumber++;
            
            // Usar el nuevo parser
            String[] fields = CSVUtils.parseCSVLine(line);
            
            // Verificar que tenga exactamente 23 campos
            if (fields.length != 23) {
                System.err.println("ERROR: Línea " + lineNumber + " tiene " + fields.length + " campos, se esperaban 23");
                System.err.println("Contenido: " + line.substring(0, Math.min(100, line.length())) + "...");
                continue; // Saltar esta línea
            }
            
            // Debug: mostrar algunos valores para las primeras líneas
            if (lineNumber <= 5) {
                System.out.println("Debug línea " + lineNumber + " (" + fields.length + " campos):");
                for (int i = 0; i < Math.min(fields.length, 12); i++) {
                    System.out.println("  Campo[" + i + "]: '" + fields[i] + "'");
                }
                System.out.println();
            }
            
            // Verificar que el último campo contenga geometry
            if (fields.length < 23 || !fields[22].contains("LINESTRING")) {
                System.err.println("ERROR: Línea " + lineNumber + " no tiene geometría válida en el campo 23");
                continue;
            }
            
            String geometry = fields[22].replace("\"", "").replace("LINESTRING", "").trim();
            if (geometry.isEmpty() || !geometry.startsWith("(") || !geometry.endsWith(")")) {
                System.err.println("ERROR: Línea " + lineNumber + " tiene geometría malformada: " + geometry.substring(0, Math.min(50, geometry.length())));
                continue;
            }
            
            String[] points = geometry.substring(1, geometry.length() - 1).split(", ");
            if (points.length < 2) {
                System.err.println("ERROR: Línea " + lineNumber + " tiene menos de 2 puntos en la geometría");
                continue;
            }
            
            String start = points[0].trim();
            String end = points[points.length - 1].trim();
            
            // Extraer nombre de calle usando los campos correctos
            String street = "Unknown";
            
            // nom_mapa está en el campo 8 (índice 7)
            if (fields[7] != null && !fields[7].trim().isEmpty()) {
                street = fields[7].replace("\"", "").trim();
            }
            
            // Si nom_mapa está vacío, usar nomoficial (campo 3, índice 2)
            if (street.isEmpty() || street.equals("Unknown")) {
                if (fields[2] != null && !fields[2].trim().isEmpty()) {
                    street = fields[2].replace("\"", "").trim();
                }
            }
            
            if (street.isEmpty()) {
                street = "Unknown";
            }
            
            // Determinar si la calle es bidireccional basándose en el campo 'sentido' (campo 12, índice 11)
            boolean isBidirectional = false;
            if (fields.length > 11 && fields[11] != null) {
                String sentido = fields[11].replace("\"", "").trim().toUpperCase();
                isBidirectional = sentido.equals("DOBLE");
                
                // Debug: mostrar sentidos encontrados
                if (lineNumber <= 10) {
                    System.out.println("Línea " + lineNumber + " - Sentido: '" + sentido + "' - Bidireccional: " + isBidirectional);
                }
            }
            
            int origin = nodeIndex.computeIfAbsent(start, k -> {
                int id = currentId.getAndIncrement();
                idToCoord.put(id, start);
                return id;
            });
            
            int destination = nodeIndex.computeIfAbsent(end, k -> {
                int id = currentId.getAndIncrement();
                idToCoord.put(id, end);
                return id;
            });
            
            // Extraer longitud (campo 11, índice 10)
            double length = 1.0;
            try {
                if (fields[10] != null && !fields[10].trim().isEmpty()) {
                    String lengthStr = fields[10].replace("\"", "").trim();
                    length = Double.parseDouble(lengthStr);
                }
            } catch (NumberFormatException e) {
                System.err.println("WARNING: Línea " + lineNumber + " tiene longitud inválida: " + fields[10]);
            }
            
            long cost = (long) length;
            routes.add(new Route(origin, destination, cost, street, isBidirectional));
        }
        br.close();
        
        int n = nodeIndex.size();
        System.out.println("Total lines processed: " + (lineNumber - 1));
        System.out.println("Number of nodes: " + n);
        System.out.println("Number of routes: " + routes.size());
        
        // Initialize graph
        Node[] graph = new Node[n];
        for (int i = 0; i < n; i++) {
            graph[i] = new Node(i);
        }
        
        // Add edges to graph considerando direccionalidad
        int totalEdges = 0;
        for (Route route : routes) {
            // Siempre añadir la arista en la dirección original
            Edge forwardEdge = new Edge(route.origin, route.destination, route.cost, route.street);
            graph[route.origin].outEdges.add(forwardEdge);
            graph[route.destination].inEdges.add(forwardEdge);
            totalEdges++;
            
            // Si es bidireccional, añadir también la arista inversa
            if (route.isBidirectional) {
                Edge backwardEdge = new Edge(route.destination, route.origin, route.cost, route.street);
                graph[route.destination].outEdges.add(backwardEdge);
                graph[route.origin].inEdges.add(backwardEdge);
                totalEdges++;
            }
        }
        
        System.out.println("Number of directed edges: " + totalEdges);

        // Análisis de direccionalidad
        int bidirectionalCount = 0;
        int unidirectionalCount = 0;
        for (Route route : routes) {
            if (route.isBidirectional) {
                bidirectionalCount++;
            } else {
                unidirectionalCount++;
            }
        }

        System.out.println("Bidirectional routes: " + bidirectionalCount);
        System.out.println("Unidirectional routes: " + unidirectionalCount);
        System.out.println("Percentage bidirectional: " + (bidirectionalCount * 100.0 / routes.size()) + "%");
                
        // Crear mapa de aristas para búsqueda rápida
        Map<String, String> streetNameMap = new HashMap<>();
        for (Route route : routes) {
            streetNameMap.put(route.origin + "_" + route.destination, route.street);
            if (route.isBidirectional) {
                streetNameMap.put(route.destination + "_" + route.origin, route.street);
            }
        }
        
        System.out.println("Preprocessing the graph...");
        long preprocessingStartTime = System.currentTimeMillis();
        ContractionHierarchies ch = new ContractionHierarchies(graph);
        ch.preprocess();
        long preprocessingTime = System.currentTimeMillis() - preprocessingStartTime;
        System.out.println("Preprocessing complete!");
        System.out.println("Total preprocessing time: " + (preprocessingTime / 1000.0) + " seconds");
        
        // Análisis de conectividad del grafo
        System.out.println("Analyzing graph connectivity...");
        Map<Integer, Integer> componentMap = new HashMap<>();
        int componentCount = 0;
        
        // Simple DFS para encontrar componentes conectados
        boolean[] visited = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                componentCount++;
                GraphUtils.dfsComponent(graph, i, visited, componentMap, componentCount);
            }
        }
        
        System.out.println("Number of connected components: " + componentCount);
        
        // Mostrar tamaño de cada componente
        Map<Integer, Integer> componentSizes = new HashMap<>();
        for (int comp : componentMap.values()) {
            componentSizes.put(comp, componentSizes.getOrDefault(comp, 0) + 1);
        }
        
        System.out.println("Component sizes:");
        for (Map.Entry<Integer, Integer> entry : componentSizes.entrySet()) {
            if (entry.getValue() > 1) { // Solo mostrar componentes con más de 1 nodo
                System.out.println("Component " + entry.getKey() + ": " + entry.getValue() + " nodes");
            }
        }
        
        // Mostrar algunas coordenadas de ejemplo
        System.out.println("\nExample coordinates from the dataset:");
        int count = 0;
        for (Map.Entry<Integer, String> entry : idToCoord.entrySet()) {
            if (count < 10) {
                System.out.println("ID " + entry.getKey() + ": " + entry.getValue() + " (Component: " + componentMap.get(entry.getKey()) + ")");
                count++;
            } else {
                break;
            }
        }
        
        // CONSULTA ESPECIFICA AUTOMATICA: Nodo 0 (CANTILO, INT.) -> Nodo 9 (LA CACHILA)
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CONSULTA ESPECIFICA AUTOMATICA");
        System.out.println("=".repeat(60));
        
        // Buscar información sobre los nodos específicos
        String node0Name = "CANTILO, INT.";
        String node9Name = "LA CACHILA";
        
        if (idToCoord.containsKey(0) && idToCoord.containsKey(9)) {
            showDetailedRoute(graph, idToCoord, streetNameMap, 0, 9, node0Name, node9Name);
            
            // ANALISIS PROFUNDO: Conectividad local de los nodos
            analyzeLocalConnectivity(graph, idToCoord, streetNameMap, 0, node0Name);
            analyzeLocalConnectivity(graph, idToCoord, streetNameMap, 9, node9Name);
            
            // BUSCAR RUTAS QUE SI FUNCIONEN para verificar que el sistema esta bien
            findConnectedPairs(graph, idToCoord, streetNameMap, 3);
            
        } else {
            System.out.println("ERROR: Los nodos 0 o 9 no existen en el dataset");
            System.out.println("Nodos disponibles: 0 a " + (n - 1));
            if (idToCoord.containsKey(0)) {
                System.out.println("Nodo 0 existe: " + idToCoord.get(0));
            }
            if (idToCoord.containsKey(9)) {
                System.out.println("Nodo 9 existe: " + idToCoord.get(9));
            }
        }
        
        // Query phase - input manual del usuario
        BidirectionalSearch bidirectionalSearch = new BidirectionalSearch(graph);
        
        Scanner in = new Scanner(System.in);
        System.out.println("\nEnter the number of queries (0 to skip):");
        int q = in.nextInt();
        in.nextLine(); // Consume newline
        
        for (int i = 0; i < q; i++) {
            System.out.println("Enter source coordinates (longitude latitude):");
            System.out.println("Example: -58.462187566063186 -34.53451590464167");
            String sourceCoord = in.nextLine().trim();
            
            System.out.println("Enter target coordinates (longitude latitude):");
            System.out.println("Example: -58.46678379560394 -34.535002649404596");
            String targetCoord = in.nextLine().trim();
            
            // Encontrar los nodos más cercanos a las coordenadas dadas
            int source = GraphUtils.findNearestNode(sourceCoord, nodeIndex, idToCoord);
            int target = GraphUtils.findNearestNode(targetCoord, nodeIndex, idToCoord);
            
            if (source == -1) {
                System.out.println("Could not find a node near the source coordinates: " + sourceCoord);
                continue;
            }
            
            if (target == -1) {
                System.out.println("Could not find a node near the target coordinates: " + targetCoord);
                continue;
            }
            
            System.out.println("Found nearest nodes - Source: " + source + " (Component: " + componentMap.get(source) + "), Target: " + target + " (Component: " + componentMap.get(target) + ")");
            
            // Verificar si están en el mismo componente
            if (!componentMap.get(source).equals(componentMap.get(target))) {
                System.out.println("No route possible - nodes are in different connected components");
                System.out.println("---");
                continue;
            }
            
            // Reset distance data for new query
            for (Node node : graph) {
                node.distance = new Distance();
            }
            
            BidirectionalSearch.PathResult result = bidirectionalSearch.computeShortestPath(source, target, i);
            
            if (result.distance == -1) {
                System.out.println("No route exists with Contraction Hierarchies");
                // FALLBACK: Intentar Dijkstra simple para verificar conectividad real
                System.out.println("Trying simple Dijkstra as fallback...");
                long simpleDijkstraResult = GraphUtils.simpleDijkstra(graph, source, target);
                
                if (simpleDijkstraResult == -1) {
                    System.out.println("Confirmed: No route exists between the given coordinates (considering street directions)");
                } else {
                    System.out.println("WARNING: Route exists (" + simpleDijkstraResult + "m) but CH failed to find it");
                    System.out.println("This suggests an issue with the Contraction Hierarchies preprocessing");
                }
            } else {
                System.out.println("Shortest distance: " + result.distance + " meters");
                
                // Reconstruir y mostrar la ruta
                List<Integer> path = bidirectionalSearch.reconstructPath(source, target, result.meetingNode);
                
                if (path.size() > 1) {
                    System.out.println("Route (" + path.size() + " nodes):");
                    for (int j = 0; j < path.size() - 1; j++) {
                        int from = path.get(j);
                        int to = path.get(j + 1);
                        
                        // Buscar el nombre de la calle
                        String streetName = streetNameMap.get(from + "_" + to);
                        if (streetName == null) {
                            // Buscar en aristas del grafo si no está en el mapa original
                            for (Edge edge : graph[from].outEdges) {
                                if (edge.to == to) {
                                    streetName = edge.streetName;
                                    break;
                                }
                            }
                            
                            if (streetName == null) streetName = "Unknown street";
                        }
                        
                        System.out.println("  " + (j + 1) + ". Node " + from + " -> " + to + " via " + streetName);
                    }
                    
                    // Mostrar las coordenadas reales de origen y destino
                    System.out.println("\nActual origin coordinates: " + idToCoord.get(source));
                    System.out.println("Actual destination coordinates: " + idToCoord.get(target));
                } else {
                    System.out.println("Unable to reconstruct the complete path");
                }
            }
            System.out.println("---");
        }
        
        in.close();
        
        // Automatically launch GUI after console queries
        System.out.println("\n" + "=".repeat(60));
        System.out.println("INTERFAZ GRAFICA - INICIANDO AUTOMATICAMENTE");
        System.out.println("=".repeat(60));
        System.out.println("Launching GUI automatically...");
        
        // Set map data for GUI
        setMapData(graph, idToCoord, streetNameMap, bidirectionalSearch);
        
        // Create and show GUI
        Test guiInstance = new Test();
        guiInstance.initializeGUI();
        
        System.out.println("GUI launched successfully! You can now:");
        System.out.println("1. View the complete street map of Buenos Aires");
        System.out.println("2. Zoom and pan around the map");
        System.out.println("3. Click on intersections to select origin and destination");
        System.out.println("4. Calculate and visualize routes");
        System.out.println("\nThe console will remain active for additional debug output.");
        
        // Keep the program alive for the GUI
        System.out.println("GUI is now running. Close the window to exit the program.");
    }
    
    // Custom JPanel for map visualization
    class MapPanel extends JPanel {
        
        public MapPanel() {
            setPreferredSize(new Dimension((int)mapWidth, (int)mapHeight));
            setBackground(BACKGROUND_COLOR);
            
            // Mouse listeners for pan and zoom
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    isDragging = true;
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    isDragging = false;
                }
                
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        handleMapClick(e.getX(), e.getY());
                    }
                }
            });
            
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isDragging) {
                        double deltaX = e.getX() - lastMouseX;
                        double deltaY = e.getY() - lastMouseY;
                        
                        // Move the offset directly by pixel amount (no scale division needed)
                        offsetX += deltaX;
                        offsetY += deltaY;
                        
                        lastMouseX = e.getX();
                        lastMouseY = e.getY();
                        
                        repaint();
                    }
                }
                
                @Override
                public void mouseMoved(MouseEvent e) {
                    // Show tooltip with nearest node info
                    showNodeInfo(e.getX(), e.getY());
                }
            });
            
            // Mouse wheel listener for zoom
            addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double zoomFactor = 1.15; // Slightly smoother zoom
                    double oldScale = scale;
                    
                    // Get mouse position for centered zoom
                    double mouseX = e.getX();
                    double mouseY = e.getY();
                    
                    if (e.getWheelRotation() > 0) {
                        scale /= zoomFactor;
                    } else {
                        scale *= zoomFactor;
                    }
                    
                    // Dynamic zoom limits - further increased max zoom for ultra-detailed view
                    double minScale = 50.0;      // Minimum zoom for overview
                    double maxScale = 1000000.0; // Maximum zoom for ultra-detailed view
                    scale = Math.max(minScale, Math.min(scale, maxScale));
                    
                    // Adjust offset to zoom towards mouse cursor
                    double scaleRatio = scale / oldScale;
                    offsetX = mouseX - (mouseX - offsetX) * scaleRatio;
                    offsetY = mouseY - (mouseY - offsetY) * scaleRatio;
                    
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (graphData == null || idToCoordData == null) {
                g.drawString("Loading map data...", 20, 30);
                return;
            }
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Calculate map bounds and auto-fit if not done yet
            if (minLat == 0 && maxLat == 0) {
                calculateMapBounds();
                fitMapToWindow();
            }
            
            // Draw all streets
            drawStreets(g2d);
            
            // Draw all nodes as white circles (vacant points)
            drawAllNodes(g2d);
            
            // Draw selected route
            if (!currentRoute.isEmpty()) {
                drawRoute(g2d, currentRoute);
            }
            
            // Draw selected nodes (highlighted)
            drawSelectedNodes(g2d);
            
            // Draw scale and legend
            drawLegend(g2d);
        }
        
        private void calculateMapBounds() {
            // Buenos Aires strict bounds for filtering
            double BA_MIN_LON = -58.7;
            double BA_MAX_LON = -58.3;
            double BA_MIN_LAT = -34.8;
            double BA_MAX_LAT = -34.4;
            
            minLat = BA_MAX_LAT;
            maxLat = BA_MIN_LAT;
            minLon = BA_MAX_LON;
            maxLon = BA_MIN_LON;
            
            int validCoords = 0;
            int totalCoords = 0;
            
            for (String coord : idToCoordData.values()) {
                totalCoords++;
                String[] parts = coord.split(" ");
                if (parts.length == 2) {
                    try {
                        double lon = Double.parseDouble(parts[0]);
                        double lat = Double.parseDouble(parts[1]);
                        
                        // Only include coordinates that are clearly in Buenos Aires
                        if (lon >= BA_MIN_LON && lon <= BA_MAX_LON && 
                            lat >= BA_MIN_LAT && lat <= BA_MAX_LAT) {
                            
                            minLon = Math.min(minLon, lon);
                            maxLon = Math.max(maxLon, lon);
                            minLat = Math.min(minLat, lat);
                            maxLat = Math.max(maxLat, lat);
                            validCoords++;
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid coordinates
                    }
                }
            }
            
            // If we have valid coordinates, add small padding
            if (validCoords > 0) {
                double lonRange = maxLon - minLon;
                double latRange = maxLat - minLat;
                
                // Ensure minimum range
                if (lonRange < 0.1) {
                    double center = (minLon + maxLon) / 2;
                    minLon = center - 0.05;
                    maxLon = center + 0.05;
                }
                if (latRange < 0.1) {
                    double center = (minLat + maxLat) / 2;
                    minLat = center - 0.05;
                    maxLat = center + 0.05;
                }
                
                // Add small padding
                lonRange = maxLon - minLon;
                latRange = maxLat - minLat;
                minLon -= lonRange * 0.02;
                maxLon += lonRange * 0.02;
                minLat -= latRange * 0.02;
                maxLat += latRange * 0.02;
            } else {
                // Use default Buenos Aires bounds if no valid coordinates found
                minLon = -58.6;
                maxLon = -58.3;
                minLat = -34.7;
                maxLat = -34.5;
            }
            
            System.out.println("Map bounds calculated:");
            System.out.println("Valid coordinates: " + validCoords + "/" + totalCoords);
            System.out.println("Longitude: " + minLon + " to " + maxLon);
            System.out.println("Latitude: " + minLat + " to " + maxLat);
        }
        
        private void fitMapToWindow() {
            // Calculate the scale needed to fit the entire map in the window
            double lonRange = maxLon - minLon;
            double latRange = maxLat - minLat;
            
            if (lonRange <= 0 || latRange <= 0) return; // Invalid range
            
            // Get actual panel size
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            
            // Use default size if panel not yet sized
            if (panelWidth <= 0) panelWidth = 800;
            if (panelHeight <= 0) panelHeight = 600;
            
            // Calculate scale to fit the map in the window
            // Simple approach: map the coordinate range to pixels
            double scaleX = (panelWidth * 0.9) / lonRange;  // 90% of width
            double scaleY = (panelHeight * 0.9) / latRange; // 90% of height
            
            // Use the smaller scale to ensure everything fits
            scale = Math.min(scaleX, scaleY);
            
            // Ensure scale is reasonable
            if (scale < 1000) scale = 1000;     // Minimum scale for visibility
            if (scale > 1000000) scale = 1000000; // Ultra maximum scale consistent with mouse zoom
            
            // Calculate offset to center the map
            // Map center coordinates
            double mapCenterLon = (minLon + maxLon) / 2.0;
            double mapCenterLat = (minLat + maxLat) / 2.0;
            
            // Center point in screen coordinates before scaling
            double centerX = (mapCenterLon - minLon) * scaleX;
            double centerY = (maxLat - mapCenterLat) * scaleY;
            
            // Offset to center in panel
            offsetX = (panelWidth / 2.0) - centerX;
            offsetY = (panelHeight / 2.0) - centerY;
            
            System.out.println("Auto-fit applied:");
            System.out.println("Scale: " + scale);
            System.out.println("Offset: (" + offsetX + ", " + offsetY + ")");
            System.out.println("Panel size: " + panelWidth + "x" + panelHeight);
            System.out.println("Coordinate range: lon(" + lonRange + ") lat(" + latRange + ")");
        }
        
        private void drawStreets(Graphics2D g2d) {
            // Draw all street segments with direction indicators
            for (Node node : graphData) {
                for (Edge edge : node.outEdges) {
                    Point2D.Double start = coordToScreen(idToCoordData.get(edge.from));
                    Point2D.Double end = coordToScreen(idToCoordData.get(edge.to));
                    
                    if (start != null && end != null) {
                        drawStreetSegment(g2d, start, end, edge);
                    }
                }
            }
        }
        
        private void drawStreetSegment(Graphics2D g2d, Point2D.Double start, Point2D.Double end, Edge edge) {
            // Check if this street is bidirectional by looking for reverse edge
            boolean isBidirectional = false;
            for (Edge reverseEdge : graphData[edge.to].outEdges) {
                if (reverseEdge.to == edge.from && reverseEdge.streetName.equals(edge.streetName)) {
                    isBidirectional = true;
                    break;
                }
            }
            
            // Set line style based on directionality and zoom level
            float baseThickness = Math.max(1.0f, (float)(scale / 50000)); // Scale thickness with zoom
            if (scale > 100000) {
                baseThickness = Math.max(2.0f, (float)(scale / 100000)); // Thicker lines for ultra-high zoom
            }
            
            if (isBidirectional) {
                // Bidirectional: thicker line
                g2d.setColor(STREET_COLOR);
                g2d.setStroke(new BasicStroke(baseThickness * 1.5f));
            } else {
                // Unidirectional: thinner line
                g2d.setColor(STREET_COLOR.darker());
                g2d.setStroke(new BasicStroke(baseThickness));
            }
            
            // Draw the main line
            g2d.drawLine((int)start.x, (int)start.y, (int)end.x, (int)end.y);
            
            // Draw direction arrow for unidirectional streets (only at higher zoom levels)
            if (!isBidirectional && scale > 2000) {
                drawDirectionArrow(g2d, start, end);
            }
        }
        
        private void drawDirectionArrow(Graphics2D g2d, Point2D.Double start, Point2D.Double end) {
            // Calculate arrow parameters
            double dx = end.x - start.x;
            double dy = end.y - start.y;
            double length = Math.sqrt(dx * dx + dy * dy);
            
            // Only draw arrow if line is long enough
            if (length < 20) return;
            
            // Normalize direction vector
            double unitX = dx / length;
            double unitY = dy / length;
            
            // Arrow size based on zoom level - improved for ultra-high zoom
            double arrowSize = Math.min(15, Math.max(3, scale / 10000));
            if (scale > 100000) {
                arrowSize = Math.min(25, scale / 50000); // Larger arrows for ultra-high zoom
            }
            
            // Position arrow at 70% along the line
            double arrowX = start.x + dx * 0.7;
            double arrowY = start.y + dy * 0.7;
            
            // Calculate arrow head points
            double angle = Math.PI / 6; // 30 degrees
            double arrowX1 = arrowX - arrowSize * (unitX * Math.cos(angle) - unitY * Math.sin(angle));
            double arrowY1 = arrowY - arrowSize * (unitX * Math.sin(angle) + unitY * Math.cos(angle));
            double arrowX2 = arrowX - arrowSize * (unitX * Math.cos(-angle) - unitY * Math.sin(-angle));
            double arrowY2 = arrowY - arrowSize * (unitX * Math.sin(-angle) + unitY * Math.cos(-angle));
            
            // Draw arrow head with thickness scaled to zoom level
            float arrowThickness = Math.max(0.8f, (float)(scale / 100000));
            if (scale > 200000) {
                arrowThickness = Math.max(1.5f, (float)(scale / 200000)); // Thicker arrows for ultra-high zoom
            }
            g2d.setStroke(new BasicStroke(arrowThickness));
            g2d.drawLine((int)arrowX, (int)arrowY, (int)arrowX1, (int)arrowY1);
            g2d.drawLine((int)arrowX, (int)arrowY, (int)arrowX2, (int)arrowY2);
        }
        
        private void drawRoute(Graphics2D g2d, List<Integer> route) {
            g2d.setColor(SELECTED_ROUTE_COLOR);
            
            // Scale route line thickness with zoom level
            float routeThickness = Math.max(3.0f, (float)(scale / 20000));
            if (scale > 100000) {
                routeThickness = Math.max(5.0f, (float)(scale / 50000)); // Thicker routes for ultra-high zoom
            }
            g2d.setStroke(new BasicStroke(routeThickness));
            
            for (int i = 0; i < route.size() - 1; i++) {
                Point2D.Double start = coordToScreen(idToCoordData.get(route.get(i)));
                Point2D.Double end = coordToScreen(idToCoordData.get(route.get(i + 1)));
                
                if (start != null && end != null) {
                    g2d.drawLine((int)start.x, (int)start.y, (int)end.x, (int)end.y);
                }
            }
        }
        
        private void drawSelectedNodes(Graphics2D g2d) {
            // Calculate node size based on zoom level for better visibility
            int selectedNodeSize = Math.max(8, Math.min(25, (int)(scale / 4000)));
            if (scale > 100000) {
                selectedNodeSize = Math.max(15, Math.min(40, (int)(scale / 15000))); // Larger for ultra-high zoom
            }
            
            // Draw origin node
            if (selectedOrigin != null) {
                Point2D.Double pos = coordToScreen(idToCoordData.get(selectedOrigin));
                if (pos != null) {
                    g2d.setColor(ORIGIN_COLOR);
                    g2d.fillOval((int)pos.x - selectedNodeSize/2, (int)pos.y - selectedNodeSize/2, selectedNodeSize, selectedNodeSize);
                    g2d.setColor(Color.BLACK);
                    g2d.drawOval((int)pos.x - selectedNodeSize/2, (int)pos.y - selectedNodeSize/2, selectedNodeSize, selectedNodeSize);
                    
                    // Scale text size with zoom
                    int fontSize = Math.max(10, Math.min(18, (int)(scale / 10000)));
                    g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
                    g2d.drawString("Origin", (int)pos.x + selectedNodeSize/2 + 5, (int)pos.y - selectedNodeSize/2 - 5);
                }
            }
            
            // Draw destination node
            if (selectedDestination != null) {
                Point2D.Double pos = coordToScreen(idToCoordData.get(selectedDestination));
                if (pos != null) {
                    g2d.setColor(DESTINATION_COLOR);
                    g2d.fillOval((int)pos.x - selectedNodeSize/2, (int)pos.y - selectedNodeSize/2, selectedNodeSize, selectedNodeSize);
                    g2d.setColor(Color.BLACK);
                    g2d.drawOval((int)pos.x - selectedNodeSize/2, (int)pos.y - selectedNodeSize/2, selectedNodeSize, selectedNodeSize);
                    
                    // Scale text size with zoom
                    int fontSize = Math.max(10, Math.min(18, (int)(scale / 10000)));
                    g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
                    g2d.drawString("Destination", (int)pos.x + selectedNodeSize/2 + 5, (int)pos.y - selectedNodeSize/2 - 5);
                }
            }
        }
        
        private void drawLegend(Graphics2D g2d) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            
            int x = 10;
            int y = getHeight() - 120;
            
            g2d.drawString("Zoom: " + String.format("%.0f", scale) + " (Range: 50 - 1,000,000)", x, y);
            g2d.drawString("Nodes: " + (graphData != null ? graphData.length : 0), x, y + 15);
            
            // Node visibility info
            if (scale >= 500) {
                g2d.drawString("o White circles = Available nodes", x, y + 30);
                g2d.drawString("* Green = Origin, Blue = Destination", x, y + 45);
            } else {
                g2d.drawString("Zoom in more to see individual nodes", x, y + 30);
            }
            
            g2d.drawString("Mouse: wheel=zoom, drag=pan, click=select", x, y + 60);
            
            if (selectedOrigin != null || selectedDestination != null) {
                y += 75;
                if (selectedOrigin == null) {
                    g2d.drawString("Click a node to select origin", x, y);
                } else if (selectedDestination == null) {
                    g2d.drawString("Click another node to select destination", x, y);
                } else {
                    g2d.drawString("Press 'Find Route' to calculate path", x, y);
                }
            }
        }
        
        private Point2D.Double coordToScreen(String coord) {
            if (coord == null) return null;
            
            String[] parts = coord.split(" ");
            if (parts.length != 2) return null;
            
            try {
                double lon = Double.parseDouble(parts[0]);
                double lat = Double.parseDouble(parts[1]);
                
                // Filter out coordinates outside Buenos Aires area (same bounds as calculateMapBounds)
                if (lon < -58.7 || lon > -58.3 || lat < -34.8 || lat > -34.4) {
                    return null; // Don't render coordinates outside Buenos Aires
                }
                
                // Simple coordinate transformation
                double x = (lon - minLon) * scale + offsetX;
                double y = (maxLat - lat) * scale + offsetY; // Y is inverted for screen coords
                
                return new Point2D.Double(x, y);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        private Integer screenToNodeId(double screenX, double screenY) {
            double threshold = 10.0; // pixels
            Integer nearestNode = null;
            double minDistance = threshold;
            
            for (Map.Entry<Integer, String> entry : idToCoordData.entrySet()) {
                Point2D.Double pos = coordToScreen(entry.getValue());
                if (pos != null) {
                    double distance = Math.sqrt(Math.pow(pos.x - screenX, 2) + Math.pow(pos.y - screenY, 2));
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestNode = entry.getKey();
                    }
                }
            }
            
            return nearestNode;
        }
        
        private void handleMapClick(double x, double y) {
            Integer nodeId = screenToNodeId(x, y);
            
            if (nodeId != null) {
                if (selectedOrigin == null) {
                    selectedOrigin = nodeId;
                    statusLabel.setText("Origin selected: Node " + nodeId + ". Click another node for destination.");
                } else if (selectedDestination == null && !nodeId.equals(selectedOrigin)) {
                    selectedDestination = nodeId;
                    statusLabel.setText("Destination selected: Node " + nodeId + ". Press 'Find Route' to calculate path.");
                    findRouteButton.setEnabled(true);
                } else {
                    // Reset and start over
                    selectedOrigin = nodeId;
                    selectedDestination = null;
                    currentRoute.clear();
                    statusLabel.setText("Origin selected: Node " + nodeId + ". Click another node for destination.");
                    findRouteButton.setEnabled(false);
                }
                
                repaint();
            }
        }
        
        private void showNodeInfo(double x, double y) {
            Integer nodeId = screenToNodeId(x, y);
            
            if (nodeId != null && nodeId != lastHoveredNode) {
                Node node = graphData[nodeId];
                String coord = idToCoordData.get(nodeId);
                String info = String.format("Node %d: %s\nOut edges: %d, In edges: %d", 
                                           nodeId, coord, node.outEdges.size(), node.inEdges.size());
                
                setToolTipText(info);
                lastHoveredNode = nodeId;
            } else if (nodeId == null) {
                setToolTipText(null);
                lastHoveredNode = null;
            }
        }
        
        private Integer lastHoveredNode = null;
        
        private void drawAllNodes(Graphics2D g2d) {
            // Only show nodes if we're zoomed in enough for them to be visible and useful
            if (scale < 500) {
                return; // Skip drawing nodes at low zoom levels to avoid clutter
            }
            
            // Calculate appropriate node size based on zoom level - improved for ultra-high zoom
            int nodeSize = Math.max(2, Math.min(20, (int)(scale / 5000)));
            if (scale > 100000) {
                nodeSize = Math.max(10, Math.min(30, (int)(scale / 20000))); // Larger nodes for ultra-high zoom
            }
            
            g2d.setColor(Color.WHITE);
            
            // Draw all nodes that have coordinates
            for (Integer nodeId : idToCoordData.keySet()) {
                // Skip if this node is currently selected (will be drawn highlighted later)
                if (nodeId.equals(selectedOrigin) || nodeId.equals(selectedDestination)) {
                    continue;
                }
                
                Point2D.Double pos = coordToScreen(idToCoordData.get(nodeId));
                if (pos != null) {
                    // Check if the node is visible in the current view
                    if (pos.x >= -nodeSize && pos.x <= getWidth() + nodeSize &&
                        pos.y >= -nodeSize && pos.y <= getHeight() + nodeSize) {
                        
                        // Draw white circle with black border
                        g2d.fillOval((int)pos.x - nodeSize/2, (int)pos.y - nodeSize/2, nodeSize, nodeSize);
                        g2d.setColor(Color.BLACK);
                        g2d.drawOval((int)pos.x - nodeSize/2, (int)pos.y - nodeSize/2, nodeSize, nodeSize);
                        g2d.setColor(Color.WHITE);
                    }
                }
            }
        }
    }
    
    // GUI initialization and methods
    public void initializeGUI() {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }
    
    private void createAndShowGUI() {
        mainFrame = new JFrame("Buenos Aires Street Network - Route Planner");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());
        
        // Create map panel
        mapPanel = new MapPanel();
        mapScrollPane = new JScrollPane(mapPanel);
        mapScrollPane.setPreferredSize(new Dimension(800, 600));
        mapScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mapScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Create control panel
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        // Status and buttons
        JPanel topPanel = new JPanel(new FlowLayout());
        statusLabel = new JLabel("Click on the map to select origin and destination nodes");
        findRouteButton = new JButton("Find Route");
        clearButton = new JButton("Clear Selection");
        showGuiButton = new JButton("Show Console");
        
        findRouteButton.setEnabled(false);
        
        findRouteButton.addActionListener(e -> calculateRoute());
        clearButton.addActionListener(e -> clearSelection());
        showGuiButton.addActionListener(e -> toggleConsole());
        
        topPanel.add(statusLabel);
        topPanel.add(findRouteButton);
        topPanel.add(clearButton);
        topPanel.add(showGuiButton);
        
        // Info area
        infoArea = new JTextArea(8, 40);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane infoScrollPane = new JScrollPane(infoArea);
        infoScrollPane.setBorder(BorderFactory.createTitledBorder("Route Information"));
        
        controlPanel.add(topPanel, BorderLayout.NORTH);
        controlPanel.add(infoScrollPane, BorderLayout.CENTER);
        
        // Add components to main frame
        mainFrame.add(mapScrollPane, BorderLayout.CENTER);
        mainFrame.add(controlPanel, BorderLayout.SOUTH);
        
        // Set up the frame
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Open in fullscreen
        mainFrame.setSize(1200, 800); // Fallback size if maximized doesn't work
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
        
        // Initial info
        updateInfoArea("GUI initialized. Map data loaded with " + 
                      (graphData != null ? graphData.length : 0) + " nodes.\n" +
                      "Click on the map to select origin and destination for route planning.");
    }
    
    private void calculateRoute() {
        if (selectedOrigin == null || selectedDestination == null) {
            updateInfoArea("Please select both origin and destination nodes.");
            return;
        }
        
        updateInfoArea("Calculating route from Node " + selectedOrigin + " to Node " + selectedDestination + "...");
        
        // Reset distance data for new query
        for (Node node : graphData) {
            node.distance = new Distance();
        }
        
        // Calculate route using bidirectional search
        BidirectionalSearch.PathResult result = bidirectionalSearchData.computeShortestPath(
            selectedOrigin, selectedDestination, (int)(System.currentTimeMillis() % 1000));
        
        if (result.distance == -1) {
            updateInfoArea("No route found between the selected nodes.\n" +
                          "This could be due to:\n" +
                          "- Nodes are in different connected components\n" +
                          "- Street directions prevent connection\n" +
                          "- Graph preprocessing issue");
            currentRoute.clear();
        } else {
            // Reconstruct path
            currentRoute = bidirectionalSearchData.reconstructPath(
                selectedOrigin, selectedDestination, result.meetingNode);
            
            StringBuilder routeInfo = new StringBuilder();
            routeInfo.append("Route found!\n");
            routeInfo.append("Distance: ").append(result.distance).append(" meters\n");
            routeInfo.append("Number of segments: ").append(currentRoute.size() - 1).append("\n\n");
            
            if (currentRoute.size() > 1) {
                routeInfo.append("Route details:\n");
                for (int i = 0; i < Math.min(currentRoute.size() - 1, 10); i++) {
                    int from = currentRoute.get(i);
                    int to = currentRoute.get(i + 1);
                    
                    // Find street name
                    String streetName = "Unknown";
                    for (Edge edge : graphData[from].outEdges) {
                        if (edge.to == to) {
                            streetName = edge.streetName;
                            break;
                        }
                    }
                    
                    routeInfo.append(String.format("%d. Node %d -> %d via %s\n", 
                                                  i + 1, from, to, streetName));
                }
                
                if (currentRoute.size() > 11) {
                    routeInfo.append("... and ").append(currentRoute.size() - 11).append(" more segments\n");
                }
                
                routeInfo.append("\nOrigin: ").append(idToCoordData.get(selectedOrigin)).append("\n");
                routeInfo.append("Destination: ").append(idToCoordData.get(selectedDestination));
            }
            
            updateInfoArea(routeInfo.toString());
        }
        
        mapPanel.repaint();
    }
    
    private void clearSelection() {
        selectedOrigin = null;
        selectedDestination = null;
        currentRoute.clear();
        findRouteButton.setEnabled(false);
        statusLabel.setText("Selection cleared. Click on the map to select origin and destination nodes.");
        updateInfoArea("Selection cleared. Ready for new route planning.");
        mapPanel.repaint();
    }
    
    private void toggleConsole() {
        // This could be extended to show/hide a console window for debug output
        JOptionPane.showMessageDialog(mainFrame, 
            "Console output is available in the terminal where the application was started.\n" +
            "All preprocessing information and debug data is printed there.",
            "Console Information", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void updateInfoArea(String text) {
        SwingUtilities.invokeLater(() -> {
            infoArea.setText(text);
            infoArea.setCaretPosition(0);
        });
    }
    
    // Method to set the static data from main method
    public static void setMapData(Node[] graph, Map<Integer, String> idToCoord, 
                                 Map<String, String> streetNameMap, 
                                 BidirectionalSearch bidirectionalSearch) {
        graphData = graph;
        idToCoordData = idToCoord;
        streetNameMapData = streetNameMap;
        bidirectionalSearchData = bidirectionalSearch;
    }
}