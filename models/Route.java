package models;

public class Route {
    public int origin;
    public int destination;
    public long cost;
    public String street;
    public boolean isBidirectional;
    
    // CSV fields for CCH customization
    public String sentido;
    public String tipoC;
    public String redJerarq;
    public String bicisenda;
    
    public Route(int origin, int destination, long cost, String street, boolean isBidirectional) {
        this.origin = origin;
        this.destination = destination;
        this.cost = cost;
        this.street = street;
        this.isBidirectional = isBidirectional;
        
        // Initialize CSV fields
        this.sentido = "";
        this.tipoC = "";
        this.redJerarq = "";
        this.bicisenda = "";
    }
    
    public Route(int origin, int destination, long cost, String street, boolean isBidirectional,
                 String sentido, String tipoC, String redJerarq, String bicisenda) {
        this.origin = origin;
        this.destination = destination;
        this.cost = cost;
        this.street = street;
        this.isBidirectional = isBidirectional;
        this.sentido = sentido != null ? sentido : "";
        this.tipoC = tipoC != null ? tipoC : "";
        this.redJerarq = redJerarq != null ? redJerarq : "";
        this.bicisenda = bicisenda != null ? bicisenda : "";
    }
}