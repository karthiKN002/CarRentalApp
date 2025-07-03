package com.example.gearup.uiactivities.admin;

import static androidx.camera.core.CameraXThreads.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.gearup.R;
import com.example.gearup.utilities.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Button approveManagersButton = findViewById(R.id.approve_managers_button);
        Button approveMechanicsButton = findViewById(R.id.approve_mechanics_button);
        Button approveCarsButton = findViewById(R.id.approve_cars_button);

        approveManagersButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminApprovalActivity.class);
            intent.putExtra("userType", "manager");
            startActivity(intent);
        });

        approveMechanicsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminApprovalActivity.class);
            intent.putExtra("userType", "mechanic");
            startActivity(intent);
        });

        approveCarsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CarApprovalActivity.class);
            startActivity(intent);
        });
    }
}