package com.example.gearup.models;

import com.google.firebase.Timestamp;

public class User {
    protected String uid;
    protected String fullName;
    protected String email;
    private String fcmToken;
    protected String phoneNumber;
    protected String userType;
    protected Timestamp createdAt;
    protected String imgUrl;
    protected boolean blocked;
    protected boolean isApproved;
    protected String licenseDocument;
    protected Integer points;

    public User() {
        this.blocked = false;
        this.isApproved = true;
        this.points = 0;
    }

    public User(String uid, String fullName, String email, String phoneNumber, String userType, Timestamp createdAt, String imgUrl, boolean blocked) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.userType = userType;
        this.createdAt = createdAt;
        this.imgUrl = imgUrl;
        this.blocked = blocked;
        this.isApproved = !userType.equals("manager");
        this.points = userType.equals("customer") ? 0 : null;
    }

    // Getters and setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public String getImgUrl() { return imgUrl; }
    public void setImgUrl(String imgUrl) { this.imgUrl = imgUrl; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public boolean isApproved() { return isApproved; }
    public void setIsApproved(boolean isApproved) { this.isApproved = isApproved; }
    public String getLicenseDocument() { return licenseDocument; }
    public void setLicenseDocument(String licenseDocument) { this.licenseDocument = licenseDocument; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}