package com.example.gearup.models;

import com.example.gearup.states.car.CarAvailabilityState;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;

public class Car {
    private String id;
    private String model;
    private String brand;
    private String type;
    private int seats;
    private String location;
    private ArrayList<String> images;
    private double price;
    private float rating;
    private int ratingCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String managerId;
    @PropertyName("state")
    private CarAvailabilityState currentState;
    private String description;
    private ArrayList<String> ratedBy;

    // Deprecated fields
    @Deprecated
    private double latitude;
    @Deprecated
    private double longitude;

    // Default constructor (required by Firebase)
    public Car() {
        this.images = new ArrayList<>();
        this.ratedBy = new ArrayList<>();
    }

    // Custom constructor for creating a new car
    public Car(String model, String brand, int seats, String location, double price) {
        this.model = model;
        this.brand = brand;
        this.seats = seats;
        this.location = location;
        this.price = price;
        this.images = new ArrayList<>();
        this.ratedBy = new ArrayList<>();
        this.rating = 0f;
        this.ratingCount = 0;
        this.currentState = CarAvailabilityState.AVAILABLE;
    }

    // New methods for adapter compatibility
    public String getImageUrl() {
        return images != null && !images.isEmpty() ? images.get(0) : "";
    }

    public double getPricePerDay() {
        return price;
    }

    public boolean isAvailable() {
        return currentState == CarAvailabilityState.AVAILABLE;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ArrayList<String> getImages() {
        return images;
    }

    public void setImages(ArrayList<String> images) {
        this.images = images;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    @PropertyName("state")
    public CarAvailabilityState getCurrentState() {
        return currentState;
    }

    @PropertyName("state")
    public void setCurrentState(CarAvailabilityState currentState) {
        this.currentState = currentState;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<String> getRatedBy() {
        return ratedBy;
    }

    public void setRatedBy(ArrayList<String> ratedBy) {
        this.ratedBy = ratedBy;
    }

    // Deprecated getters and setters
    @Deprecated
    public double getLatitude() {
        return latitude;
    }

    @Deprecated
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Deprecated
    public double getLongitude() {
        return longitude;
    }

    @Deprecated
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}