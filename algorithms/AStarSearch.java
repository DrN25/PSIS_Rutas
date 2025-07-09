package algorithms;

import java.util.*;
import models.*;

public class AStarSearch {
    private final Node[] graph;
    private final Map<Integer, String> idToCoord;

    public AStarSearch(Node[] graph, Map<Integer, String> idToCoord) {
        this.graph = graph;
        this.idToCoord = idToCoord;
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
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance.f));
        boolean[] visited = new boolean[graph.length];

        for (Node node : graph) {
            node.distance = new Distance();
            node.distance.g = Long.MAX_VALUE;
            node.distance.f = Double.MAX_VALUE;
        }

        graph[origin].distance.g = 0;
        graph[origin].distance.f = heuristic(origin, destination);
        open.add(graph[origin]);

        Map<Integer, Integer> prev = new HashMap<>();

        while (!open.isEmpty()) {
            Node current = open.poll();
            int u = current.id;

            if (u == destination) break;
            if (visited[u]) continue;
            visited[u] = true;

            for (Edge edge : current.outEdges) {
                int v = edge.to;
                long tentativeG = graph[u].distance.g + edge.weight;

                if (tentativeG < graph[v].distance.g) {
                    graph[v].distance.g = tentativeG;
                    graph[v].distance.f = tentativeG + heuristic(v, destination);
                    prev.put(v, u);
                    open.add(graph[v]);
                }
            }
        }

        if (!prev.containsKey(destination)) return new Result(null, -1);

        List<Integer> path = new ArrayList<>();
        for (int at = destination; at != origin; at = prev.get(at)) {
            path.add(at);
        }
        path.add(origin);
        Collections.reverse(path);

        return new Result(path, graph[destination].distance.g);
    }

    private double heuristic(int from, int to) {
        String[] coord1 = idToCoord.get(from).split(" ");
        String[] coord2 = idToCoord.get(to).split(" ");
        double lon1 = Double.parseDouble(coord1[0]);
        double lat1 = Double.parseDouble(coord1[1]);
        double lon2 = Double.parseDouble(coord2[0]);
        double lat2 = Double.parseDouble(coord2[1]);

        double dx = lon1 - lon2;
        double dy = lat1 - lat2;
        return Math.sqrt(dx * dx + dy * dy) * 111_000; // metros aproximados
    }
}
