package com.example.gearup.builders;

import com.example.gearup.models.User;
import com.google.firebase.Timestamp;

public interface IUserBuilder {
    void setUid(String uid);
    void setFullName(String fullName);
    void setEmail(String email);
    void setPhoneNumber(String phoneNumber);
    void setUserType(String userType);
    void setLicenseDocument(String licenseDocument);
    void setPoints(Integer points);
    void setCreatedAt(Timestamp createdAt);
    void setImgUrl(String imgUrl);
    void setIsApproved(boolean isapproved); // Updated to match existing builders
    User build();
}