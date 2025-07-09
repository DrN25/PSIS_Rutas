package algorithms;

import java.util.*;
import models.*;

public class BidirectionalSearch {
    public Node[] graph;
    
    public BidirectionalSearch(Node[] graph) {
        this.graph = graph;
    }
    
    // Nueva clase para devolver tanto la distancia como el punto de encuentro
    public class PathResult {
        public long distance;
        public int meetingNode;
        
        PathResult(long distance, int meetingNode) {
            this.distance = distance;
            this.meetingNode = meetingNode;
        }
    }
    
    public PathResult computeShortestPath(int source, int target, int queryId) {
        if (source == target) return new PathResult(0, source);
        
        // Special case: if both nodes are at level 0, we may need different strategy
        boolean bothLowLevel = (graph[source].level == 0 && graph[target].level == 0);
        
        // Inicializar colas de prioridad para búsqueda forward y backward
        PriorityQueue<Node> forwardQueue = new PriorityQueue<>(new ForwardDistanceComparator());
        PriorityQueue<Node> backwardQueue = new PriorityQueue<>(new BackwardDistanceComparator());
        
        // Inicializar distancias
        graph[source].distance.forwardDist = 0;
        graph[source].distance.forwardQueryId = queryId;
        graph[target].distance.backwardDist = 0;
        graph[target].distance.backwardQueryId = queryId;
        
        forwardQueue.add(graph[source]);
        backwardQueue.add(graph[target]);
        
        long bestDist = Long.MAX_VALUE;
        int meetingNode = -1;
        
        // Búsqueda bidireccional alternada
        while (!forwardQueue.isEmpty() || !backwardQueue.isEmpty()) {
            // Paso de búsqueda hacia adelante
            if (!forwardQueue.isEmpty()) {
                Node node = forwardQueue.poll();
                
                if (node.distance.forwardProcessed || node.distance.forwardDist > bestDist) {
                    continue;
                }
                
                node.distance.forwardProcessed = true;
                
                if (node.distance.backwardQueryId == queryId) {
                    long totalDist = node.distance.forwardDist + node.distance.backwardDist;
                    if (totalDist < bestDist) {
                        bestDist = totalDist;
                        meetingNode = node.id;
                    }
                }
                
                for (Edge edge : node.outEdges) {
                    // Modified logic for better CH exploration
                    boolean shouldExplore = false;
                    
                    if (bothLowLevel) {
                        // For low-level nodes, explore more liberally
                        shouldExplore = (graph[edge.to].level >= node.level);
                    } else {
                        // Standard CH: only go to higher levels
                        shouldExplore = (graph[edge.to].level > node.level);
                    }
                    
                    if (shouldExplore) {
                        relaxEdge(node, graph[edge.to], edge, forwardQueue, true, queryId);
                    }
                }
            }
            
            // Paso de búsqueda hacia atrás
            if (!backwardQueue.isEmpty()) {
                Node node = backwardQueue.poll();
                
                if (node.distance.backwardProcessed || node.distance.backwardDist > bestDist) {
                    continue;
                }
                
                node.distance.backwardProcessed = true;
                
                if (node.distance.forwardQueryId == queryId) {
                    long totalDist = node.distance.forwardDist + node.distance.backwardDist;
                    if (totalDist < bestDist) {
                        bestDist = totalDist;
                        meetingNode = node.id;
                    }
                }
                
                for (Edge edge : node.inEdges) {
                    // Modified logic for better CH exploration
                    boolean shouldExplore = false;
                    
                    if (bothLowLevel) {
                        // For low-level nodes, explore more liberally
                        shouldExplore = (graph[edge.from].level >= node.level);
                    } else {
                        // Standard CH: only go to higher levels
                        shouldExplore = (graph[edge.from].level > node.level);
                    }
                    
                    if (shouldExplore) {
                        relaxEdge(node, graph[edge.from], edge, backwardQueue, false, queryId);
                    }
                }
            }
        }
        
        return new PathResult(bestDist == Long.MAX_VALUE ? -1 : bestDist, meetingNode);
    }
    
    // Modificar para guardar el predecesor y usar pesos customizados
    public void relaxEdge(Node current, Node neighbor, Edge edge, PriorityQueue<Node> queue, boolean forward, int queryId) {
        // Use custom weight if available, otherwise use original weight
        double edgeWeight = edge.getCustomWeight();
        
        // Skip prohibited routes (infinite weight)
        if (edgeWeight == Double.MAX_VALUE) {
            return; // Do not use prohibited routes
        }
        
        long edgeWeightLong = (long) edgeWeight;
        
        if (forward) {
            long newDist = current.distance.forwardDist + edgeWeightLong;
            if (neighbor.distance.forwardQueryId != queryId || newDist < neighbor.distance.forwardDist) {
                queue.remove(neighbor);
                neighbor.distance.forwardDist = newDist;
                neighbor.distance.forwardQueryId = queryId;
                neighbor.distance.forwardPredecessor = current.id; // Guardar predecesor
                queue.add(neighbor);
            }
        } else {
            long newDist = current.distance.backwardDist + edgeWeightLong;
            if (neighbor.distance.backwardQueryId != queryId || newDist < neighbor.distance.backwardDist) {
                queue.remove(neighbor);
                neighbor.distance.backwardDist = newDist;
                neighbor.distance.backwardQueryId = queryId;
                neighbor.distance.backwardPredecessor = current.id; // Guardar predecesor
                queue.add(neighbor);
            }
        }
    }
    
    // Método para reconstruir la ruta desde los predecesores - VERSIÓN CORREGIDA
    public List<Integer> reconstructPath(int source, int target, int meetingNode) {
        if (meetingNode == -1) return new ArrayList<>();
        
        List<Integer> forwardPath = new ArrayList<>();
        List<Integer> backwardPath = new ArrayList<>();
        
        // Reconstruir ruta hacia adelante (source -> meetingNode)
        int current = meetingNode;
        Set<Integer> visitedForward = new HashSet<>(); // Evitar loops infinitos
        while (current != source && current != -1 && !visitedForward.contains(current)) {
            visitedForward.add(current);
            forwardPath.add(0, current);
            current = graph[current].distance.forwardPredecessor;
        }
        if (current == source) {
            forwardPath.add(0, source);
        }
        
        // Reconstruir ruta hacia atrás (meetingNode -> target)
        current = meetingNode;
        Set<Integer> visitedBackward = new HashSet<>(); // Evitar loops infinitos
        boolean firstIteration = true;
        while (current != target && current != -1 && !visitedBackward.contains(current)) {
            if (!firstIteration) {
                visitedBackward.add(current);
                backwardPath.add(current);
            }
            firstIteration = false;
            int nextNode = graph[current].distance.backwardPredecessor;
            if (nextNode == current) break; // Evitar loop hacia sí mismo
            current = nextNode;
        }
        if (current == target) {
            backwardPath.add(target);
        }
        
        // Combinar las rutas - evitar duplicar el meetingNode
        if (!backwardPath.isEmpty() && !forwardPath.isEmpty() && 
            forwardPath.get(forwardPath.size() - 1).equals(backwardPath.get(0))) {
            backwardPath.remove(0); // Remover duplicado del meetingNode
        }
        
        forwardPath.addAll(backwardPath);
        return forwardPath;
    }
    
    // Fallback method using simple Dijkstra when CH fails
    public PathResult computeShortestPathFallback(int source, int target, int queryId) {
        if (source == target) return new PathResult(0, source);
        
        long[] distances = new long[graph.length];
        int[] predecessors = new int[graph.length];
        Arrays.fill(distances, Long.MAX_VALUE);
        Arrays.fill(predecessors, -1);
        distances[source] = 0;
        
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[1], b[1]));
        pq.offer(new int[]{source, 0});
        
        boolean[] visited = new boolean[graph.length];
        
        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int nodeId = current[0];
            long dist = current[1];
            
            if (visited[nodeId]) continue;
            visited[nodeId] = true;
            
            if (nodeId == target) {
                // Found target, set up predecessor info for reconstruction
                for (int i = 0; i < graph.length; i++) {
                    if (predecessors[i] != -1) {
                        graph[i].distance.forwardPredecessor = predecessors[i];
                        graph[i].distance.forwardQueryId = queryId;
                    }
                }
                return new PathResult(dist, target);
            }
            
            for (Edge edge : graph[nodeId].outEdges) {
                if (!visited[edge.to]) {
                    // Use custom weight if available
                    double edgeWeight = edge.getCustomWeight();
                    
                    // Skip prohibited routes
                    if (edgeWeight == Double.MAX_VALUE) {
                        continue;
                    }
                    
                    long newDist = dist + (long)edgeWeight;
                    if (newDist < distances[edge.to]) {
                        distances[edge.to] = newDist;
                        predecessors[edge.to] = nodeId;
                        pq.offer(new int[]{edge.to, (int)newDist});
                    }
                }
            }
        }
        
        return new PathResult(-1, -1); // No path found
    }
    
    // Enhanced main method that tries CH first, then fallback
    public PathResult computeShortestPathEnhanced(int source, int target, int queryId) {
        // Try CH first
        PathResult chResult = computeShortestPath(source, target, queryId);
        
        // If CH fails, try fallback Dijkstra
        if (chResult.distance == -1) {
            System.out.println("CH failed, trying Dijkstra fallback...");
            PathResult fallbackResult = computeShortestPathFallback(source, target, queryId);
            if (fallbackResult.distance != -1) {
                System.out.println("Fallback Dijkstra found route: " + fallbackResult.distance);
                return fallbackResult;
            }
        }
        
        return chResult;
    }
}