package com.example.ridesharing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class KathmanduGraph {

    public static class GraphNode {
        public long id;
        public double lat, lon;
        public List<GraphEdge> neighbors = new ArrayList<>();
        public GraphNode(long id, double lat, double lon) { this.id = id; this.lat = lat; this.lon = lon; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            GraphNode graphNode = (GraphNode) obj;
            return id == graphNode.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    public static class GraphEdge {
        public GraphNode target;
        public double cost;
        public GraphNode to;

        public GraphEdge(GraphNode target, double cost) { this.target = target; this.cost = cost; }
    }

    public interface GraphLoadCallback {
        void onGraphLoaded();
        void onGraphLoadProgress(String message);
        void onGraphLoadError(String error);
    }

    public static Map<Long, GraphNode> nodes = new HashMap<>();
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    public boolean isLoaded = false;

    private final List<GraphNode> nodeList = new ArrayList<>();
    private final Map<String, List<GraphNode>> spatialGrid = new HashMap<>();

    public KathmanduGraph(Context context) { KathmanduGraph.context = context; }

    public void loadGraphFromJSON(GraphLoadCallback callback) {
        new Thread(() -> {
            try {
                callback.onGraphLoadProgress("Loading map data...");

                InputStream input = context.getAssets().open("kathmandu_graph.json");
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                // Process JSON in streaming mode to avoid memory issues
                processJSONStream(reader, callback);

                callback.onGraphLoadProgress("Optimizing graph...");
                simplifyGraph();
                nodeList.addAll(nodes.values());
                buildSpatialIndex();

                isLoaded = true;
                Log.d("KathmanduGraph", "Graph loaded with " + nodes.size() + " nodes and " +
                        countTotalEdges() + " edges");
                callback.onGraphLoaded();

            } catch (Exception e) {
                Log.e("KathmanduGraph", "Load error: " + e.getMessage(), e);
                callback.onGraphLoadError("Load failed: " + e.getMessage());
            }
        }).start();
    }

    private void processJSONStream(BufferedReader reader, GraphLoadCallback callback) throws Exception {
        String line;
        boolean inNodesSection = false;
        boolean inWaysSection = false;
        int nodeCount = 0;
        int wayCount = 0;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.contains("\"nodes\":")) {
                inNodesSection = true;
                inWaysSection = false;
                callback.onGraphLoadProgress("Processing nodes...");
                continue;
            }

            if (line.contains("\"ways\":")) {
                inNodesSection = false;
                inWaysSection = true;
                callback.onGraphLoadProgress("Processing ways...");
                continue;
            }

            if (inNodesSection) {
                if (line.equals("[") || line.equals("]")) continue;
                if (line.endsWith(",")) line = line.substring(0, line.length() - 1);

                try {
                    JSONObject nodeJson = new JSONObject(line);
                    long id = nodeJson.getLong("id");
                    double lat = nodeJson.getDouble("lat");
                    double lon = nodeJson.getDouble("lon");

                    GraphNode node = new GraphNode(id, lat, lon);
                    nodes.put(id, node);
                    nodeCount++;

                    if (nodeCount % 5000 == 0) {
                        callback.onGraphLoadProgress("Loaded " + nodeCount + " nodes");
                    }
                } catch (Exception e) {
                    Log.w("KathmanduGraph", "Skipping invalid node: " + e.getMessage());
                }
            }

            if (inWaysSection) {
                if (line.equals("[") || line.equals("]")) continue;
                if (line.endsWith(",")) line = line.substring(0, line.length() - 1);

                try {
                    JSONObject wayJson = new JSONObject(line);
                    JSONArray nodeIds = wayJson.getJSONArray("nodes");

                    for (int j = 0; j < nodeIds.length() - 1; j++) {
                        long nodeId1 = nodeIds.getLong(j);
                        long nodeId2 = nodeIds.getLong(j + 1);

                        GraphNode n1 = nodes.get(nodeId1);
                        GraphNode n2 = nodes.get(nodeId2);

                        if (n1 != null && n2 != null) {
                            double cost = distance(n1.lat, n1.lon, n2.lat, n2.lon);
                            n1.neighbors.add(new GraphEdge(n2, cost));
                            n2.neighbors.add(new GraphEdge(n1, cost));
                        }
                    }

                    wayCount++;
                    if (wayCount % 1000 == 0) {
                        callback.onGraphLoadProgress("Processed " + wayCount + " roads");
                    }
                } catch (Exception e) {
                    Log.w("KathmanduGraph", "Skipping invalid way: " + e.getMessage());
                }
            }
        }

        reader.close();
        Log.d("KathmanduGraph", "Finished processing: " + nodeCount + " nodes, " + wayCount + " ways");
    }

    private int countTotalEdges() {
        int totalEdges = 0;
        for (GraphNode node : nodes.values()) {
            totalEdges += node.neighbors.size();
        }
        return totalEdges;
    }

    private void simplifyGraph() {
        List<Long> nodesToRemove = new ArrayList<>();

        for (GraphNode node : new ArrayList<>(nodes.values())) {
            if (node.neighbors.size() == 2) {
                GraphEdge edge1 = node.neighbors.get(0);
                GraphEdge edge2 = node.neighbors.get(1);

                double newCost = edge1.cost + edge2.cost;

                addEdgeIfNotExists(edge1.target, edge2.target, newCost);
                addEdgeIfNotExists(edge2.target, edge1.target, newCost);

                removeNodeFromNeighbors(node);
                nodesToRemove.add(node.id);
            }
        }

        for (Long nodeId : nodesToRemove) {
            nodes.remove(nodeId);
        }
    }

    private void addEdgeIfNotExists(GraphNode from, GraphNode to, double cost) {
        for (GraphEdge edge : from.neighbors) {
            if (edge.target.id == to.id) {
                return;
            }
        }
        from.neighbors.add(new GraphEdge(to, cost));
    }

    private void removeNodeFromNeighbors(GraphNode nodeToRemove) {
        for (GraphNode node : nodes.values()) {
            node.neighbors.removeIf(edge -> edge.target.id == nodeToRemove.id);
        }
    }

    private void buildSpatialIndex() {
        spatialGrid.clear();
        for (GraphNode node : nodes.values()) {
            String gridKey = getGridKey(node.lat, node.lon);
            if (!spatialGrid.containsKey(gridKey)) {
                spatialGrid.put(gridKey, new ArrayList<>());
            }
            Objects.requireNonNull(spatialGrid.get(gridKey)).add(node);
        }
    }

    private String getGridKey(double lat, double lon) {
        double gridSize = 0.002;
        int latIndex = (int) (lat / gridSize);
        int lonIndex = (int) (lon / gridSize);
        return latIndex + "," + lonIndex;
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ---------------------------
    // NEW FUNCTION: getNodeByName
    // ---------------------------
    public static GraphNode getNodeByName(String placeName) {
        try {
            Geocoder geocoder = new Geocoder(context);
            List<Address> addresses = geocoder.getFromLocationName(placeName, 1);

            if (addresses == null || addresses.isEmpty()) {
                Log.w("KathmanduGraph", "Geocoder: No results for " + placeName);
                return null;
            }

            double lat = addresses.get(0).getLatitude();
            double lon = addresses.get(0).getLongitude();

            // Find nearest graph node to these coordinates
            GraphNode nearest = null;
            double minDist = Double.MAX_VALUE;

            for (GraphNode node : nodes.values()) {
                double d = distance(lat, lon, node.lat, node.lon);
                if (d < minDist) {
                    minDist = d;
                    nearest = node;
                }
            }

            return nearest;

        } catch (Exception e) {
            Log.e("KathmanduGraph", "getNodeByName failed: " + e.getMessage(), e);
            return null; // Don’t crash app
        }
    }
}
