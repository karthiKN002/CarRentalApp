package com.example.gearup.uiactivities.customer;

import android.os.Bundle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.example.gearup.uiactivities.customer.ProfileFragment;
import com.example.gearup.uiactivities.customer.CustomerChatListFragment;
import com.example.gearup.uiactivities.customer.NearbyMechanicsFragment;
import com.example.gearup.uiactivities.customer.ViewAvailableCarFragment;
import com.example.gearup.uiactivities.customer.ViewContractsFragment;
import com.example.gearup.utilities.SignOutActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AlertDialog;

public class CustomerDashboardActivity extends AppCompatActivity {

    private PlacesClient placesClient;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.GOOGLE_MAPS_API_KEY);
        }
        placesClient = Places.createClient(this);

        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.customerToolbar);
        setSupportActionBar(toolbar);

        setupNavigationDrawer();
        setupBottomNavigationView();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.customerFragmentContainer, new ViewAvailableCarFragment())
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.navigation_view_available_cars);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        placesClient = null;
    }

    public PlacesClient getPlacesClient() {
        return placesClient;
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navView);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_logout) {
                SignOutActivity.signOut(this, mAuth, googleSignInClient);
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
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_view_available_cars) {
                fragment = new ViewAvailableCarFragment();
            } else if (itemId == R.id.navigation_view_available_contracts) {
                fragment = new ViewContractsFragment();
            } else if (itemId == R.id.navigation_mechanics_list) {
                fragment = new NearbyMechanicsFragment();
            } else if (itemId == R.id.navigation_chats) {
                fragment = new CustomerChatListFragment();
            } else if (itemId == R.id.navigation_profile) {
                fragment = new ProfileFragment();
            }
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.customerFragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            return false;
        });
    }
}