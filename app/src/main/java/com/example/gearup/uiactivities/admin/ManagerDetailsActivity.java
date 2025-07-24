package com.example.gearup.uiactivities.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gearup.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class ManagerDetailsActivity extends AppCompatActivity {
    private static final String TAG = "ManagerDetailsActivity";
    private TextView textViewName, textViewEmail, textViewPhone, textViewBusinessAddress;
    private Button buttonViewAadhar, buttonViewPan, buttonViewLicense, buttonViewPassbook, buttonViewPhoto, buttonApprove, buttonReject;
    private FirebaseFirestore db;
    private String managerId, userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_details);

        db = FirebaseFirestore.getInstance();
        textViewName = findViewById(R.id.textViewName);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewPhone = findViewById(R.id.textViewPhone);
        textViewBusinessAddress = findViewById(R.id.textViewBusinessAddress);
        buttonViewAadhar = findViewById(R.id.buttonViewAadhar);
        buttonViewPan = findViewById(R.id.buttonViewPan);
        buttonViewLicense = findViewById(R.id.buttonViewLicense);
        buttonViewPassbook = findViewById(R.id.buttonViewPassbook);
        buttonViewPhoto = findViewById(R.id.buttonViewPhoto);
        buttonApprove = findViewById(R.id.buttonApprove);
        buttonReject = findViewById(R.id.buttonReject);

        managerId = getIntent().getStringExtra("managerId");
        if (managerId != null) {
            loadManagerDetails(managerId);
        } else {
            Log.e(TAG, "No managerId provided");
            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
            finish();
        }

        buttonApprove.setOnClickListener(v -> approveUser());
        buttonReject.setOnClickListener(v -> rejectUser());
    }

    private void loadManagerDetails(String managerId) {
        db.collection("users").document(managerId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        textViewName.setText(document.getString("fullName"));
                        textViewEmail.setText(document.getString("email"));
                        textViewPhone.setText(document.getString("phone"));
                        textViewBusinessAddress.setText(document.getString("businessAddress") != null ? document.getString("businessAddress") : "N/A");
                        userType = document.getString("userType");

                        if ("manager".equals(userType)) {
                            // Manager-specific fields
                            setupDocumentButton(buttonViewAadhar, document.getString("aadharDocument"), "View Aadhar");
                            setupDocumentButton(buttonViewPan, document.getString("panDocument"), "View PAN");
                            setupDocumentButton(buttonViewLicense, document.getString("licenseDocument"), "View License");
                            setupDocumentButton(buttonViewPassbook, document.getString("passbookImage"), "View Passbook");
                            buttonViewPhoto.setVisibility(View.GONE);
                        } else if ("mechanic".equals(userType)) {
                            // Mechanic-specific fields
                            String identityUrl = document.getString("identityDocument");
                            if (identityUrl != null) {
                                // Prefer Aadhar button for identity document, fallback to PAN if needed
                                setupDocumentButton(buttonViewAadhar, identityUrl, "View Identity (Aadhar/PAN)");
                                buttonViewPan.setVisibility(View.GONE);
                            } else {
                                setupDocumentButton(buttonViewAadhar, null, "No Identity");
                                buttonViewPan.setVisibility(View.GONE);
                            }
                            setupDocumentButton(buttonViewPhoto, document.getString("photo"), "View Photo");
                            buttonViewLicense.setVisibility(View.GONE);
                            buttonViewPassbook.setVisibility(View.GONE);
                            textViewBusinessAddress.setVisibility(View.GONE);
                        } else {
                            Log.w(TAG, "Unknown userType: " + userType);
                            Toast.makeText(this, "Invalid user type", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        Log.d(TAG, "Loaded user: ID=" + managerId + ", Type=" + userType + ", Identity=" + document.getString("identityDocument") + ", Photo=" + document.getString("photo"));
                    } else {
                        Log.e(TAG, "Document does not exist: " + managerId);
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load details: " + e.getMessage());
                    Toast.makeText(this, "Failed to load details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupDocumentButton(Button button, String url, String defaultText) {
        if (url != null && !url.isEmpty()) {
            button.setText(defaultText);
            button.setEnabled(true);
            button.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open document URL: " + url + ", error: " + e.getMessage());
                    Toast.makeText(this, "Unable to open document", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            button.setText("No Document");
            button.setEnabled(false);
        }
    }

    private void approveUser() {
        db.collection("users").document(managerId)
                .update("isApproved", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, userType + " approved: " + managerId);
                    Toast.makeText(this, userType + " approved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to approve: " + e.getMessage());
                    Toast.makeText(this, "Failed to approve: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectUser() {
        db.collection("users").document(managerId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, userType + " rejected: " + managerId);
                    Toast.makeText(this, userType + " rejected", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reject: " + e.getMessage());
                    Toast.makeText(this, "Failed to reject: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}