package models;

import java.util.Comparator;

public class ForwardDistanceComparator implements Comparator<Node> {
    
    @Override
    public int compare(Node a, Node b) {
        return Long.compare(a.distance.forwardDist, b.distance.forwardDist);
    }
}
