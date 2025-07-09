package gui;

import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.Map;
import models.*;

public class MapInteractionHandler {
    private final MapPanel panel;
    private int lastMouseX, lastMouseY;
    private boolean isDragging = false;
    private Integer lastHoveredNode = null;

    public MapInteractionHandler(MapPanel panel) {
        this.panel = panel;
    }

    public MouseAdapter getMouseAdapter() {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                isDragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    panel.offsetX += e.getX() - lastMouseX;
                    panel.offsetY += e.getY() - lastMouseY;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    panel.repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                handleMapClick(e.getX(), e.getY());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                showNodeInfo(e.getX(), e.getY());
            }
        };
    }

    public MouseWheelListener getWheelListener() {
        return e -> {
            double factor = 1.15;
            double oldScale = panel.scale;
            double mouseX = e.getX(), mouseY = e.getY();

            panel.scale *= (e.getWheelRotation() < 0) ? factor : 1 / factor;

            double ratio = panel.scale / oldScale;
            panel.offsetX = mouseX - (mouseX - panel.offsetX) * ratio;
            panel.offsetY = mouseY - (mouseY - panel.offsetY) * ratio;

            panel.repaint();
        };
    }

    private void handleMapClick(double x, double y) {
        Integer nodeId = screenToNodeId(x, y);

        if (nodeId != null) {
            if (panel.selectedOrigin == null) {
                panel.selectedOrigin = nodeId;
                panel.statusLabel.setText("Origin selected: Node " + nodeId + ". Click another node for destination.");
            } else if (panel.selectedDestination == null && !nodeId.equals(panel.selectedOrigin)) {
                panel.selectedDestination = nodeId;
                panel.statusLabel.setText("Destination selected: Node " + nodeId + ". Press 'Find Route' to calculate path.");
            } else {
                // Reset and start over
                panel.selectedOrigin = nodeId;
                panel.selectedDestination = null;
                panel.currentRoute.clear();
                panel.statusLabel.setText("Origin selected: Node " + nodeId + ". Click another node for destination.");
            }

            // Activar botones si ambos nodos están seleccionados
            boolean bothSelected = panel.selectedOrigin != null && panel.selectedDestination != null;
            if (panel.findRouteButton != null) panel.findRouteButton.setEnabled(bothSelected);
            if (panel.findRouteAStarButton != null) panel.findRouteAStarButton.setEnabled(bothSelected);
            if (panel.findRouteALTButton != null) panel.findRouteALTButton.setEnabled(bothSelected);

            panel.repaint();
        }
    }

    private void showNodeInfo(double x, double y) {
        Integer nodeId = screenToNodeId(x, y);

        if (nodeId != null && !nodeId.equals(lastHoveredNode)) {
            Node node = panel.graphData[nodeId];
            String coord = panel.idToCoordData.get(nodeId);
            String info = String.format("Node %d: %s\nOut edges: %d, In edges: %d",
                    nodeId, coord, node.outEdges.size(), node.inEdges.size());

            panel.setToolTipText(info);
            lastHoveredNode = nodeId;
        } else if (nodeId == null) {
            panel.setToolTipText(null);
            lastHoveredNode = null;
        }
    }

    private Integer screenToNodeId(double screenX, double screenY) {
        double threshold = 10.0;
        Integer nearestNode = null;
        double minDistance = threshold;

        for (Map.Entry<Integer, String> entry : panel.idToCoordData.entrySet()) {
            Point2D.Double pos = CoordTransformer.coordToScreen(
                entry.getValue(),
                panel.minLon,
                panel.maxLat,
                panel.scale,
                panel.offsetX,
                panel.offsetY
            );

            if (pos != null) {
                double distance = pos.distance(screenX, screenY);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestNode = entry.getKey();
                }
            }
        }

        return nearestNode;
    }
}
