package models;

public class EdgeWeightCustomizer {
    
    /**
     * Calculates the weight for an edge based on the vehicle profile
     * @param edge The edge to calculate weight for
     * @param profile The vehicle profile
     * @return The calculated weight (Double.MAX_VALUE for prohibited routes)
     */
    public static double calculateWeight(Edge edge, VehicleProfile profile) {
        double baseDistance = edge.getDistance();
        
        // Get CSV fields from edge
        String sentido = edge.getSentido();
        String tipoC = edge.getTipoC();
        String redJerarq = edge.getRedJerarq();
        String bicisenda = edge.getBicisenda();
        
        switch (profile) {
            case VEHICULOS:
                return calculateVehiculosWeight(baseDistance, sentido, tipoC, redJerarq);
            
            case BICICLETA:
                return calculateBicicletaWeight(baseDistance, sentido, tipoC, redJerarq, bicisenda);
            
            case PEATONAL:
                return calculatePeatonalWeight(baseDistance, sentido, tipoC, redJerarq);
            
            default:
                return baseDistance;
        }
    }
    
    private static double calculateVehiculosWeight(double baseDistance, String sentido, 
                                                   String tipoC, String redJerarq) {
        // PROHIBITED ROUTES (very light color in visualization)
        if ("PEATONAL".equals(sentido) || 
            "SENDERO".equals(tipoC) || 
            "CALLE PEATONAL".equals(tipoC)) {
            return Double.MAX_VALUE;
        }
        
        // ALLOWED ROUTES with speed factors
        double factor = 1.0;
        
        // Speed bonus by road hierarchy
        if ("VIA TRONCAL".equals(redJerarq)) {
            factor = 0.6; // Fastest
        } else if ("VIA DISTRIBUIDORA PRINCIPAL".equals(redJerarq)) {
            factor = 0.7;
        } else if ("VIA DISTRIBUIDORA COMPLEMENTARIA".equals(redJerarq)) {
            factor = 0.9;
        }
        
        // Bonus by road type
        if ("AUTOPISTA".equals(tipoC)) {
            factor *= 0.5; // Very fast
        } else if ("AVENIDA".equals(tipoC)) {
            factor *= 0.8; // Fast
        } else if ("PASAJE".equals(tipoC)) {
            factor *= 1.3; // Slow
        }
        
        // Private passage penalty
        if ("PJE. PRIVADO".equals(sentido)) {
            factor *= 2.0;
        }
        
        return baseDistance * factor;
    }
    
    private static double calculateBicicletaWeight(double baseDistance, String sentido, 
                                                   String tipoC, String redJerarq, String bicisenda) {
        // PROHIBITED ROUTES (red in visualization)
        if ("AUTOPISTA".equals(tipoC) || 
            "VIA TRONCAL".equals(redJerarq)) {
            return Double.MAX_VALUE;
        }
        
        double factor = 1.0;
        
        // VERY PREFERRED: Explicit bike infrastructure
        if (bicisenda != null && !bicisenda.isEmpty() && !"-".equals(bicisenda)) {
            factor = 0.2; // Very preferred
        }
        // PREFERRED: Local streets (less traffic)
        else if ("VIA LOCAL".equals(redJerarq)) {
            factor = 0.7; // Preferred for bikes
        }
        // NORMAL: Complementary distributor roads
        else if ("VIA DISTRIBUIDORA COMPLEMENTARIA".equals(redJerarq)) {
            factor = 1.0; // Normal
        }
        // DISCOURAGED: Main distributor roads and large avenues
        else if ("VIA DISTRIBUIDORA PRINCIPAL".equals(redJerarq)) {
            factor = 2.5; // Discouraged due to heavy traffic
        }
        
        // Additional penalties by street type
        if ("AVENIDA".equals(tipoC)) {
            factor *= 1.5; // Avenues have more traffic
        } else if ("PASAJE".equals(tipoC)) {
            factor *= 0.8; // Passages are quieter
        }
        
        // Private passage penalty
        if ("PJE. PRIVADO".equals(sentido)) {
            factor *= 2.0;
        }
        
        return baseDistance * factor;
    }
    
    private static double calculatePeatonalWeight(double baseDistance, String sentido, 
                                                  String tipoC, String redJerarq) {
        // PROHIBITED ROUTES
        if ("AUTOPISTA".equals(tipoC)) {
            return Double.MAX_VALUE;
        }
        
        double factor = 1.0;
        
        // Bonus for pedestrian infrastructure
        if ("PEATONAL".equals(sentido)) {
            factor = 0.7; // Preferred
        } else if ("SENDERO".equals(tipoC) || "CALLE PEATONAL".equals(tipoC)) {
            factor = 0.5; // Very preferred
        } else if ("PASAJE".equals(tipoC)) {
            factor = 0.9; // Pedestrian-friendly
        }
        
        // Avoid heavy traffic areas
        if ("VIA TRONCAL".equals(redJerarq)) {
            factor *= 3.0; // Very unpleasant
        } else if ("VIA DISTRIBUIDORA PRINCIPAL".equals(redJerarq)) {
            factor *= 2.0; // Unpleasant
        } else if ("AVENIDA".equals(tipoC)) {
            factor *= 1.5; // Some traffic
        }
        
        return baseDistance * factor;
    }
}
