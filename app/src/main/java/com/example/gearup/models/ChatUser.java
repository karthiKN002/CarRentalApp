package com.example.gearup.models;

import java.util.Date;

public class ChatUser {
    private String userId;
    private String name;
    private String email;
    private String carName;
    private long timestamp;

    public ChatUser(String userId, String name) {
        this.userId = userId;
        this.name = name;
        this.email = null;
        this.carName = "";
        this.timestamp = 0;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCarName() { return carName; }
    public void setCarName(String carName) { this.carName = carName; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}