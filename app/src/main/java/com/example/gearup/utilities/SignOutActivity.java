package com.example.gearup.utilities;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.gearup.BuildConfig;
import com.example.gearup.utilities.LoginActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class SignOutActivity {

    private static final String TAG = "SignOutActivity";

    public static void signOut(Context context, FirebaseAuth mAuth, GoogleSignInClient googleSignInClient) {
        // Sign out from Firebase
        if (mAuth != null) {
            mAuth.signOut();
            Log.d(TAG, "Firebase sign-out successful");
        }

        // Sign out from Google
        if (googleSignInClient != null) {
            googleSignInClient.signOut()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Google sign-out successful");
                        } else {
                            Log.e(TAG, "Google sign-out failed", task.getException());
                        }
                    });
        } else {
            // If googleSignInClient is null, initialize it (though ideally it should be passed correctly)
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID) // Replace with your client ID
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(context, gso);
            googleSignInClient.signOut();
        }

        // Clear any shared preferences if needed
        context.getSharedPreferences("CarRentalAppPrefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Navigate to LoginActivity
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}