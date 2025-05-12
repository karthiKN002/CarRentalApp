package com.example.gearup.uiactivities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gearup.R;

public class AdminDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Button approveManagersButton = findViewById(R.id.approve_managers_button);
        approveManagersButton.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminApprovalActivity.class));
        });
    }
}