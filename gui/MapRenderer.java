package gui;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import models.Edge;
import models.Node;

public class MapRenderer {
    private final MapPanel panel;

    public MapRenderer(MapPanel panel) {
        this.panel = panel;
    }

    public void render(Graphics2D g2d) {
        if (panel.graphData == null || panel.idToCoordData == null) {
            g2d.drawString("Loading map data...", 20, 30);
            return;
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (panel.minLat == 0 && panel.maxLat == 0) {
            panel.calculateMapBounds();
            panel.fitMapToWindow();
        }

        drawStreets(g2d);
        drawAllNodes(g2d);
        drawRoute(g2d, panel.currentRoute);
        drawSelectedNodes(g2d);
        drawLegend(g2d);
    }

    private void drawStreets(Graphics2D g2d) {
        for (Node node : panel.graphData) {
            for (Edge edge : node.outEdges) {
                Point2D.Double start = coordToScreen(panel.idToCoordData.get(edge.from));
                Point2D.Double end = coordToScreen(panel.idToCoordData.get(edge.to));

                if (start != null && end != null) {
                    drawStreetSegment(g2d, start, end, edge);
                }
            }
        }
    }

    private void drawStreetSegment(Graphics2D g2d, Point2D.Double start, Point2D.Double end, Edge edge) {
        boolean isBidirectional = false;
        for (Edge reverseEdge : panel.graphData[edge.to].outEdges) {
            if (reverseEdge.to == edge.from && reverseEdge.streetName.equals(edge.streetName)) {
                isBidirectional = true;
                break;
            }
        }

        double scale = panel.scale;
        float baseThickness = Math.max(1.0f, (float)(scale / 50000));
        if (scale > 100000) baseThickness = Math.max(2.0f, (float)(scale / 100000));

        if (isBidirectional) {
            g2d.setColor(MapPanel.STREET_COLOR);
            g2d.setStroke(new BasicStroke(baseThickness * 1.5f));
        } else {
            g2d.setColor(MapPanel.STREET_COLOR.darker());
            g2d.setStroke(new BasicStroke(baseThickness));
        }

        g2d.drawLine((int) start.x, (int) start.y, (int) end.x, (int) end.y);

        if (!isBidirectional && scale > 2000) {
            drawDirectionArrow(g2d, start, end);
        }
    }

    private void drawDirectionArrow(Graphics2D g2d, Point2D.Double start, Point2D.Double end) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 20) return;

        double unitX = dx / length;
        double unitY = dy / length;

        double scale = panel.scale;
        double arrowSize = Math.min(15, Math.max(3, scale / 10000));
        if (scale > 100000) arrowSize = Math.min(25, scale / 50000);

        double arrowX = start.x + dx * 0.7;
        double arrowY = start.y + dy * 0.7;

        double angle = Math.PI / 6;
        double arrowX1 = arrowX - arrowSize * (unitX * Math.cos(angle) - unitY * Math.sin(angle));
        double arrowY1 = arrowY - arrowSize * (unitX * Math.sin(angle) + unitY * Math.cos(angle));
        double arrowX2 = arrowX - arrowSize * (unitX * Math.cos(-angle) - unitY * Math.sin(-angle));
        double arrowY2 = arrowY - arrowSize * (unitX * Math.sin(-angle) + unitY * Math.cos(-angle));

        float arrowThickness = Math.max(0.8f, (float)(scale / 100000));
        if (scale > 200000) arrowThickness = Math.max(1.5f, (float)(scale / 200000));

        g2d.setStroke(new BasicStroke(arrowThickness));
        g2d.drawLine((int) arrowX, (int) arrowY, (int) arrowX1, (int) arrowY1);
        g2d.drawLine((int) arrowX, (int) arrowY, (int) arrowX2, (int) arrowY2);
    }

    private void drawRoute(Graphics2D g2d, List<Integer> route) {
        if (route == null || route.isEmpty()) return;

        g2d.setColor(MapPanel.SELECTED_ROUTE_COLOR);
        float thickness = Math.max(3.0f, (float)(panel.scale / 20000));
        if (panel.scale > 100000) thickness = Math.max(5.0f, (float)(panel.scale / 50000));
        g2d.setStroke(new BasicStroke(thickness));

        for (int i = 0; i < route.size() - 1; i++) {
            Point2D.Double start = coordToScreen(panel.idToCoordData.get(route.get(i)));
            Point2D.Double end = coordToScreen(panel.idToCoordData.get(route.get(i + 1)));

            if (start != null && end != null) {
                g2d.drawLine((int) start.x, (int) start.y, (int) end.x, (int) end.y);
            }
        }
    }

    private void drawSelectedNodes(Graphics2D g2d) {
        double scale = panel.scale;
        int size = Math.max(8, Math.min(25, (int)(scale / 4000)));
        if (scale > 100000) size = Math.max(15, Math.min(40, (int)(scale / 15000)));

        if (panel.selectedOrigin != null) {
            drawNode(g2d, panel.selectedOrigin, MapPanel.ORIGIN_COLOR, "Origin", size);
        }

        if (panel.selectedDestination != null) {
            drawNode(g2d, panel.selectedDestination, MapPanel.DESTINATION_COLOR, "Destination", size);
        }
    }

    private void drawNode(Graphics2D g2d, int nodeId, Color color, String label, int size) {
        Point2D.Double pos = coordToScreen(panel.idToCoordData.get(nodeId));
        if (pos != null) {
            g2d.setColor(color);
            g2d.fillOval((int) pos.x - size / 2, (int) pos.y - size / 2, size, size);
            g2d.setColor(Color.BLACK);
            g2d.drawOval((int) pos.x - size / 2, (int) pos.y - size / 2, size, size);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString(label, (int) pos.x + size / 2 + 5, (int) pos.y - size / 2 - 5);
        }
    }

    private void drawLegend(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));

        int x = 10;
        int y = panel.getHeight() - 120;

        g2d.drawString("Zoom: " + String.format("%.0f", panel.scale) + " (Range: 50 - 1,000,000)", x, y);
        g2d.drawString("Nodes: " + (panel.graphData != null ? panel.graphData.length : 0), x, y + 15);

        if (panel.scale >= 500) {
            g2d.drawString("o White circles = Available nodes", x, y + 30);
            g2d.drawString("* Green = Origin, Blue = Destination", x, y + 45);
        } else {
            g2d.drawString("Zoom in more to see individual nodes", x, y + 30);
        }

        g2d.drawString("Mouse: wheel=zoom, drag=pan, click=select", x, y + 60);

        if (panel.selectedOrigin != null || panel.selectedDestination != null) {
            y += 75;
            if (panel.selectedOrigin == null) {
                g2d.drawString("Click a node to select origin", x, y);
            } else if (panel.selectedDestination == null) {
                g2d.drawString("Click another node to select destination", x, y);
            } else {
                g2d.drawString("Press 'Find Route' to calculate path", x, y);
            }
        }
    }

    private void drawAllNodes(Graphics2D g2d) {
        if (panel.scale < 500) return;

        int nodeSize = Math.max(2, Math.min(20, (int)(panel.scale / 5000)));
        if (panel.scale > 100000) nodeSize = Math.max(10, Math.min(30, (int)(panel.scale / 20000)));

        g2d.setColor(Color.WHITE);

        for (Integer nodeId : panel.idToCoordData.keySet()) {
            if (nodeId.equals(panel.selectedOrigin) || nodeId.equals(panel.selectedDestination)) continue;

            Point2D.Double pos = coordToScreen(panel.idToCoordData.get(nodeId));
            if (pos != null && pos.x >= -nodeSize && pos.x <= panel.getWidth() + nodeSize
                    && pos.y >= -nodeSize && pos.y <= panel.getHeight() + nodeSize) {
                g2d.fillOval((int) pos.x - nodeSize / 2, (int) pos.y - nodeSize / 2, nodeSize, nodeSize);
                g2d.setColor(Color.BLACK);
                g2d.drawOval((int) pos.x - nodeSize / 2, (int) pos.y - nodeSize / 2, nodeSize, nodeSize);
                g2d.setColor(Color.WHITE);
            }
        }
    }

    private Point2D.Double coordToScreen(String coord) {
        if (coord == null) return null;

        String[] parts = coord.split(" ");
        if (parts.length != 2) return null;

        try {
            double lon = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);

            if (lon < -58.7 || lon > -58.3 || lat < -34.8 || lat > -34.4) return null;

            double x = (lon - panel.minLon) * panel.scale + panel.offsetX;
            double y = (panel.maxLat - lat) * panel.scale + panel.offsetY;
            return new Point2D.Double(x, y);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
