package com.example.ridesharing;

public class RideRequest {
    public String pickup;
    public String destination;
    public String status;
    public String name;
    public long timestamp;

    public RideRequest() {
    }

    public RideRequest(String pickup, String destination, String status, String name, long timestamp) {
        this.pickup = pickup;
        this.destination = destination;
        this.status = status;
        this.timestamp = timestamp;
        this.name = name;
    }
}

