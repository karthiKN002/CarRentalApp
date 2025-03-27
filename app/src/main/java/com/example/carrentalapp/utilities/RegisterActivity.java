package com.example.carrentalapp.utilities;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.carrentalapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends Activity {

    private EditText editTextFullName, editTextEmail, editTextPassword, editTextPhone, editTextOtp;
    private RadioGroup radioGroupUserType;
    private Button buttonSendOtp, buttonVerifyOtp, buttonRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private boolean isOtpVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize UI components
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextOtp = findViewById(R.id.editTextOtp);
        radioGroupUserType = findViewById(R.id.radioGroupUserType);
        buttonSendOtp = findViewById(R.id.buttonSendOtp);
        buttonVerifyOtp = findViewById(R.id.buttonVerifyOtp);
        buttonRegister = findViewById(R.id.buttonRegister);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initially hide register button
        buttonRegister.setVisibility(View.GONE);

        buttonSendOtp.setOnClickListener(v -> sendVerificationCode());
        buttonVerifyOtp.setOnClickListener(v -> verifyCode());
        buttonRegister.setOnClickListener(v -> registerUser());
    }

    private void sendVerificationCode() {
        String phoneNumber = editTextPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            editTextPhone.setError("Enter a valid phone number");
            return;
        }

        if (!phoneNumber.matches("\\d{10}")) {
            editTextPhone.setError("Invalid phone number. Must be 10 digits.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSendOtp.setEnabled(false);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber("+91" + phoneNumber) // Add country code
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        progressBar.setVisibility(View.GONE);
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        progressBar.setVisibility(View.GONE);
                        buttonSendOtp.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        progressBar.setVisibility(View.GONE);
                        buttonSendOtp.setEnabled(true);
                        RegisterActivity.this.verificationId = verificationId;
                        resendingToken = token;
                        Toast.makeText(RegisterActivity.this, "OTP Sent Successfully", Toast.LENGTH_SHORT).show();
                        // Show OTP field and verify button
                        findViewById(R.id.otpLayout).setVisibility(View.VISIBLE);
                        buttonVerifyOtp.setVisibility(View.VISIBLE);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode() {
        String code = editTextOtp.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            editTextOtp.setError("Enter the OTP");
            return;
        }

        if (verificationId == null) {
            Toast.makeText(this, "OTP not sent yet. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        isOtpVerified = true;
                        buttonRegister.setVisibility(View.VISIBLE);
                        Toast.makeText(RegisterActivity.this, "Verification Successful", Toast.LENGTH_SHORT).show();
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(RegisterActivity.this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Verification Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void registerUser() {
        if (!isOtpVerified) {
            Toast.makeText(this, "Please verify your phone number first", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        int selectedId = radioGroupUserType.getCheckedRadioButtonId();
        String userType;

        if (selectedId == -1) {
            Toast.makeText(this, "Please select User or Manager", Toast.LENGTH_SHORT).show();
            return;
        } else {
            userType = (selectedId == R.id.radioButtonManager) ? "manager" : "user";
        }

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save additional user data to Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("uid", user.getUid());
                            userData.put("fullName", fullName);
                            userData.put("email", email);
                            userData.put("phone", phone);
                            userData.put("userType", userType);
                            userData.put("createdAt", System.currentTimeMillis());

                            db.collection("Users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(RegisterActivity.this,
                                                "Registration Successful as " + userType,
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressBar.setVisibility(View.GONE);
                                        buttonRegister.setEnabled(true);
                                        Toast.makeText(RegisterActivity.this,
                                                "Failed to save user data: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        buttonRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this,
                                "Registration Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}