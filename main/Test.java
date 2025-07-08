package main;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

// Swing imports for GUI
import javax.swing.*;
import java.awt.*;
import models.*;
import algorithms.*;
import graph.*;
import loader.*;
import gui.*;

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
    public static void main(String[] args) throws IOException {       
        MapDataResult result = CSVRouteLoader.loadFromCSV("main/rutas.csv");
        int n = result.nodeIndex.size();
        System.out.println("Number of nodes: " + n);
        System.out.println("Number of routes: " + result.routes.size());
        
        // Initialize graph
        Node[] graph = new Node[n];
        for (int i = 0; i < n; i++) {
            graph[i] = new Node(i);
        }
        
        // Add edges to graph considerando direccionalidad
        int totalEdges = 0;
        for (Route route : result.routes) {
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
        for (Route route : result.routes) {
            if (route.isBidirectional) {
                bidirectionalCount++;
            } else {
                unidirectionalCount++;
            }
        }

        System.out.println("Bidirectional routes: " + bidirectionalCount);
        System.out.println("Unidirectional routes: " + unidirectionalCount);
        System.out.println("Percentage bidirectional: " + (bidirectionalCount * 100.0 / result.routes.size()) + "%");
                
        // Crear mapa de aristas para búsqueda rápida
        Map<String, String> streetNameMap = new HashMap<>();
        for (Route route : result.routes) {
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
        for (Map.Entry<Integer, String> entry : result.idToCoord.entrySet()) {
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
        
        if (result.idToCoord.containsKey(0) && result.idToCoord.containsKey(9)) {
            RouteDisplayer.showDetailedRoute(graph, result.idToCoord, streetNameMap, 0, 9, node0Name, node9Name);
            
            // ANALISIS PROFUNDO: Conectividad local de los nodos
            ConnectivityAnalizer.analyzeLocalConnectivity(graph, result.idToCoord, streetNameMap, 0, node0Name);
            ConnectivityAnalizer.analyzeLocalConnectivity(graph, result.idToCoord, streetNameMap, 9, node9Name);
            
            // BUSCAR RUTAS QUE SI FUNCIONEN para verificar que el sistema esta bien
            ConnectivityAnalizer.findConnectedPairs(graph, result.idToCoord, streetNameMap, 3);
            
        } else {
            System.out.println("ERROR: Los nodos 0 o 9 no existen en el dataset");
            System.out.println("Nodos disponibles: 0 a " + (n - 1));
            if (result.idToCoord.containsKey(0)) {
                System.out.println("Nodo 0 existe: " + result.idToCoord.get(0));
            }
            if (result.idToCoord.containsKey(9)) {
                System.out.println("Nodo 9 existe: " + result.idToCoord.get(9));
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
            int source = GraphUtils.findNearestNode(sourceCoord, result.nodeIndex, result.idToCoord);
            int target = GraphUtils.findNearestNode(targetCoord, result.nodeIndex, result.idToCoord);
            
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
            
            BidirectionalSearch.PathResult resul = bidirectionalSearch.computeShortestPath(source, target, i);
            
            if (resul.distance == -1) {
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
                System.out.println("Shortest distance: " + resul.distance + " meters");
                
                // Reconstruir y mostrar la ruta
                List<Integer> path = bidirectionalSearch.reconstructPath(source, target, resul.meetingNode);
                
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
                    System.out.println("\nActual origin coordinates: " + result.idToCoord.get(source));
                    System.out.println("Actual destination coordinates: " + result.idToCoord.get(target));
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
        setMapData(graph, result.idToCoord, streetNameMap, bidirectionalSearch);
        
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

        // === Instancia de componentes ===
        statusLabel = new JLabel("Click on the map to select origin and destination nodes");
        findRouteButton = new JButton("Find Route");
        clearButton = new JButton("Clear Selection");
        showGuiButton = new JButton("Show Console");

        mapPanel = new MapPanel();
        mapPanel.statusLabel = statusLabel;
        mapPanel.findRouteButton = findRouteButton;

        // Si ya tienes cargado el grafo, pásalo al panel
        mapPanel.graphData = this.graphData;
        mapPanel.idToCoordData = this.idToCoordData;

        // Scroll para el mapa
        mapScrollPane = new JScrollPane(mapPanel);
        mapScrollPane.setPreferredSize(new Dimension(800, 600));
        mapScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mapScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Panel de control (abajo)
        JPanel controlPanel = new JPanel(new BorderLayout());

        // Panel superior con botones
        JPanel topPanel = new JPanel(new FlowLayout());
        findRouteButton.setEnabled(false);

        findRouteButton.addActionListener(e -> calculateRoute());
        clearButton.addActionListener(e -> clearSelection());
        showGuiButton.addActionListener(e -> toggleConsole());

        topPanel.add(statusLabel);
        topPanel.add(findRouteButton);
        topPanel.add(clearButton);
        topPanel.add(showGuiButton);

        // Área de información
        infoArea = new JTextArea(8, 40);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane infoScrollPane = new JScrollPane(infoArea);
        infoScrollPane.setBorder(BorderFactory.createTitledBorder("Route Information"));

        // Armar panel de control
        controlPanel.add(topPanel, BorderLayout.NORTH);
        controlPanel.add(infoScrollPane, BorderLayout.CENTER);

        // Agregar todo al frame
        mainFrame.add(mapScrollPane, BorderLayout.CENTER);
        mainFrame.add(controlPanel, BorderLayout.SOUTH);

        // Configuración visual del frame
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        mainFrame.setSize(1200, 800);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);

        // Mensaje inicial
        updateInfoArea("GUI initialized. Map data loaded with " +
                (graphData != null ? graphData.length : 0) + " nodes.\n" +
                "Click on the map to select origin and destination for route planning.");
    }

    
    private void calculateRoute() {
        System.out.println("HOLA ");
        if (mapPanel.selectedOrigin == null || mapPanel.selectedDestination == null) {
            updateInfoArea("Please select both origin and destination nodes.");
            return;
        }
        System.out.println("HOLA1  ");
        updateInfoArea("Calculating route from Node " + mapPanel.selectedOrigin + " to Node " + mapPanel.selectedDestination + "...");

        for (Node node : mapPanel.graphData) {
            node.distance = new Distance();
        }
        System.out.println("HOLA2");
        // Ejecutar búsqueda bidireccional
        BidirectionalSearch.PathResult result = bidirectionalSearchData.computeShortestPath(
            mapPanel.selectedOrigin, mapPanel.selectedDestination, (int)(System.currentTimeMillis() % 1000)
        );

        if (result.distance == -1) {
            updateInfoArea("No route found between the selected nodes.\n" +
                "- Nodes are in different connected components\n" +
                "- Street directions prevent connection\n" +
                "- Graph preprocessing issue");
            mapPanel.currentRoute.clear();
        } else {
            mapPanel.currentRoute.clear();
            mapPanel.currentRoute.addAll(bidirectionalSearchData.reconstructPath(
                mapPanel.selectedOrigin, mapPanel.selectedDestination, result.meetingNode));

            // Mostrar detalles
            StringBuilder routeInfo = new StringBuilder();
            routeInfo.append("Route found!\n");
            routeInfo.append("Distance: ").append(result.distance).append(" meters\n");
            routeInfo.append("Number of segments: ").append(mapPanel.currentRoute.size() - 1).append("\n\n");

            if (mapPanel.currentRoute.size() > 1) {
                routeInfo.append("Route details:\n");
                for (int i = 0; i < Math.min(mapPanel.currentRoute.size() - 1, 10); i++) {
                    int from = mapPanel.currentRoute.get(i);
                    int to = mapPanel.currentRoute.get(i + 1);

                    String streetName = "Unknown";
                    for (Edge edge : mapPanel.graphData[from].outEdges) {
                        if (edge.to == to) {
                            streetName = edge.streetName;
                            break;
                        }
                    }

                    routeInfo.append(String.format("%d. Node %d -> %d via %s\n",
                        i + 1, from, to, streetName));
                }

                if (mapPanel.currentRoute.size() > 11) {
                    routeInfo.append("... and ").append(mapPanel.currentRoute.size() - 11).append(" more segments\n");
                }

                routeInfo.append("\nOrigin: ").append(mapPanel.idToCoordData.get(mapPanel.selectedOrigin)).append("\n");
                routeInfo.append("Destination: ").append(mapPanel.idToCoordData.get(mapPanel.selectedDestination));
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