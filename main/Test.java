package main;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    private JComboBox<VehicleProfile> profileSelector;
    private JLabel profileLabel;
    private JComboBox<Algorithm> algorithmSelector;
    private JLabel algorithmLabel;
    
    // Map data and state
    private static Node[] graphData;
    private static Map<Integer, String> idToCoordData;
    private static Map<String, String> streetNameMapData;
    private static BidirectionalSearch bidirectionalSearchData;
    private static ContractionHierarchies chInstance;
    
    private Integer selectedOrigin = null;
    private Integer selectedDestination = null;
    private List<Integer> currentRoute = new ArrayList<>();
    public static void main(String[] args) throws IOException {       
        MapDataResult result = CSVRouteLoader.loadFromCSV("main/rutas.csv");
        int n = result.nodeIndex.size();
        // System.out.println("Number of nodes: " + n);
        // System.out.println("Number of routes: " + result.routes.size());
        
        // Initialize graph
        Node[] graph = new Node[n];
        for (int i = 0; i < n; i++) {
            graph[i] = new Node(i);
        }
        
        // Add edges to graph considerando direccionalidad
        int totalEdges = 0;
        for (Route route : result.routes) {
            // Siempre añadir la arista en la dirección original, incluyendo campos CSV
            Edge forwardEdge = new Edge(route.origin, route.destination, route.cost, route.street,
                                      route.sentido, route.tipoC, route.redJerarq, route.bicisenda);
            graph[route.origin].outEdges.add(forwardEdge);
            graph[route.destination].inEdges.add(forwardEdge);
            totalEdges++;
            
            // Si es bidireccional, añadir también la arista inversa
            if (route.isBidirectional) {
                Edge backwardEdge = new Edge(route.destination, route.origin, route.cost, route.street,
                                           route.sentido, route.tipoC, route.redJerarq, route.bicisenda);
                graph[route.destination].outEdges.add(backwardEdge);
                graph[route.origin].inEdges.add(backwardEdge);
                totalEdges++;
            }
        }
            
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
        chInstance = ch; // Store the CH instance for GUI use
        
        // Initialize with default profile
        chInstance.setProfile(VehicleProfile.VEHICULOS);
        
        long preprocessingTime = System.currentTimeMillis() - preprocessingStartTime;
        System.out.println("Preprocessing complete!");
        System.out.println("Total preprocessing time: " + (preprocessingTime / 1000.0) + " seconds");
        
        // Análisis de conectividad del grafo
        // System.out.println("Analyzing graph connectivity...");
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
        
        // Buscar información sobre los nodos específicos
        String node0Name = "CANTILO, INT.";
        String node9Name = "LA CACHILA";
        
        if (result.idToCoord.containsKey(0) && result.idToCoord.containsKey(9)) {
            // ANALISIS PROFUNDO: Conectividad local de los nodos
            ConnectivityAnalizer.analyzeLocalConnectivity(graph, result.idToCoord, 0, node0Name);
            ConnectivityAnalizer.analyzeLocalConnectivity(graph, result.idToCoord, 9, node9Name);
            
            // BUSCAR RUTAS QUE SI FUNCIONEN para verificar que el sistema esta bien
            ConnectivityAnalizer.findConnectedPairs(graph, result.idToCoord, 3);
            
        } else {
            // System.out.println("ERROR: Los nodos 0 o 9 no existen en el dataset");
            // System.out.println("Nodos disponibles: 0 a " + (n - 1));
            if (result.idToCoord.containsKey(0)) {
                // System.out.println("Nodo 0 existe: " + result.idToCoord.get(0));
            }
            if (result.idToCoord.containsKey(9)) {
                // System.out.println("Nodo 9 existe: " + result.idToCoord.get(9));
            }
        }
        
        // Query phase - input manual del usuario
        BidirectionalSearch bidirectionalSearch = new BidirectionalSearch(graph);
        
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
        
        // Profile selector
        profileLabel = new JLabel("Profile:");
        profileSelector = new JComboBox<>(VehicleProfile.values());
        profileSelector.setSelectedItem(VehicleProfile.VEHICULOS);
        profileSelector.addActionListener(e -> onProfileChanged());
        
        // Algorithm selector
        algorithmLabel = new JLabel("Algorithm:");
        algorithmSelector = new JComboBox<>(Algorithm.values());
        algorithmSelector.setSelectedItem(Algorithm.CCH);
        algorithmSelector.addActionListener(e -> onAlgorithmChanged());

        mapPanel = new MapPanel();
        mapPanel.statusLabel = statusLabel;
        mapPanel.findRouteButton = findRouteButton;

        // Si ya tienes cargado el grafo, pásalo al panel
        mapPanel.graphData = Test.graphData;
        mapPanel.idToCoordData = Test.idToCoordData;

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

        // Cambiar el orden: primero algoritmo, luego perfil
        topPanel.add(algorithmLabel);
        topPanel.add(algorithmSelector);
        topPanel.add(profileLabel);
        topPanel.add(profileSelector);
        topPanel.add(statusLabel);
        topPanel.add(findRouteButton);
        topPanel.add(clearButton);
        topPanel.add(showGuiButton);

        // Area de informacion
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
        if (mapPanel.selectedOrigin == null || mapPanel.selectedDestination == null) {
            updateInfoArea("Please select both origin and destination nodes.");
            return;
        }

        Algorithm selectedAlgorithm = (Algorithm) algorithmSelector.getSelectedItem();
        updateInfoArea("Calculating route using " + selectedAlgorithm + " from Node " +
                    mapPanel.selectedOrigin + " to Node " + mapPanel.selectedDestination + "...");

        for (Node node : mapPanel.graphData) {
            node.distance = new Distance();
        }

        List<Integer> path = null;
        long distance = -1;
        long durationMs = -1;

        try {
            long startTime = System.nanoTime(); // ⏱ INICIO

            switch (selectedAlgorithm) {
                case CCH:
                    BidirectionalSearch.PathResult chResult = bidirectionalSearchData.computeShortestPathEnhanced(
                        mapPanel.selectedOrigin, mapPanel.selectedDestination, (int)(System.currentTimeMillis() % 1000)
                    );
                    if (chResult.distance != -1) {
                        path = bidirectionalSearchData.reconstructPath(
                            mapPanel.selectedOrigin, mapPanel.selectedDestination, chResult.meetingNode);
                        distance = chResult.distance;
                    }
                    break;

                case ASTAR:
                    AStarSearch aStar = new AStarSearch(mapPanel.graphData, mapPanel.idToCoordData);
                    AStarSearch.Result aStarResult = aStar.compute(mapPanel.selectedOrigin, mapPanel.selectedDestination);
                    path = aStarResult.path;
                    distance = aStarResult.distance;
                    break;

                case ALT:
                    List<Integer> landmarks = Arrays.asList(0, Math.min(10, mapPanel.graphData.length-1),
                                                        Math.min(50, mapPanel.graphData.length-1));
                    ALTSearch alt = new ALTSearch(mapPanel.graphData, mapPanel.idToCoordData, landmarks);
                    ALTSearch.Result altResult = alt.compute(mapPanel.selectedOrigin, mapPanel.selectedDestination);
                    path = altResult.path;
                    distance = altResult.distance;
                    break;
            }

            long endTime = System.nanoTime(); // ⏱ FIN
            durationMs = (endTime - startTime) / 1_000_000;

        } catch (Exception e) {
            updateInfoArea("Error calculating route with " + selectedAlgorithm + ": " + e.getMessage());
            return;
        }

        if (path == null || path.isEmpty() || distance == -1) {
            updateInfoArea("No route found between the selected nodes using " + selectedAlgorithm + ".\n" +
                "- Nodes are in different connected components\n" +
                "- Street directions prevent connection\n" +
                "- Algorithm-specific limitations");
            mapPanel.currentRoute.clear();
        } else {
            mapPanel.currentRoute.clear();
            mapPanel.currentRoute.addAll(path);

            // Tiempo estimado según perfil
            VehicleProfile selectedProfile = (VehicleProfile) profileSelector.getSelectedItem();
            double speedMps;
            switch (selectedProfile) {
                case BICICLETA: speedMps = 4.16; break;
                case PEATONAL:  speedMps = 1.39; break;
                case VEHICULOS:
                default:        speedMps = 8.33; break;
            }

            double timeSeconds = (distance > 0 && speedMps > 0) ? (distance / speedMps) : 0;
            int minutes = (int) (timeSeconds / 60);
            int seconds = (int) (timeSeconds % 60);

            // Mostrar detalles
            StringBuilder routeInfo = new StringBuilder();
            routeInfo.append("Route found using ").append(selectedAlgorithm).append("!\n");
            routeInfo.append("Time taken: ").append(durationMs).append(" ms\n");
            routeInfo.append("Distance: ").append(distance).append(" meters\n");
            routeInfo.append("Estimated time: ").append(minutes).append(" min ").append(seconds).append(" sec\n");
            routeInfo.append("Number of segments: ").append(path.size() - 1).append("\n\n");

            if (path.size() > 1) {
                routeInfo.append("Route details:\n");
                for (int i = 0; i < path.size() - 1; i++) {  // ← recorremos todo el path
                    int from = path.get(i);
                    int to = path.get(i + 1);

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
        // Limpiar selección y camino en el MapPanel
        if (mapPanel != null) {
            mapPanel.selectedOrigin = null;
            mapPanel.selectedDestination = null;
            if (mapPanel.currentRoute != null) mapPanel.currentRoute.clear();
            mapPanel.repaint();
        }
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
    
    private void onProfileChanged() {
        VehicleProfile selectedProfile = (VehicleProfile) profileSelector.getSelectedItem();
        if (selectedProfile != null && chInstance != null) {
            System.out.println("Profile changing to: " + selectedProfile);
            chInstance.setProfile(selectedProfile);
            
            // Update map panel profile
            if (mapPanel != null) {
                mapPanel.currentProfile = selectedProfile;
                mapPanel.currentRoute.clear();
                mapPanel.repaint();
            }
            
            updateInfoArea("Profile changed to: " + selectedProfile.toString() + "\n" +
                "Map visualization updated. Find a new route to see profile-specific routing.\n" +
                "IMPORTANT: Prohibited routes (red) will now be avoided in routing calculations.");
        }
    }
    
    private void onAlgorithmChanged() {
        Algorithm selectedAlgorithm = (Algorithm) algorithmSelector.getSelectedItem();
        if (selectedAlgorithm != null) {
            System.out.println("Algorithm changing to: " + selectedAlgorithm);
            
            // Update map panel algorithm
            if (mapPanel != null) {
                mapPanel.currentAlgorithm = selectedAlgorithm;
                mapPanel.currentRoute.clear();
                mapPanel.repaint();
            }
            
            updateInfoArea("Algorithm changed to: " + selectedAlgorithm.toString() + "\n" +
                "Select new route points and click 'Find Route' to use the selected algorithm.");
        }
    }
}