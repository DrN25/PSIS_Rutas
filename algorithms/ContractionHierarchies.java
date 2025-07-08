package algorithms;

import java.util.*;
import models.*;

public class ContractionHierarchies {
    public Node[] graph;
    
    // OPTIMIZACIÓN SEGURA 1: Tracking de nodos en cola sin queue.remove()
    private boolean[] inQueue;
    private long[] lastImportanceUpdate;
    
    public ContractionHierarchies(Node[] graph) {
        this.graph = graph;
        this.inQueue = new boolean[graph.length];
        this.lastImportanceUpdate = new long[graph.length];
    }
    
    // Main method to perform the contraction
    public void preprocess() {
        PriorityQueue<Node> queue = new PriorityQueue<>(graph.length, new ImportanceComparator());
        
        // Initial importance calculation
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < graph.length; i++) {
            graph[i].computeImportance();
            queue.add(graph[i]);
            inQueue[i] = true;
            lastImportanceUpdate[i] = startTime;
        }
        
        int level = 0;
        int totalNodes = graph.length;
        long lastReportTime = System.currentTimeMillis();
        
        // Process nodes in order of importance
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            inQueue[node.id] = false;
            
            // OPTIMIZACIÓN SEGURA: Solo recalcular importancia ocasionalmente
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastImportanceUpdate[node.id] > 50) { // Cada 50ms
                long oldImportance = node.importance;
                node.computeImportance();
                lastImportanceUpdate[node.id] = currentTime;
                
                // Si la importancia cambió significativamente, reinsertarlo SOLO si hay otros nodos
                if (!queue.isEmpty() && Math.abs(node.importance - oldImportance) > oldImportance * 0.05) {
                    if (node.importance > queue.peek().importance) {
                        queue.add(node);
                        inQueue[node.id] = true;
                        continue;
                    }
                }
            }
            
            // Contract the node
            contractNode(node);
            node.contracted = true;
            node.level = level++;
            
            // Reportar progreso cada 10% o cada 5 segundos
            if (level % Math.max(1, totalNodes / 20) == 0 || (currentTime - lastReportTime) > 5000) {
                double percentage = (level * 100.0) / totalNodes;
                long elapsedTime = (currentTime - startTime) / 1000;
                System.out.printf("Progress: %.1f%% (%d/%d nodes contracted) - %d seconds elapsed\n", 
                                percentage, level, totalNodes, elapsedTime);
                lastReportTime = currentTime;
            }
            
            // OPTIMIZACIÓN SEGURA: Update neighbors sin queue.remove()
            updateNeighborsImportanceOptimized(node, queue);
        }
        
        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        System.out.printf("Preprocessing completed in %d seconds\n", totalTime);
    }
    
    private void contractNode(Node node) {
        // OPTIMIZACIÓN SEGURA 3: Crear copias para evitar ConcurrentModificationException
        List<Edge> inEdgesCopy = new ArrayList<>(node.inEdges);
        List<Edge> outEdgesCopy = new ArrayList<>(node.outEdges);
        
        // OPTIMIZACIÓN ADICIONAL: Límite dinámico de shortcuts
        int nodeConnectivity = inEdgesCopy.size() + outEdgesCopy.size();
        int maxShortcuts = Math.min(100, Math.max(10, 150 - nodeConnectivity * 3)); // Menos shortcuts para nodos muy conectados
        int shortcutCount = 0;
        
        // For each pair of incoming and outgoing edges
        for (Edge inEdge : inEdgesCopy) {
            if (graph[inEdge.from].contracted || shortcutCount >= maxShortcuts) continue;
            
            for (Edge outEdge : outEdgesCopy) {
                if (graph[outEdge.to].contracted || inEdge.from == outEdge.to || shortcutCount >= maxShortcuts) continue;
                
                // Check if this is the shortest path or if there's a witness path
                long directDist = inEdge.weight + outEdge.weight;
                if (isShortestPathOptimized(inEdge.from, outEdge.to, directDist, node.id)) {
                    // Create shortcut with combined street name
                    String combinedStreet = inEdge.streetName + " -> " + outEdge.streetName;
                    Edge shortcut = new Edge(inEdge.from, outEdge.to, directDist, combinedStreet);
                    graph[inEdge.from].outEdges.add(shortcut);
                    graph[outEdge.to].inEdges.add(shortcut);
                    shortcutCount++;
                }
            }
        }
    }
    
    // OPTIMIZACIÓN SEGURA 2: Usar arrays en lugar de HashMap
    private boolean isShortestPathOptimized(int from, int to, long shortcutDist, int viaNode) {
        // Usar arrays en lugar de HashMap para mejor performance
        long[] distances = new long[graph.length];
        Arrays.fill(distances, Long.MAX_VALUE);
        
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingLong(nd -> nd.distance));
        distances[from] = 0L;
        pq.add(new NodeDistance(from, 0));
        
        // OPTIMIZACIÓN ADICIONAL: Límites dinámicos basados en conectividad
        int nodeConnectivity = graph[viaNode].inEdges.size() + graph[viaNode].outEdges.size();
        long maxHops = Math.min(12, Math.max(3, 15 - nodeConnectivity / 10)); // Menos hops para nodos muy conectados
        int maxNodesExplored = Math.min(200, Math.max(20, 300 - nodeConnectivity * 5)); // Menos exploración para nodos complejos
        
        int hopCount = 0;
        int nodesExplored = 0;
        
        while (!pq.isEmpty() && hopCount < maxHops && nodesExplored < maxNodesExplored) {
            NodeDistance current = pq.poll();
            nodesExplored++;
            
            if (current.nodeId == to) {
                return current.distance >= shortcutDist; // If true, shortcut is needed
            }
            
            if (current.distance > distances[current.nodeId]) {
                continue;
            }
            
            // OPTIMIZACIÓN: Early termination si ya es más largo que el shortcut
            if (current.distance >= shortcutDist) {
                continue;
            }
            
            // OPTIMIZACIÓN SEGURA 3: Usar copia para evitar modificación concurrente
            List<Edge> outEdgesCopy = new ArrayList<>(graph[current.nodeId].outEdges);
            for (Edge edge : outEdgesCopy) {
                // Skip edges to the node being contracted
                if (edge.to == viaNode || graph[edge.to].contracted) continue;
                
                long newDist = current.distance + edge.weight;
                if (newDist < distances[edge.to] && newDist < shortcutDist) {
                    distances[edge.to] = newDist;
                    pq.add(new NodeDistance(edge.to, newDist));
                }
            }
            
            hopCount++;
        }
        
        return true; // Couldn't find a witness path, shortcut needed
    }
            
    // OPTIMIZACION SEGURA 1: Eliminar queue.remove() - LA MAS IMPORTANTE
    private void updateNeighborsImportanceOptimized(Node node, PriorityQueue<Node> queue) {
        // Usar Set para evitar duplicados y procesamiento múltiple
        Set<Integer> neighborsToUpdate = new HashSet<>();
        
        // OPTIMIZACIÓN SEGURA 3: Crear copias para evitar problemas de concurrencia
        List<Edge> inEdgesCopy = new ArrayList<>(node.inEdges);
        List<Edge> outEdgesCopy = new ArrayList<>(node.outEdges);
        
        // Recopilar vecinos únicos
        for (Edge edge : inEdgesCopy) {
            if (!graph[edge.from].contracted) {
                neighborsToUpdate.add(edge.from);
            }
        }
        
        for (Edge edge : outEdgesCopy) {
            if (!graph[edge.to].contracted) {
                neighborsToUpdate.add(edge.to);
            }
        }
        
        // CRITICO: Actualizar cada vecino solo una vez y sin queue.remove()
        for (int neighborId : neighborsToUpdate) {
            Node neighbor = graph[neighborId];
            neighbor.contractedNeighbors++;
            
            // Solo agregar a la cola si no está ya presente
            if (!inQueue[neighborId]) {
                neighbor.computeImportance();
                queue.add(neighbor);
                inQueue[neighborId] = true;
                lastImportanceUpdate[neighborId] = System.currentTimeMillis();
            }
        }
    }
            
    // Helper class for Dijkstra
    private static class NodeDistance {
        int nodeId;
        long distance;
        
        public NodeDistance(int nodeId, long distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }
}