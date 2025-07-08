package models;

import java.util.Comparator;

/**
 * Comparator for backward search in bidirectional Dijkstra.
 * Orders nodes by their backward distance from the target.
 * Used in priority queues during the backward search phase.
 */
public class BackwardDistanceComparator implements Comparator<Node> {
    
    @Override
    public int compare(Node a, Node b) {
        return Long.compare(a.distance.backwardDist, b.distance.backwardDist);
    }
}
