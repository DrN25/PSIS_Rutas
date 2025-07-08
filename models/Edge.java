package models;

public class Edge {
    public int from;
    public int to;
    public long weight;
    public String streetName;
        
    public Edge(int from, int to, long weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.streetName = "Unknown";
    }
        
    public Edge(int from, int to, long weight, String streetName) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.streetName = streetName != null ? streetName : "Unknown";
    }
}
