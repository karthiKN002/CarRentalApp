package com.example.gearup.uiactivities.admin;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;

public class CarPendingApproval {
    private String id;
    private String model;
    private String brand;
    private String location;
    private int seats;
    private double price;
    private String description;
    private ArrayList<String> images;
    private String fcDocument;
    private String rcDocument;
    private String insuranceDocument;
    private String pucDocument;
    private String managerId;
    private double latitude;
    private double longitude;
    private ArrayList<String> ratedBy;
    private double rating;
    private int ratingCount;
    private String state;
    @ServerTimestamp
    private Date createdAt;

    // Required for Firestore deserialization
    public CarPendingApproval() {
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ArrayList<String> getImages() { return images; }
    public void setImages(ArrayList<String> images) { this.images = images; }
    public String getFcDocument() { return fcDocument; }
    public void setFcDocument(String fcDocument) { this.fcDocument = fcDocument; }
    public String getRcDocument() { return rcDocument; }
    public void setRcDocument(String rcDocument) { this.rcDocument = rcDocument; }
    public String getInsuranceDocument() { return insuranceDocument; }
    public void setInsuranceDocument(String insuranceDocument) { this.insuranceDocument = insuranceDocument; }
    public String getPucDocument() { return pucDocument; }
    public void setPucDocument(String pucDocument) { this.pucDocument = pucDocument; }
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public ArrayList<String> getRatedBy() { return ratedBy; }
    public void setRatedBy(ArrayList<String> ratedBy) { this.ratedBy = ratedBy; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}