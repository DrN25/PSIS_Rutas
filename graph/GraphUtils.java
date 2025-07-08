package graph;

import java.util.Arrays;
import java.util.Map;
import java.util.PriorityQueue;

import models.Edge;
import models.Node;

public class GraphUtils {
  // Método para encontrar el nodo más cercano a una coordenada dada
    public static int findNearestNode(String targetCoord, Map<String, Integer> nodeIndex, Map<Integer, String> idToCoord) {
        // Buscar coincidencia exacta primero
        if (nodeIndex.containsKey(targetCoord)) {
            return nodeIndex.get(targetCoord);
        }
        
        // Si no hay coincidencia exacta, buscar por proximidad
        String[] targetParts = targetCoord.split(" ");
        if (targetParts.length != 2) return -1;
        
        try {
            double targetLon = Double.parseDouble(targetParts[0]);
            double targetLat = Double.parseDouble(targetParts[1]);
            
            int nearestNode = -1;
            double minDistance = Double.MAX_VALUE;
            
            for (Map.Entry<Integer, String> entry : idToCoord.entrySet()) {
                String[] coordParts = entry.getValue().split(" ");
                if (coordParts.length == 2) {
                    double lon = Double.parseDouble(coordParts[0]);
                    double lat = Double.parseDouble(coordParts[1]);
                    
                    // Calcular distancia euclidiana simple
                    double distance = Math.sqrt(Math.pow(lon - targetLon, 2) + Math.pow(lat - targetLat, 2));
                    
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestNode = entry.getKey();
                    }
                }
            }
            
            return nearestNode;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    // Método para análisis de conectividad - VERSIÓN ITERATIVA (evita StackOverflow)
    public static void dfsComponent(Node[] graph, int nodeId, boolean[] visited, 
                                    Map<Integer, Integer> componentMap, int componentId) {
        // Usar stack iterativo en lugar de recursión para evitar StackOverflow
        java.util.Stack<Integer> stack = new java.util.Stack<>();
        stack.push(nodeId);
        
        while (!stack.isEmpty()) {
            int currentNode = stack.pop();
            
            if (visited[currentNode]) continue;
            
            visited[currentNode] = true;
            componentMap.put(currentNode, componentId);
            
            // Agregar todos los vecinos no visitados al stack
            for (Edge edge : graph[currentNode].outEdges) {
                if (!visited[edge.to]) {
                    stack.push(edge.to);
                }
            }
            
            for (Edge edge : graph[currentNode].inEdges) {
                if (!visited[edge.from]) {
                    stack.push(edge.from);
                }
            }
        }
    }
    
    // Método de fallback: Dijkstra simple para verificar conectividad real
    public static long simpleDijkstra(Node[] graph, int source, int target) {
        long[] distances = new long[graph.length];
        Arrays.fill(distances, Long.MAX_VALUE);
        boolean[] visited = new boolean[graph.length];
        
        PriorityQueue<Integer> pq = new PriorityQueue<>((a, b) -> Long.compare(distances[a], distances[b]));
        distances[source] = 0;
        pq.add(source);
        
        int maxIterations = 10000; // Límite para evitar búsquedas infinitas
        int iterations = 0;
        
        while (!pq.isEmpty() && iterations < maxIterations) {
            int current = pq.poll();
            iterations++;
            
            if (current == target) {
                return distances[target];
            }
            
            if (visited[current]) continue;
            visited[current] = true;
            
            // Explorar aristas salientes
            for (Edge edge : graph[current].outEdges) {
                if (!visited[edge.to]) {
                    long newDist = distances[current] + edge.weight;
                    if (newDist < distances[edge.to]) {
                        distances[edge.to] = newDist;
                        pq.add(edge.to);
                    }
                }
            }
        }
        
        return distances[target] == Long.MAX_VALUE ? -1 : distances[target];
    }
}
