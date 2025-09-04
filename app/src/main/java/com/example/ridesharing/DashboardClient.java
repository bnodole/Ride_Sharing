package com.example.ridesharing;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

public class DashboardClient extends AppCompatActivity {

    private MapView map;
    private EditText searchEdit,ownLocationEditText;
    private Button searchBtn,requestBtn;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentRequestId;

    private static final int LOCATION_PERMISSION_CODE = 1;

    private static final double NORTH = 27.80;
    private static final double SOUTH = 27.60;
    private static final double EAST = 85.55;
    private static final double WEST = 85.25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_dashboard_client);

        map = findViewById(R.id.map);
        searchEdit = findViewById(R.id.destination);
        searchBtn = findViewById(R.id.searchBtn);
        ownLocationEditText = findViewById(R.id.ownLocation);
        requestBtn = findViewById(R.id.requestBtn);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        map.setScrollableAreaLimitLatitude(NORTH, SOUTH, 0);
        map.setScrollableAreaLimitLongitude(WEST, EAST, 0);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setMinZoomLevel(12.0);
        map.setMaxZoomLevel(19.0);
        map.getController().setZoom(15.0);

        // Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            getCurrentLocation();
        }



        // Location Overlay
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        map.getOverlays().add(locationOverlay);


        // Search Button Click
        searchBtn.setOnClickListener(v -> {
            String query = searchEdit.getText().toString().trim();
            if (!query.isEmpty()) {
                searchLocation(query);
            }
        });

        requestBtn.setOnClickListener(v -> {
            String pickup = ownLocationEditText.getText().toString().trim();
            String destination = searchEdit.getText().toString().trim();

            if (pickup.isEmpty() || destination.isEmpty()) {
                Toast.makeText(this, "Please provide both pickup and destination.", Toast.LENGTH_SHORT).show();
                return;
            }

            sendRideRequestToFirebase(pickup, destination);
        });
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
            Intent intent = new Intent(DashboardClient.this, Profile.class);
            intent.putExtra("phone", getIntent().getStringExtra("phone"));
            startActivity(intent);
        } else if (item.getItemId() == R.id.logout) {
            createPopUp(R.id.logout);
        } else if (item.getItemId() == R.id.exit) {
            createPopUp(R.id.exit);
        }
        return true;
    }

    private void searchLocation(String query) {
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

                    double NORTH = 27.80;
                    double SOUTH = 27.60;
                    double EAST = 85.55;
                    double WEST = 85.25;

                    if (lat >= SOUTH && lat <= NORTH && lon >= WEST && lon <= EAST) {
                        runOnUiThread(() -> {
                            GeoPoint point = new GeoPoint(lat, lon);
                            Marker marker = new Marker(map);
                            marker.setPosition(point);
                            marker.setTitle("Search Result");
                            map.getOverlays().add(marker);
                            map.getController().setZoom(18.0);
                            map.getController().animateTo(point);
                            map.invalidate();
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(this, "Search restricted to Kathmandu Valley only!", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "No results found!", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error during search", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }


    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                getAddressFromLocation(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(this, "Couldn't get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAddressFromLocation(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            GeoPoint point = new GeoPoint(lat, lon);
            Marker marker = new Marker(map);
            marker.setPosition(point);
            marker.setTitle("Search Result");
            map.getOverlays().add(marker);
            map.getController().setZoom(18.0);
            map.getController().animateTo(point);
            map.invalidate();
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0); // Full address
                ownLocationEditText.setText(address);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to get address", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendRideRequestToFirebase(String pickup, String destination) {
        String phone = getIntent().getStringExtra("phone");

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(phone);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);

                    // Now save ride request to Firebase
                    DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("ride_requests");
                    String requestId = rideRef.push().getKey();
                    currentRequestId = requestId;
                    listenForRideAcceptance();

                    RideRequest rideRequest = new RideRequest(
                            pickup, destination, "pending",name, System.currentTimeMillis()
                    );

                    if (requestId != null) {
                        rideRef.child(requestId).setValue(rideRequest)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getApplicationContext(), "Ride Requested", Toast.LENGTH_SHORT).show();
                                    requestBtn.setEnabled(false);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getApplicationContext(), "Failed to request ride", Toast.LENGTH_SHORT).show()
                                );
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to fetch user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForRideAcceptance() {
        if (currentRequestId == null) return;

        DatabaseReference rideRef = FirebaseDatabase.getInstance()
                .getReference("ride_requests")
                .child(currentRequestId);

        rideRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    if ("accepted".equalsIgnoreCase(status)) {
                        String driverName = snapshot.child("driverName").getValue(String.class);
                        Toast.makeText(getApplicationContext(),
                                "Your ride has been accepted by " + driverName,
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }


    public void createPopUp(int button_id){
        String messageString = null,titleString = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(DashboardClient.this);
        if (button_id == R.id.logout){
            titleString = "Log Out";
            messageString = "Do you want to Log Out?";
        }
        if (button_id == R.id.exit){
            titleString = "Exit";
            messageString = "Do you want to Exit App?";
        }
        builder.setTitle(titleString);
        builder.setMessage(messageString);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (button_id == R.id.logout) {
                    Intent intent = new Intent(DashboardClient.this, Index.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    finish();
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
