package models;

import java.util.Comparator;

/**
 * Comparator for forward search in bidirectional Dijkstra.
 * Orders nodes by their forward distance from the source.
 * Used in priority queues during the forward search phase.
 */
public class ForwardDistanceComparator implements Comparator<Node> {
    
    @Override
    public int compare(Node a, Node b) {
        return Long.compare(a.distance.forwardDist, b.distance.forwardDist);
    }
}
