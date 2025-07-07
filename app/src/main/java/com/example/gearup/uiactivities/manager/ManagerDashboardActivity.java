package com.example.gearup.uiactivities.manager;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.example.gearup.uiactivities.customer.NearbyMechanicsFragment;
import com.example.gearup.uiactivities.customer.ViewContractsFragment;
import com.example.gearup.utilities.SignOutActivity;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.appcompat.app.AlertDialog;

public class ManagerDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private PlacesClient placesClient;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.managerToolbar);
        if (toolbar == null) {
            Log.e("ManagerDashboardActivity", "Toolbar not found in layout");
            return;
        }
        setSupportActionBar(toolbar);

        // Initialize Places and PlacesClient
        if (TextUtils.isEmpty(BuildConfig.GOOGLE_MAPS_API_KEY)) {
            Log.e("ManagerDashboardActivity", "Google Maps API key is missing");
            return;
        }
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.GOOGLE_MAPS_API_KEY);
        }
        placesClient = Places.createClient(this);

        setupNavigationDrawer(toolbar);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView == null) {
            Log.e("ManagerDashboardActivity", "BottomNavigationView not found in layout");
            return;
        }
        setupBottomNavigationView();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new ChatListFragment());
            bottomNavigationView.setSelectedItemId(R.id.navigation_chat);
        }
    }

    public PlacesClient getPlacesClient() {
        return placesClient;
    }

    private void setupNavigationDrawer(Toolbar toolbar) {
        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navView);
        if (drawerLayout == null || navigationView == null) {
            Log.e("ManagerDashboardActivity", "DrawerLayout or NavigationView not found");
            return;
        }
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_logout) {
                SignOutActivity.signOut(this, mAuth, null);
                finish();
            } else if (itemId == R.id.nav_help) {
                showHelpDialog();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Help")
                .setMessage("For enquiry and issue contact:\n\nPhone: +91 6381531297\nEmail: gearupsupport@gmail.com")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_chat) {
                fragment = new ChatListFragment();
            } else if (itemId == R.id.navigation_view_cars) {
                fragment = new ViewCarsFragment();
            } else if (itemId == R.id.navigation_add_car) {
                fragment = new AddCarFragment();
            } else if (itemId == R.id.navigation_view_contracts) {
                fragment = new ViewContractsFragment();
            } else if (itemId == R.id.navigation_mechanics_list) {
                fragment = new NearbyMechanicsFragment();
            }
            return loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.managerFragmentContainer, fragment)
                    .addToBackStack(null)
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