package com.example.gearup;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class FirebaseConfig extends Application {
    private static final String TAG = "FirebaseConfig";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Check Google Play Services availability
            int playServicesStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            if (playServicesStatus != ConnectionResult.SUCCESS) {
                Log.e(TAG, "Google Play Services unavailable: " + playServicesStatus);
                Log.e(TAG, "Error message: " + GooglePlayServicesUtil.getErrorString(playServicesStatus));
                Toast.makeText(this, "Google Play Services is not available: " + GooglePlayServicesUtil.getErrorString(playServicesStatus), Toast.LENGTH_LONG).show();
                return;
            } else {
                Log.d(TAG, "Google Play Services available");
            }

            // Initialize Firebase
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");

            // Configure Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseFirestore.setLoggingEnabled(true);
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            db.setFirestoreSettings(settings);

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(null);

            // Configure App Check
           /* FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
            firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());
            firebaseAppCheck.setTokenAutoRefreshEnabled(true);
            Log.d(TAG, "App Check: Initialized with Debug provider");*/

        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage(), e);
            Toast.makeText(this, "Firebase initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}