package com.example.gearup.builders;

import com.example.gearup.models.User;

public class UserEngineer {
    private IUserBuilder userBuilder;

    public UserEngineer(IUserBuilder userBuilder) {
        this.userBuilder = userBuilder;
    }

    public void constructUser() {
    }

    public User getUser() {
        return userBuilder.build();
    }
}