package com.example.gearup.builders;

import com.example.gearup.models.User;
import com.google.firebase.Timestamp;

public class AdminBuilder implements IUserBuilder {
    private User user;

    public AdminBuilder() {
        this.user = new User();
        this.user.setBlocked(false);
        this.user.setIsApproved(true);
        this.user.setPoints(null);
    }

    @Override
    public void setUid(String uid) {
        user.setUid(uid);
    }

    @Override
    public void setFullName(String fullName) {
        user.setFullName(fullName);
    }

    @Override
    public void setEmail(String email) {
        user.setEmail(email);
    }

    @Override
    public void setPhoneNumber(String phoneNumber) {
        user.setPhoneNumber(phoneNumber);
    }

    @Override
    public void setUserType(String userType) {
        user.setUserType("admin");
    }

    @Override
    public void setLicenseDocument(String licenseDocument) {
    }

    @Override
    public void setPoints(Integer points) {
    }

    @Override
    public void setCreatedAt(Timestamp createdAt) {
        user.setCreatedAt(createdAt);
    }

    @Override
    public void setImgUrl(String imgUrl) {
        user.setImgUrl(imgUrl);
    }

    @Override
    public void setIsApproved(boolean isapproved) { // Added to fix the error
        user.setIsApproved(isapproved);
    }

    @Override
    public User build() {
        return user;
    }
}