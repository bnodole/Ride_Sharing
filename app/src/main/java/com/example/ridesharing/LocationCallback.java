package com.example.ridesharing;

import java.util.HashMap;

public interface LocationCallback {
    void onLocationFound(double lat, double lon);
    void onLocationError(String message);
}

