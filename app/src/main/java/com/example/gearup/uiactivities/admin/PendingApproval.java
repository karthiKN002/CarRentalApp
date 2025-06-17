package com.example.gearup.uiactivities.admin;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.PropertyName;

public class PendingApproval {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    @PropertyName("licenseDocument")
    private String licenseDocument;
    @PropertyName("storeVideo")
    private String storeVideo;
    private String uid;
    private Object createdAt; // Use Object for FieldValue.serverTimestamp()
    private Boolean blocked;
    private String userType;
    private Boolean isApproved;
    private Integer points;

    public PendingApproval() {}

    public boolean hasDocument() {
        return licenseDocument != null && !licenseDocument.isEmpty();
    }

    public boolean hasVideo() {
        return storeVideo != null && !storeVideo.isEmpty();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    @PropertyName("licenseDocument")
    public String getDocumentUrl() { return licenseDocument; }
    @PropertyName("licenseDocument")
    public void setDocumentUrl(String documentUrl) { this.licenseDocument = documentUrl; }
    @PropertyName("storeVideo")
    public String getStoreVideo() { return storeVideo; }
    @PropertyName("storeVideo")
    public void setStoreVideo(String storeVideo) { this.storeVideo = storeVideo; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public Object getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }
    public Boolean getBlocked() { return blocked; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
}