package com.example.gearup.factories;

import com.example.gearup.builders.AdminBuilder;
import com.example.gearup.builders.CustomerBuilder;
import com.example.gearup.builders.IUserBuilder;
import com.example.gearup.builders.UserEngineer;
import com.example.gearup.models.User;
import com.google.firebase.Timestamp;

public class UserFactory {
    public static User createUser(String userType, String uid, String fullName, String email, String phoneNumber,
                                  String licenseDocument, Timestamp createdAt, String imgUrl, boolean approved) {
        IUserBuilder userBuilder;

        if ("admin".equalsIgnoreCase(userType)) {
            userBuilder = new AdminBuilder();
            userBuilder.setIsApproved(approved); // Set approved state
        } else {
            userBuilder = new CustomerBuilder();
            userBuilder.setLicenseDocument(licenseDocument);
            userBuilder.setPoints("customer".equalsIgnoreCase(userType) ? 0 : null);
            userBuilder.setIsApproved(approved); // Set approved state
        }

        userBuilder.setUid(uid);
        userBuilder.setFullName(fullName != null ? fullName.trim() : "Unknown");
        userBuilder.setEmail(email);
        userBuilder.setPhoneNumber(phoneNumber);
        userBuilder.setUserType(userType);
        userBuilder.setCreatedAt(createdAt);
        userBuilder.setImgUrl(imgUrl);

        UserEngineer userEngineer = new UserEngineer(userBuilder);
        userEngineer.constructUser();

        return userEngineer.getUser();
    }
}