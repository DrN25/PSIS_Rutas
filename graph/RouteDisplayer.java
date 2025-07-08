package graph;

import java.util.List;
import java.util.Map;

import algorithms.BidirectionalSearch;
import models.*;

public class RouteDisplayer {
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
}
