package loader;

import models.*;
import java.util.Map;
import java.util.List;

public class MapDataResult {
    public Node[] graph;
    public Map<Integer, String> idToCoord;
    public Map<String, Integer> nodeIndex;
    public Map<String, String> streetNameMap;
    public List<Route> routes;

    public MapDataResult(Node[] graph, Map<Integer, String> idToCoord, Map<String, Integer> nodeIndex,
                         Map<String, String> streetNameMap, List<Route> routes) {
        this.graph = graph;
        this.idToCoord = idToCoord;
        this.nodeIndex = nodeIndex;
        this.streetNameMap = streetNameMap;
        this.routes = routes;
    }
}
