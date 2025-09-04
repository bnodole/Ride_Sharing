package com.example.ridesharing;

public class User {
    public String name, phone, password, role;

    public User() {} // Needed for Firebase

    public User(String name, String phone, String password, String role) {
        this.name = name;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }
}
