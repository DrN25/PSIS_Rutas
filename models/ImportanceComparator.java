package models;

import java.util.Comparator;

/**
 * Comparator for contraction order based on node importance.
 * Used during the contraction hierarchies preprocessing phase.
 * Nodes with lower importance are contracted first.
 */
public class ImportanceComparator implements Comparator<Node> {
    
    @Override
    public int compare(Node a, Node b) {
        return Long.compare(a.importance, b.importance);
    }
}
