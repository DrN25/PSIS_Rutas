package algorithms;

import java.util.*;
import models.*;

public class DijkstraSearch {
    private final Node[] graph;
    
    public DijkstraSearch(Node[] graph) {
        this.graph = graph;
    }
    
    public static class Result {
        public List<Integer> path;
        public long distance;
        
        public Result(List<Integer> path, long distance) {
            this.path = path;
            this.distance = distance;
        }
    }
    
    public Result compute(int origin, int destination) {
        if (origin == destination) {
            return new Result(Arrays.asList(origin), 0);
        }
        
        long[] distances = new long[graph.length];
        int[] predecessors = new int[graph.length];
        Arrays.fill(distances, Long.MAX_VALUE);
        Arrays.fill(predecessors, -1);
        distances[origin] = 0;
        
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[1], b[1]));
        pq.offer(new int[]{origin, 0});
        
        boolean[] visited = new boolean[graph.length];
        
        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int nodeId = current[0];
            long dist = current[1];
            
            if (visited[nodeId]) continue;
            visited[nodeId] = true;
            
            if (nodeId == destination) {
                // Reconstruct path
                List<Integer> path = new ArrayList<>();
                for (int at = destination; at != -1; at = predecessors[at]) {
                    path.add(at);
                }
                Collections.reverse(path);
                return new Result(path, dist);
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
        
        return new Result(null, -1); // No path found
    }
}
