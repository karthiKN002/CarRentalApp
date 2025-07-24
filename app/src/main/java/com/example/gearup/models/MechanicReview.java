package com.example.gearup.models;

import com.google.firebase.Timestamp;

public class MechanicReview {
    private String reviewId;
    private String mechanicId;
    private String customerId;
    private String reviewText;
    private float rating;
    private Timestamp createdAt;

    public MechanicReview() {}

    public MechanicReview(String reviewId, String mechanicId, String customerId, String reviewText, float rating, Timestamp createdAt) {
        this.reviewId = reviewId;
        this.mechanicId = mechanicId;
        this.customerId = customerId;
        this.reviewText = reviewText;
        this.rating = rating;
        this.createdAt = createdAt;
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public String getMechanicId() { return mechanicId; }
    public void setMechanicId(String mechanicId) { this.mechanicId = mechanicId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}