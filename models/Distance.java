package models;

public class Distance {
    public long value;
    public int queryId;

    // Para búsqueda bidireccional
    public long forwardDist;
    public long backwardDist;
    public int forwardQueryId;
    public int backwardQueryId;

    public boolean forwardProcessed;
    public boolean backwardProcessed;

    // Para rastrear el camino
    public int forwardPredecessor;
    public int backwardPredecessor;

    // NUEVOS CAMPOS PARA A*
    public long g;       // costo real desde el origen
    public double f;     // costo total estimado (g + h)

    public Distance() {
        this.value = Long.MAX_VALUE;
        this.queryId = -1;
        this.forwardDist = Long.MAX_VALUE;
        this.backwardDist = Long.MAX_VALUE;
        this.forwardQueryId = -1;
        this.backwardQueryId = -1;
        this.forwardPredecessor = -1;
        this.backwardPredecessor = -1;

        // Inicializar g y f para A*
        this.g = Long.MAX_VALUE;
        this.f = Double.MAX_VALUE;
    }
}
