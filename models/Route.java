package models;

public class Route {
    public int origin;
    public int destination;
    public long cost;
    public String street;
    public boolean isBidirectional;
    
    public Route(int origin, int destination, long cost, String street, boolean isBidirectional) {
        this.origin = origin;
        this.destination = destination;
        this.cost = cost;
        this.street = street;
        this.isBidirectional = isBidirectional;
    }
}