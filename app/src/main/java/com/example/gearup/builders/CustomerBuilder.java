package com.example.gearup.builders;

import com.example.gearup.models.User;
import com.google.firebase.Timestamp;

public class CustomerBuilder implements IUserBuilder {
    private User user;

    public CustomerBuilder() {
        this.user = new User();
        this.user.setBlocked(false);
        this.user.setIsApproved(true);
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
        user.setUserType(userType);
        if ("manager".equals(userType)) {
            user.setIsApproved(false);
        }
    }

    @Override
    public void setLicenseDocument(String licenseDocument) {
        user.setLicenseDocument(licenseDocument);
    }

    @Override
    public void setPoints(Integer points) {
        user.setPoints(points);
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
    public void setIsApproved(boolean isApproved) { // Added to implement the interface
        user.setIsApproved(isApproved);
    }

    @Override
    public User build() {
        return user;
    }
}