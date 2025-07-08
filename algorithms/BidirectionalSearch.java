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
                    if (graph[edge.to].level > node.level) {
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
                    if (graph[edge.from].level > node.level) {
                        relaxEdge(node, graph[edge.from], edge, backwardQueue, false, queryId);
                    }
                }
            }
        }
        
        return new PathResult(bestDist == Long.MAX_VALUE ? -1 : bestDist, meetingNode);
    }
    
    // Modificar para guardar el predecesor
    public void relaxEdge(Node current, Node neighbor, Edge edge, PriorityQueue<Node> queue, boolean forward, int queryId) {
        if (forward) {
            long newDist = current.distance.forwardDist + edge.weight;
            if (neighbor.distance.forwardQueryId != queryId || newDist < neighbor.distance.forwardDist) {
                queue.remove(neighbor);
                neighbor.distance.forwardDist = newDist;
                neighbor.distance.forwardQueryId = queryId;
                neighbor.distance.forwardPredecessor = current.id; // Guardar predecesor
                queue.add(neighbor);
            }
        } else {
            long newDist = current.distance.backwardDist + edge.weight;
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
}