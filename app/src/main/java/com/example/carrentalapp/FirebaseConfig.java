package com.example.carrentalapp;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class FirebaseConfig extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase with error handling
        try {
            FirebaseApp.initializeApp(this);
            Log.d("FirebaseInit", "Firebase initialized successfully");

            // Configure App Check
            FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance());
                Log.d("AppCheck", "Debug provider initialized");
            } else {
                appCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance());
                Log.d("AppCheck", "Play Integrity provider initialized");
            }
        } catch (Exception e) {
            Log.e("FirebaseInit", "Firebase initialization failed", e);
        }

        // Removed Google Play Services check since it requires Activity context
        // This check should be moved to your main Activity instead
    }
}