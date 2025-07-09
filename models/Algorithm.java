package models;

public enum Algorithm {
    CCH("CCH"),
    ASTAR("A*"),
    ALT("ALT");
    // DIJKSTRA("Dijkstra"); // Eliminado del selector
    
    private final String displayName;
    
    Algorithm(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
