package models;

public enum VehicleProfile {
    VEHICULOS("Vehiculos (Auto/Transporte publico)"),
    BICICLETA("Bicicleta (Evita autopistas y trafico)"),
    PEATONAL("Peatonal (Solo rutas a pie)");
    
    private final String displayName;
    
    VehicleProfile(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
