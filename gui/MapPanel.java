// MapPanel.java
package gui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import models.*;

public class MapPanel extends JPanel {
    private final MapRenderer renderer;
    private final MapInteractionHandler interactionHandler;

    public Node[] graphData;
    public Map<Integer, String> idToCoordData;
    public List<Integer> currentRoute;
    public Integer selectedOrigin;
    public Integer selectedDestination;
    public double scale, offsetX, offsetY;
    public double minLat, maxLat, minLon, maxLon;

    public JLabel statusLabel;
    public JButton findRouteButton;

    public static final Color STREET_COLOR = new Color(200, 200, 200);
    public static final Color ORIGIN_COLOR = Color.GREEN;
    public static final Color DESTINATION_COLOR = Color.BLUE;
    public static final Color SELECTED_ROUTE_COLOR = Color.RED;

    public MapPanel() {
        setPreferredSize(new Dimension(1200, 800));
        setBackground(Color.WHITE);

        this.renderer = new MapRenderer(this);
        this.interactionHandler = new MapInteractionHandler(this);
        this.currentRoute = new ArrayList<>();

        addMouseListener(interactionHandler.getMouseAdapter());
        addMouseMotionListener(interactionHandler.getMouseAdapter());
        addMouseWheelListener(interactionHandler.getWheelListener());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (graphData == null || idToCoordData == null) {
            g.drawString("Loading map data...", 20, 30);
            return;
        }

        if (minLat == 0 && maxLat == 0) {
            calculateMapBounds();
            fitMapToWindow();
        }

        renderer.render((Graphics2D) g);
    }

    public void calculateMapBounds() {
        double BA_MIN_LON = -58.7, BA_MAX_LON = -58.3;
        double BA_MIN_LAT = -34.8, BA_MAX_LAT = -34.4;

        minLat = BA_MAX_LAT;
        maxLat = BA_MIN_LAT;
        minLon = BA_MAX_LON;
        maxLon = BA_MIN_LON;

        int validCoords = 0;

        for (String coord : idToCoordData.values()) {
            String[] parts = coord.split(" ");
            if (parts.length == 2) {
                try {
                    double lon = Double.parseDouble(parts[0]);
                    double lat = Double.parseDouble(parts[1]);

                    if (lon >= BA_MIN_LON && lon <= BA_MAX_LON &&
                        lat >= BA_MIN_LAT && lat <= BA_MAX_LAT) {
                        minLon = Math.min(minLon, lon);
                        maxLon = Math.max(maxLon, lon);
                        minLat = Math.min(minLat, lat);
                        maxLat = Math.max(maxLat, lat);
                        validCoords++;
                    }
                } catch (NumberFormatException e) {
                    // skip
                }
            }
        }

        if (validCoords > 0) {
            double lonRange = maxLon - minLon;
            double latRange = maxLat - minLat;

            if (lonRange < 0.1) {
                double center = (minLon + maxLon) / 2;
                minLon = center - 0.05;
                maxLon = center + 0.05;
            }
            if (latRange < 0.1) {
                double center = (minLat + maxLat) / 2;
                minLat = center - 0.05;
                maxLat = center + 0.05;
            }

            lonRange = maxLon - minLon;
            latRange = maxLat - minLat;
            minLon -= lonRange * 0.02;
            maxLon += lonRange * 0.02;
            minLat -= latRange * 0.02;
            maxLat += latRange * 0.02;
        } else {
            minLon = -58.6;
            maxLon = -58.3;
            minLat = -34.7;
            maxLat = -34.5;
        }
    }

    public void fitMapToWindow() {
        double lonRange = maxLon - minLon;
        double latRange = maxLat - minLat;

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        if (panelWidth <= 0) panelWidth = 800;
        if (panelHeight <= 0) panelHeight = 600;

        double scaleX = (panelWidth * 0.9) / lonRange;
        double scaleY = (panelHeight * 0.9) / latRange;
        scale = Math.min(scaleX, scaleY);

        if (scale < 1000) scale = 1000;
        if (scale > 1000000) scale = 1000000;

        double mapCenterLon = (minLon + maxLon) / 2.0;
        double mapCenterLat = (minLat + maxLat) / 2.0;

        double centerX = (mapCenterLon - minLon) * scale;
        double centerY = (maxLat - mapCenterLat) * scale;

        offsetX = (panelWidth / 2.0) - centerX;
        offsetY = (panelHeight / 2.0) - centerY;
    }
}
