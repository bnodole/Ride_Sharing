package com.example.ridesharing;

import java.util.*;

public class AStarPathFinder {

    public static List<KathmanduGraph.GraphNode> findPathAStar(KathmanduGraph.GraphNode start,
                                                               KathmanduGraph.GraphNode goal) {
        PriorityQueue<NodeRecord> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<Integer, NodeRecord> closedSet = new HashMap<>();
        Map<Integer, NodeRecord> allNodes = new HashMap<>();

        // Start node
        NodeRecord startRecord = new NodeRecord(start, null, 0, heuristic(start, goal));
        openSet.add(startRecord);
        allNodes.put((int) start.id, startRecord);

        while (!openSet.isEmpty()) {
            NodeRecord current = openSet.poll();

            // Goal check
            if (current.node.id == goal.id) {
                return reconstructPath(current);
            }

            closedSet.put((int) current.node.id, current);

            // Expand neighbors
            for (KathmanduGraph.GraphEdge edge : current.node.neighbors) {
                KathmanduGraph.GraphNode neighbor = edge.to;
                double tentativeG = current.g + edge.cost;

                NodeRecord neighborRecord = allNodes.get(neighbor.id);

                if (neighborRecord == null) {
                    double h = heuristic(neighbor, goal);
                    neighborRecord = new NodeRecord(neighbor, current, tentativeG, h);
                    allNodes.put((int) neighbor.id, neighborRecord);
                    openSet.add(neighborRecord);
                } else if (tentativeG < neighborRecord.g) {
                    neighborRecord.g = tentativeG;
                    neighborRecord.parent = current;
                    openSet.remove(neighborRecord); // re-balance priority queue
                    openSet.add(neighborRecord);
                }
            }
        }

        // No path found
        return Collections.emptyList();
    }

    protected static double heuristic(KathmanduGraph.GraphNode a, KathmanduGraph.GraphNode b) {
        // Haversine formula for distance on Earth
        double R = 6371; // Earth radius in km
        double lat1 = Math.toRadians(a.lat);
        double lon1 = Math.toRadians(a.lon);
        double lat2 = Math.toRadians(b.lat);
        double lon2 = Math.toRadians(b.lon);

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double h = Math.sin(dlat / 2) * Math.sin(dlat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dlon / 2) * Math.sin(dlon / 2);

        return 2 * R * Math.asin(Math.sqrt(h));
    }

    private static List<KathmanduGraph.GraphNode> reconstructPath(NodeRecord node) {
        List<KathmanduGraph.GraphNode> path = new ArrayList<>();
        NodeRecord current = node;
        while (current != null) {
            path.add(0, current.node);
            current = current.parent;
        }
        return path;
    }

    private static class NodeRecord {
        KathmanduGraph.GraphNode node;
        NodeRecord parent;
        double g; // cost so far
        double h; // heuristic estimate
        double f; // total = g + h

        NodeRecord(KathmanduGraph.GraphNode node, NodeRecord parent, double g, double h) {
            this.node = node;
            this.parent = parent;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }
    }
}
