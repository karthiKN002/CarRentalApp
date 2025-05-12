package com.example.gearup.uiactivities.customer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class CustomerDashboardActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private BottomNavigationView bottomNavigationView;
    private androidx.appcompat.widget.Toolbar toolbar;
    private TextView textViewGreeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        mAuth = FirebaseAuth.getInstance();

        setupToolbar();

        textViewGreeting = findViewById(R.id.textViewGreeting);
        SharedPreferences sharedPreferences = getSharedPreferences("CarRentalAppPrefs", MODE_PRIVATE);
        String firstName = sharedPreferences.getString("first_name", "User");
        textViewGreeting.setText("Hi, " + firstName);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if (savedInstanceState == null) {
            replaceFragment(new ViewAvailableCarFragment());
            bottomNavigationView.setSelectedItemId(R.id.navigation_view_available_cars);
        }

        setupGoogleSignInClient();
        setupBottomNavigationView();
        setupListeners();
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.customerToolbar);
        setSupportActionBar(toolbar);
    }

    private void setupGoogleSignInClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build();
        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso);
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_view_available_cars) {
                selectedFragment = new ViewAvailableCarFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.navigation_view_available_contracts) {
                selectedFragment = new ViewContractsFragment();
            } else if (itemId == R.id.navigation_nearby_mechanics) {
                selectedFragment = new MapsFragment(); // New fragment for maps
            } else if (itemId == R.id.navigation_news) {
                selectedFragment = new NewsFragment();
            } /*else if (itemId == R.id.navigation_sign_out) {
                SignOutActivity.signOut(this, mAuth, googleSignInClient);
                return true;
            }*/

            return loadFragment(selectedFragment);
        });
    }

    private void setupListeners() {}

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.customerFragmentContainer, fragment)
                    .commit();
            Log.d("CustomerDashboard", "Loading fragment: " + fragment.getClass().getSimpleName());
            return true;
        }
        return false;
    }

    private void replaceFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.customerFragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
            Log.d("CustomerDashboard", "Replacing fragment: " + fragment.getClass().getSimpleName());
        }
    }
}