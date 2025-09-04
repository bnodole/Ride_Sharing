package com.example.ridesharing;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DashboardDriver extends AppCompatActivity {

    private ListView requestList;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> requests;
    private ArrayList<HashMap<String, Object>> requestDetails;
    private MapView map;

    private DatabaseReference requestRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_dashboard_driver);

        requestList = findViewById(R.id.requestList);
        map = findViewById(R.id.driverMap);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(14.0);

        requests = new ArrayList<>();
        requestDetails = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, requests);
        requestList.setAdapter(adapter);

        requestRef = FirebaseDatabase.getInstance().getReference("ride_requests");

        // Fetch ride requests
        requestRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requests.clear();
                requestDetails.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    HashMap<String, Object> request = (HashMap<String, Object>) snap.getValue();
                    if (request == null) continue;

                    String status = String.valueOf(request.get("status"));
                    if ("accepted".equals(status)) continue;

                    request.put("id", snap.getKey());
                    requestDetails.add(request);

                    String user = String.valueOf(request.get("name"));
                    String pickup = String.valueOf(request.get("pickup"));
                    String destination = String.valueOf(request.get("destination"));

                    requests.add("User: " + user + "\nFrom: " + pickup + "\nTo: " + destination +"\nPrice: 500");
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardDriver.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
            }
        });

        // Accept request on tap
        requestList.setOnItemClickListener((parent, view, position, id) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardDriver.this);
            builder.setTitle("Accept Ride?");
            builder.setMessage("Do you want to accept this ride?");
            builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    HashMap<String, Object> selected = requestDetails.get(position);
                    String status = String.valueOf(selected.get("status"));
                    String requestId = String.valueOf(selected.get("id"));

                    if ("accepted".equals(status)) {
                        Toast.makeText(DashboardDriver.this, "Already accepted", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String pickup = String.valueOf(selected.get("pickup"));
                    String destination = String.valueOf(selected.get("destination"));

                    searchLocation(pickup, new LocationCallback() {
                        @Override
                        public void onLocationFound(double pickupLat, double pickupLon) {
                            searchLocation(destination, new LocationCallback() {
                                @Override
                                public void onLocationFound(double destLat, double destLon) {
                                    runOnUiThread(() -> {
                                        try {
                                            map.getOverlays().clear();

                                            GeoPoint pickupPoint = new GeoPoint(pickupLat, pickupLon);
                                            GeoPoint destPoint = new GeoPoint(destLat, destLon);

                                            // 1. Add Pickup Marker
                                            Marker pickupMarker = new Marker(map);
                                            pickupMarker.setPosition(pickupPoint);
                                            pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                            pickupMarker.setTitle("Pickup");
                                            map.getOverlays().add(pickupMarker);

                                            // 2. Add Destination Marker
                                            Marker destMarker = new Marker(map);
                                            destMarker.setPosition(destPoint);
                                            destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                            destMarker.setTitle("Destination");
                                            map.getOverlays().add(destMarker);

                                            //Fetch Route
                                            fetchRoute(pickupLat, pickupLon, destLat, destLon);




                                            fetchRouteFromORS(pickupLat, pickupLon, destLat, destLon);
                                            map.getController().setCenter(pickupPoint);
                                            map.invalidate();

                                            requestRef.child(requestId).child("status").setValue("accepted");
                                            Toast.makeText(DashboardDriver.this, "Ride accepted!", Toast.LENGTH_SHORT).show();
                                            requestList.setEnabled(false);

                                        } catch (Exception e) {
                                            Toast.makeText(DashboardDriver.this, "Error displaying map data", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }

                                @Override
                                public void onLocationError(String message) {
                                    Toast.makeText(DashboardDriver.this, "Destination error: " + message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onLocationError(String message) {
                            Toast.makeText(DashboardDriver.this, "Pickup error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            builder.setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        // Delete request on long press
        requestList.setOnItemLongClickListener((parent, view, position, id) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardDriver.this);
            builder.setTitle("Delete Request?");
            builder.setMessage("Do you want to delete this Ride Request?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String requestId = String.valueOf(requestDetails.get(position).get("id"));
                    requestRef.child(requestId).removeValue();
                    Toast.makeText(DashboardDriver.this, "Request deleted", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            return true;
        });
    }

    private void searchLocation(String query, LocationCallback callback) {
        new Thread(() -> {
            try {
                String url = "https://nominatim.openstreetmap.org/search?q=" +
                        URLEncoder.encode(query, "UTF-8") + "&format=json&limit=1";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("User-Agent", "OSMDroidDemo");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONArray jsonArray = new JSONArray(result.toString());
                if (jsonArray.length() > 0) {
                    JSONObject obj = jsonArray.getJSONObject(0);
                    double lat = obj.getDouble("lat");
                    double lon = obj.getDouble("lon");

                    runOnUiThread(() -> callback.onLocationFound(lat, lon));
                } else {
                    runOnUiThread(() -> callback.onLocationError("No results found"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> callback.onLocationError("Error during search"));
            }
        }).start();
    }

    public void fetchRoute(double startLat, double startLon, double endLat, double endLon){
        // A* pathfinding (hidden, does not affect ORS or UI)
        new Thread(() -> {
            try {
                KathmanduGraph.GraphNode startNode = KathmanduGraph.getNodeByName("Ratnapark");
                KathmanduGraph.GraphNode goalNode = KathmanduGraph.getNodeByName("Baneshwor");

                List<KathmanduGraph.GraphNode> localPath = AStarPathFinder.findPathAStar(startNode, goalNode);

                double localDistance = 0;
                for (int i = 0; i < localPath.size() - 1; i++) {
                    KathmanduGraph.GraphNode a = localPath.get(i);
                    KathmanduGraph.GraphNode b = localPath.get(i + 1);
                    // ✅ reuse AStarPathFinder's heuristic method
                    localDistance += AStarPathFinder.heuristic(a, b);
                }

                Log.d("ASTAR_CHECK", "A* path nodes: " + localPath.size() + " Distance: " + localDistance + " km");

            } catch (Exception e) {
                Log.e("ASTAR_ERROR", "A* failed: " + e.getMessage());
            }
        }).start();

    }
    private void fetchRouteFromORS(double startLat, double startLon, double endLat, double endLon) {
        new Thread(() -> {
            try {
                String url = "https://api.openrouteservice.org/v2/directions/driving-car/geojson";
                String jsonInput = "{\n" +
                        "  \"coordinates\": [[ " + startLon + ", " + startLat + " ], [ " + endLon + ", " + endLat + " ]]\n" +
                        "}";

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjQyZDAyZTY4YmMyYjRjM2Q5YWVkZmY2NWQ4NGU3ODA3IiwiaCI6Im11cm11cjY0In0=");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.getOutputStream().write(jsonInput.getBytes());

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONObject response = new JSONObject(result.toString());
                JSONArray coordinates = response.getJSONArray("features")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                List<GeoPoint> routePoints = new ArrayList<>();
                for (int i = 0; i < coordinates.length(); i++) {
                    JSONArray point = coordinates.getJSONArray(i);
                    double lon = point.getDouble(0);
                    double lat = point.getDouble(1);
                    routePoints.add(new GeoPoint(lat, lon));
                }

                runOnUiThread(() -> {
                    Polyline routeLine = new Polyline();
                    routeLine.setPoints(routePoints);
                    routeLine.setTitle("Route");
                    map.getOverlays().add(routeLine);
                    map.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(DashboardDriver.this, "Failed to get route", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.profile){
            Intent intent = new Intent(DashboardDriver.this, Profile.class);
            intent.putExtra("phone", getIntent().getStringExtra("phone"));
            startActivity(intent);
        } else if (item.getItemId() == R.id.logout) {
            createPopUp(R.id.logout);
        } else if (item.getItemId() == R.id.exit) {
            createPopUp(R.id.exit);
        }
        return true;
    }

    public void createPopUp(int button_id){
        String messageString = null,titleString = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(DashboardDriver.this);
        if (button_id == R.id.logout){
            titleString = "Log Out";
            messageString = "Do you want to Log Out?";
        }
        if (button_id == R.id.exit){
            titleString = "Exit";
            messageString = "Do you want to Exit App?";
        }
        if (button_id == -1){
            titleString = "Delete Request";
            messageString = "Do you want to Delete Request?";
        }
        builder.setTitle(titleString);
        builder.setMessage(messageString);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (button_id == R.id.logout) {
                    Intent intent = new Intent(DashboardDriver.this, Index.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else if(button_id == R.id.exit) {
                    finish();
                } else if (button_id == -1) {

                }

            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}