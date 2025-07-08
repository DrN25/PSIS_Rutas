package models;

import java.util.ArrayList;

/**
 * Basic class for representing a graph node with contraction hierarchies properties.
 * This class is used for road network graph representation with support for:
 * - Contraction Hierarchies preprocessing
 * - Bidirectional search queries
 * - Route planning algorithms
 */
public class Node {
    public int id;
    public ArrayList<Edge> outEdges;
    public ArrayList<Edge> inEdges;
    public int level;
    public boolean contracted;
    public long importance;
    
    // For contraction
    public int edgeDiff;
    public int shortcutCount;
    public int contractedNeighbors;
    
    // For queries
    public Distance distance;
    
    public Node(int id) {
        this.id = id;
        this.outEdges = new ArrayList<>();
        this.inEdges = new ArrayList<>();
        this.contracted = false;
        this.level = 0;
        this.distance = new Distance();
    }
    
    public void computeImportance() {
        // Number of shortcuts that might be added - number of edges removed
        this.edgeDiff = (inEdges.size() * outEdges.size()) - inEdges.size() - outEdges.size();
        this.shortcutCount = inEdges.size() + outEdges.size();
        this.importance = edgeDiff * 14 + shortcutCount * 25 + contractedNeighbors * 10;
    }
}
