package models;

import java.util.Comparator;

public class ImportanceComparator implements Comparator<Node> {
    
    @Override
    public int compare(Node a, Node b) {
        return Long.compare(a.importance, b.importance);
    }
}
