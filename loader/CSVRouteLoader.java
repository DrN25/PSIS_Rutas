package loader;

import models.*;
import utils.CSVUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CSVRouteLoader {

    public static MapDataResult loadFromCSV(String dirpath) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dirpath), StandardCharsets.UTF_8));

        String line;
        List<Route> routes = new ArrayList<>();
        Map<String, Integer> nodeIndex = new HashMap<>();
        Map<Integer, String> idToCoord = new HashMap<>();
        AtomicInteger currentId = new AtomicInteger(0);

        br.readLine(); // skip header
        int lineNumber = 1;

        while ((line = br.readLine()) != null) {
            lineNumber++;
            String[] fields = CSVUtils.parseCSVLine(line);

            if (fields.length != 23) continue;

            if (fields.length < 23 || !fields[22].contains("LINESTRING")) continue;

            String geometry = fields[22].replace("\"", "").replace("LINESTRING", "").trim();
            if (geometry.isEmpty() || !geometry.startsWith("(") || !geometry.endsWith(")")) continue;

            String[] points = geometry.substring(1, geometry.length() - 1).split(", ");
            if (points.length < 2) continue;

            String start = points[0].trim();
            String end = points[points.length - 1].trim();

            String street = "Unknown";
            if (fields[7] != null && !fields[7].trim().isEmpty()) {
                street = fields[7].replace("\"", "").trim();
            } else if (fields[2] != null && !fields[2].trim().isEmpty()) {
                street = fields[2].replace("\"", "").trim();
            }

            boolean isBidirectional = false;
            if (fields.length > 11 && fields[11] != null) {
                String sentido = fields[11].replace("\"", "").trim().toUpperCase();
                isBidirectional = sentido.equals("DOBLE");
            }

            int origin = nodeIndex.computeIfAbsent(start, k -> {
                int id = currentId.getAndIncrement();
                idToCoord.put(id, start);
                return id;
            });

            int destination = nodeIndex.computeIfAbsent(end, k -> {
                int id = currentId.getAndIncrement();
                idToCoord.put(id, end);
                return id;
            });

            double length = 1.0;
            try {
                if (fields[10] != null && !fields[10].trim().isEmpty()) {
                    length = Double.parseDouble(fields[10].replace("\"", "").trim());
                }
            } catch (NumberFormatException ignored) {}

            long cost = (long) length;
            routes.add(new Route(origin, destination, cost, street, isBidirectional));
        }
        br.close();

        int n = nodeIndex.size();
        Node[] graph = new Node[n];
        for (int i = 0; i < n; i++) graph[i] = new Node(i);

        int totalEdges = 0;
        for (Route route : routes) {
            Edge forwardEdge = new Edge(route.origin, route.destination, route.cost, route.street);
            graph[route.origin].outEdges.add(forwardEdge);
            graph[route.destination].inEdges.add(forwardEdge);
            totalEdges++;

            if (route.isBidirectional) {
                Edge backwardEdge = new Edge(route.destination, route.origin, route.cost, route.street);
                graph[route.destination].outEdges.add(backwardEdge);
                graph[route.origin].inEdges.add(backwardEdge);
                totalEdges++;
            }
        }

        Map<String, String> streetNameMap = new HashMap<>();
        for (Route route : routes) {
            streetNameMap.put(route.origin + "_" + route.destination, route.street);
            if (route.isBidirectional) {
                streetNameMap.put(route.destination + "_" + route.origin, route.street);
            }
        }

      return new MapDataResult(graph, idToCoord, nodeIndex, streetNameMap, routes);
    }
}
