package com.example.gearup.uiactivities.manager;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.example.gearup.uiactivities.customer.ViewContractsFragment;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ManagerDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Places and PlacesClient
        if (TextUtils.isEmpty(BuildConfig.GOOGLE_MAPS_API_KEY)) {
            Log.e("ManagerDashboardActivity", "Google Maps API key is missing");
            return;
        }
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.GOOGLE_MAPS_API_KEY);
        }
        placesClient = Places.createClient(this);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        setupBottomNavigationView();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new AddCarFragment());
            bottomNavigationView.setSelectedItemId(R.id.navigation_add_car);
        }
    }

    public PlacesClient getPlacesClient() {
        return placesClient;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        placesClient = null;
    }
}