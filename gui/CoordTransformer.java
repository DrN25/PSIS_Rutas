package gui;

import java.awt.geom.Point2D;
import java.util.Map;

public class CoordTransformer {
    public static Point2D.Double coordToScreen(String coord, double minLon, double maxLat, double scale, double offsetX, double offsetY) {
        try {
            String[] parts = coord.split(" ");
            double lon = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);

            double x = (lon - minLon) * scale + offsetX;
            double y = (maxLat - lat) * scale + offsetY;
            return new Point2D.Double(x, y);
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer screenToNodeId(double x, double y, Map<Integer, String> idToCoord, double minLon, double maxLat, double scale, double offsetX, double offsetY) {
        double minDist = 10.0;
        Integer nearest = null;

        for (Map.Entry<Integer, String> entry : idToCoord.entrySet()) {
            Point2D.Double pos = coordToScreen(entry.getValue(), minLon, maxLat, scale, offsetX, offsetY);
            if (pos != null) {
                double dist = pos.distance(x, y);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = entry.getKey();
                }
            }
        }
        return nearest;
    }
}
