package models;

import java.util.Comparator;

public class BackwardDistanceComparator implements Comparator<Node> {
    
    @Override
    public int compare(Node a, Node b) {
        return Long.compare(a.distance.backwardDist, b.distance.backwardDist);
    }
}
