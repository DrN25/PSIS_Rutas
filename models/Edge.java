package models;

public class Edge {
    public int from;
    public int to;
    public long weight;
    public String streetName;
    
    // CCH-related fields
    private double customWeight;
    private double originalDistance;
    
    // CSV fields for filtering
    private String sentido;
    private String tipoC;
    private String redJerarq;
    private String bicisenda;
        
    public Edge(int from, int to, long weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.streetName = "Unknown";
        this.originalDistance = weight;
        this.customWeight = weight;
        initializeCSVFields();
    }
        
    public Edge(int from, int to, long weight, String streetName) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.streetName = streetName != null ? streetName : "Unknown";
        this.originalDistance = weight;
        this.customWeight = weight;
        initializeCSVFields();
    }
    
    public Edge(int from, int to, long weight, String streetName, 
                String sentido, String tipoC, String redJerarq, String bicisenda) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.streetName = streetName != null ? streetName : "Unknown";
        this.originalDistance = weight;
        this.customWeight = weight;
        this.sentido = sentido != null ? sentido : "";
        this.tipoC = tipoC != null ? tipoC : "";
        this.redJerarq = redJerarq != null ? redJerarq : "";
        this.bicisenda = bicisenda != null ? bicisenda : "";
    }
    
    private void initializeCSVFields() {
        this.sentido = "";
        this.tipoC = "";
        this.redJerarq = "";
        this.bicisenda = "";
    }
    
    // Getters and setters
    public double getDistance() {
        return originalDistance;
    }
    
    public double getCustomWeight() {
        return customWeight;
    }
    
    public void setCustomWeight(double customWeight) {
        this.customWeight = customWeight;
    }
    
    public String getSentido() {
        return sentido;
    }
    
    public void setSentido(String sentido) {
        this.sentido = sentido != null ? sentido : "";
    }
    
    public String getTipoC() {
        return tipoC;
    }
    
    public void setTipoC(String tipoC) {
        this.tipoC = tipoC != null ? tipoC : "";
    }
    
    public String getRedJerarq() {
        return redJerarq;
    }
    
    public void setRedJerarq(String redJerarq) {
        this.redJerarq = redJerarq != null ? redJerarq : "";
    }
    
    public String getBicisenda() {
        return bicisenda;
    }
    
    public void setBicisenda(String bicisenda) {
        this.bicisenda = bicisenda != null ? bicisenda : "";
    }
    
    public boolean isProhibited() {
        return customWeight == Double.MAX_VALUE;
    }
    
    public boolean isPreferred() {
        return customWeight < originalDistance;
    }
}
