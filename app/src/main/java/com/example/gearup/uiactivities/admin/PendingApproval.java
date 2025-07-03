package com.example.gearup.uiactivities.admin;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class PendingApproval {
    private String id;
    private String fullName;
    private String email;
    private String userType;
    private boolean isApproved;
    private String aadharDocument;
    private String bankAccount;
    private String businessAddress;
    private String ifscCode;
    private String licenseDocument;
    private GeoPoint location;
    private String locationString;
    private String panDocument;
    private String passbookImage;
    private String phone;
    private boolean blocked;
    private String identityDocument; // Added for mechanic
    private String photo; // Added for mechanic
    private double rating; // Added for mechanic
    @ServerTimestamp
    private Date createdAt;

    // Required for Firestore deserialization
    public PendingApproval() {
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public String getAadharDocument() {
        return aadharDocument;
    }

    public void setAadharDocument(String aadharDocument) {
        this.aadharDocument = aadharDocument;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getLicenseDocument() {
        return licenseDocument;
    }

    public void setLicenseDocument(String licenseDocument) {
        this.licenseDocument = licenseDocument;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public String getLocationString() {
        return locationString;
    }

    public void setLocationString(String locationString) {
        this.locationString = locationString;
    }

    public String getPanDocument() {
        return panDocument;
    }

    public void setPanDocument(String panDocument) {
        this.panDocument = panDocument;
    }

    public String getPassbookImage() {
        return passbookImage;
    }

    public void setPassbookImage(String passbookImage) {
        this.passbookImage = passbookImage;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getIdentityDocument() {
        return identityDocument;
    }

    public void setIdentityDocument(String identityDocument) {
        this.identityDocument = identityDocument;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}