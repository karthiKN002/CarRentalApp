package com.example.gearup.models;

import com.google.firebase.Timestamp;

public class ManagerUser extends User {
    public ManagerUser() {
        super();
    }

    public ManagerUser(String uid, String fullName, String email, String phoneNumber, String imgUrl, boolean blocked, Timestamp createdAt) {
        super(uid, fullName, email, phoneNumber, "manager", createdAt, imgUrl, blocked);
    }
}