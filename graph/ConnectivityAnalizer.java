package graph;

import java.util.List;
import java.util.Map;

import algorithms.BidirectionalSearch;
import models.*;

public class ConnectivityAnalizer {
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
}
