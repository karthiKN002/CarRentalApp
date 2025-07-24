package com.example.gearup.uiactivities.mechanic;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.example.gearup.R;
import com.example.gearup.uiactivities.customer.ProfileFragment;
import com.example.gearup.uiactivities.customer.ViewAvailableCarFragment;
import com.example.gearup.uiactivities.manager.ChatListFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MechanicDashboardActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.mechanicToolbar);
        setSupportActionBar(toolbar);

        Switch activeSwitch = findViewById(R.id.activeSwitch);
        activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .update("isActive", isChecked)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, isChecked ? "You are now active" : "You are now inactive", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        activeSwitch.setChecked(!isChecked);
                    });
        });

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean isActive = documentSnapshot.getBoolean("isActive");
                    if (isActive != null) {
                        activeSwitch.setChecked(isActive);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_chats) {
                fragment = new ChatListFragment();
            } else if (itemId == R.id.navigation_available_cars) {
                fragment = new ViewAvailableCarFragment();
            } else if (itemId == R.id.navigation_profile) {
                fragment = new ProfileFragment();
            }
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.mechanicFragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mechanicFragmentContainer, new ChatListFragment())
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.navigation_chats);
        }
    }
}