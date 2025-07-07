package com.example.gearup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gearup.uiactivities.admin.AdminApprovalActivity;
import com.example.gearup.uiactivities.customer.CustomerDashboardActivity;
import com.example.gearup.uiactivities.manager.ManagerDashboardActivity;
import com.example.gearup.uiactivities.mechanic.MechanicDashboardActivity;
import com.example.gearup.utilities.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // No user logged in, redirect to the login screen
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // User is logged in, check their role
        String uid = currentUser.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");
                        Intent intent;
                        if ("manager".equals(userType)) {
                            intent = new Intent(MainActivity.this, ManagerDashboardActivity.class);
                        } else if ("admin".equals(userType)) {
                            intent = new Intent(MainActivity.this, AdminApprovalActivity.class);
                        } else if ("mechanic".equals(userType)) {
                            intent = new Intent(MainActivity.this, MechanicDashboardActivity.class);
                        } else if ("customer".equals(userType)) {
                            intent = new Intent(MainActivity.this, CustomerDashboardActivity.class);
                        } else {
                            // Default to login screen for unknown user types
                            intent = new Intent(MainActivity.this, LoginActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e("MainActivity", "User document does not exist for UID: " + uid);
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Failed to fetch user data: " + e.getMessage(), e);
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
    }
}