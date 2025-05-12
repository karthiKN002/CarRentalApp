package com.example.gearup.uiactivities.manager;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.gearup.R;
import com.example.gearup.uiactivities.customer.ViewContractsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ManagerDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        setupBottomNavigationView();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new AddCarFragment());
            bottomNavigationView.setSelectedItemId(R.id.navigation_add_car);
        }
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_add_car) {
                fragment = new AddCarFragment();
            } else if (itemId == R.id.navigation_view_cars) {
                fragment = new ViewCarsFragment();
            } else if (itemId == R.id.navigation_view_users) {
                fragment = new ViewUsersFragment();
            } else if (itemId == R.id.navigation_view_contracts) {
                fragment = new ViewContractsFragment();
            }
            return loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.managerFragmentContainer, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}