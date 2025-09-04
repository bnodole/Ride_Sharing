package com.example.ridesharing;


public interface LocationCallback {
    void onLocationFound(double lat, double lon);
    void onLocationError(String message);
}

