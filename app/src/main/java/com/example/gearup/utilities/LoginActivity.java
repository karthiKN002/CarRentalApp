package com.example.gearup.utilities;

import static androidx.core.content.ContextCompat.startActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.example.gearup.factories.UserFactory;
import com.example.gearup.models.User;
import com.example.gearup.uiactivities.customer.CustomerDashboardActivity;
import com.example.gearup.uiactivities.manager.ManagerDashboardActivity;
import com.example.gearup.uiactivities.admin.AdminDashboardActivity;
import com.example.gearup.uiactivities.mechanic.MechanicDashboardActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.services.calendar.CalendarScopes;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "CarRentalAppPrefs";
    private static final String ROLE_KEY = "user_role";
    private static final String GOOGLE_ACCOUNT_NAME_KEY = "google_account_name";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int RC_SIGN_IN = 9001;

    private GoogleSignInClient googleSignInClient;
    private EditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Log.d("LoginActivity", "Current UID: " + user.getUid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        checkGooglePlayServices();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeUI();
        initializeGoogleSignIn();
        checkLocationPermission();
    }

    private void initializeUI() {
        emailEditText = findViewById(R.id.emailLogin);
        passwordEditText = findViewById(R.id.passwordLogin);
        Button loginButton = findViewById(R.id.loginbtn);
        TextView registerLink = findViewById(R.id.registerLink);
        Button googleSignButton = findViewById(R.id.googleSignInButton);

        registerLink.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Log.e("LoginActivity", "Failed to start RegisterActivity", e);
                Toast.makeText(LoginActivity.this, "Error starting RegisterActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        loginButton.setOnClickListener(v -> loginUser());
        googleSignButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .requestScopes(new Scope(CalendarScopes.CALENDAR))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w("GoogleSignIn", "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In Failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        handleGoogleUser(user);
                    } else {
                        Toast.makeText(this, "Firebase Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static List<String> getAdminEmails() {
        return Arrays.asList(BuildConfig.ADMIN_EMAILS.split(","));
    }

    private void handleGoogleUser(FirebaseUser firebaseUser) {
        db.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.contains("blocked") && document.getBoolean("blocked")) {
                        Toast.makeText(this, "Your account is blocked. Please contact support.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        googleSignInClient.signOut();
                        return;
                    }

                    if (!document.exists()) {
                        String userType = getAdminEmails().contains(firebaseUser.getEmail()) ? "manager" : "customer";
                        String fullName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Unknown";
                        boolean approved = !userType.equals("manager"); // Default approval state

                        User newUser = UserFactory.createUser(
                                userType,
                                firebaseUser.getUid(),
                                fullName,
                                firebaseUser.getEmail(),
                                firebaseUser.getPhoneNumber(),
                                null,
                                Timestamp.now(),
                                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() :
                                        "android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.ic_user_avatar_placeholder,
                                approved
                        );

                        saveUserToFirestore(newUser);
                    } else {
                        String userType = document.getString("userType");
                        boolean approved = document.getBoolean("isApproved");
                        String fullName = document.getString("fullName");
                        String email = document.getString("email");

                        User user = UserFactory.createUser(
                                userType,
                                firebaseUser.getUid(),
                                fullName,
                                email,
                                document.getString("phoneNumber"),
                                document.getString("licenseDocument"),
                                document.getTimestamp("createdAt"),
                                document.getString("imgUrl"),
                                approved
                        );

                        saveUserToPreferences(user.getUid(), user.getEmail(), user.getUserType(), user.getFullName());
                        navigateToDashboard(userType, approved);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Error fetching user data", e);
                    Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserToFirestore(User user) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                user.setFcmToken(task.getResult());
            }
            db.collection("users").document(user.getUid()).set(user)
                    .addOnSuccessListener(aVoid -> {
                        saveUserToPreferences(user.getUid(), user.getEmail(), user.getUserType(), user.getFullName());
                        navigateToDashboard(user.getUserType(), user.isApproved());
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LoginActivity", "Error saving user data", e);
                        Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void saveUserToPreferences(String userId, String email, String userType, String fullName) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_id", userId);
        editor.putString("email", email != null ? email : "");
        editor.putString(ROLE_KEY, userType != null ? userType : "");
        editor.putString("full_name", fullName != null ? fullName : "Unknown");
        editor.putLong("createdAt", System.currentTimeMillis());
        editor.putString(GOOGLE_ACCOUNT_NAME_KEY, email != null ? email : "");
        editor.apply();
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (validateLoginInputs(email, password)) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            navigateUser(firebaseUser, "password");
                        } else {
                            Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private boolean validateLoginInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return false;
        }
        return true;
    }

    private void saveFcmToken(String userId) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String fcmToken = task.getResult();
                db.collection("users").document(userId)
                        .update("fcmToken", fcmToken)
                        .addOnSuccessListener(aVoid -> Log.d("LoginActivity", "FCM token saved"))
                        .addOnFailureListener(e -> Log.e("LoginActivity", "Failed to save FCM token", e));
            }
        });
    }

    private void navigateUser(FirebaseUser firebaseUser, String provider) {
        db.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String userType = document.getString("userType");
                        Boolean approved = document.getBoolean("isApproved");
                        if (approved == null) {
                            approved = false;
                        }
                        String fullName = document.getString("fullName");
                        String email = document.getString("email");

                        saveUserToPreferences(firebaseUser.getUid(), email, userType, fullName);
                        saveFcmToken(firebaseUser.getUid());
                        navigateToDashboard(userType, approved);
                    } else {
                        Toast.makeText(this, "User profile not found. Please register.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Error fetching user data", e);
                    Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToDashboard(String userType, boolean approved) {
        Intent intent;
        if ("manager".equals(userType)) {
            if (!approved) {
                Toast.makeText(this, "Your manager account is pending approval", Toast.LENGTH_LONG).show();
                mAuth.signOut();
                googleSignInClient.signOut();
                return;
            }
            intent = new Intent(this, ManagerDashboardActivity.class);
        } else if ("admin".equals(userType)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if ("mechanic".equals(userType)) {
            if (!approved) {
                Toast.makeText(this, "Your mechanic account is pending approval", Toast.LENGTH_LONG).show();
                mAuth.signOut();
                googleSignInClient.signOut();
                return;
            }
            intent = new Intent(this, MechanicDashboardActivity.class);
        } else {
            intent = new Intent(this, CustomerDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "Location permission is needed to show nearby cars and distances", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Location permission needed for map features", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkGooglePlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("LoginActivity", "Google Play Services unavailable: " + resultCode);
            Toast.makeText(this, "Google Play Services is not available", Toast.LENGTH_LONG).show();
        } else {
            Log.d("LoginActivity", "Google Play Services available");
        }
    }
}