package com.example.gearup.utilities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private static final String COUNTRY_CODE = "+91";
    private static final int PICK_DOCUMENT_REQUEST = 1;
    private static final int MAX_SMS_RETRIES = 3;
    private static final long MAX_PDF_SIZE = 5 * 1024 * 1024; // 5MB
    private int smsRetryCount = 0;

    private EditText editTextFullName, editTextEmail, editTextPassword, editTextPhone, editTextOtp;
    private RadioGroup radioGroupUserType;
    private Button buttonSendOtp, buttonVerifyOtp, buttonRegister, buttonUploadDoc, buttonResendOtp;
    private ProgressBar progressBar;
    private Uri documentUri;
    private boolean isDocumentUploaded = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private boolean isOtpVerified = false;
    private static final String USER_TYPE_CUSTOMER = "customer";
    private static final String USER_TYPE_MANAGER = "manager";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        checkSmsPermission();
        setupFirebase();
        setupClickListeners();
        checkPlayServices(this);
    }

    private void initializeViews() {
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextOtp = findViewById(R.id.editTextOtp);
        buttonSendOtp = findViewById(R.id.buttonSendOtp);
        buttonVerifyOtp = findViewById(R.id.buttonVerifyOtp);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonUploadDoc = findViewById(R.id.buttonUploadDoc);
        progressBar = findViewById(R.id.progressBar);
        radioGroupUserType = findViewById(R.id.radioGroupUserType);

        buttonResendOtp = findViewById(R.id.buttonResendOtp);
        buttonResendOtp.setVisibility(View.GONE);
        buttonRegister.setVisibility(View.GONE); // Ensure Register button is hidden initially
        buttonUploadDoc.setVisibility(View.GONE); // Ensure Upload button is hidden initially

        radioGroupUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioManager && isOtpVerified) {
                buttonUploadDoc.setVisibility(View.VISIBLE);
                findViewById(R.id.uploadDocDescription).setVisibility(View.VISIBLE);
                buttonUploadDoc.setText("Upload Store License");
            } else {
                buttonUploadDoc.setVisibility(View.GONE);
                findViewById(R.id.uploadDocDescription).setVisibility(View.GONE);
                documentUri = null;
                isDocumentUploaded = false;
                buttonUploadDoc.setText("Upload Store License");
            }
        });
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECEIVE_SMS},
                    102);
        }
    }
    public void checkPlayServices(Context context) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog((Activity) context, resultCode, 9000).show();
            } else {
                Log.e("PlayServices", "This device does not support Google Play Services");
            }
        } else {
            Log.d("PlayServices", "Google Play Services is available");
        }
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        Log.d(TAG, "Connected to Firebase Production");
    }

    private void setupClickListeners() {
        buttonSendOtp.setOnClickListener(v -> sendVerificationCode());
        buttonVerifyOtp.setOnClickListener(v -> verifyCode());
        buttonUploadDoc.setOnClickListener(v -> {
            if (isManagerSelected()) {
                openDocumentPicker();
            }
        });
        buttonRegister.setOnClickListener(v -> {
            if (validateRegistration()) {
                registerUser();
            }
        });
        buttonResendOtp.setOnClickListener(v -> sendVerificationCode());
    }

    private boolean validateRegistration() {
        if (!isOtpVerified) {
            Toast.makeText(this, "Verify phone first", Toast.LENGTH_SHORT).show();
            return false;
        }

        String password = editTextPassword.getText().toString().trim();
        if (password.length() < 6) {
            editTextPassword.setError("Password must be 6+ characters");
            return false;
        }

        // Document upload is optional for managers
        return true;
    }

    private boolean isManagerSelected() {
        return radioGroupUserType.getCheckedRadioButtonId() == R.id.radioManager;
    }

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_DOCUMENT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_DOCUMENT_REQUEST && resultCode == RESULT_OK && data != null) {
            documentUri = data.getData();
            Log.d(TAG, "onActivityResult: Raw documentUri = " + (documentUri != null ? documentUri.toString() : "null"));
            if (documentUri != null) {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(documentUri, new String[]{OpenableColumns.SIZE}, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        long size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
                        Log.d(TAG, "File size: " + size + " bytes");
                        if (size > MAX_PDF_SIZE) {
                            Toast.makeText(this, "File size exceeds 5MB limit", Toast.LENGTH_SHORT).show();
                            documentUri = null;
                            return;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking file size: " + e.getMessage());
                    Toast.makeText(this, "Failed to check file size", Toast.LENGTH_SHORT).show();
                    documentUri = null;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                Log.d(TAG, "Final documentUri: " + documentUri.toString());
                buttonUploadDoc.setText("License Uploaded ✓");
                buttonUploadDoc.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0);
                isDocumentUploaded = true;
            } else {
                Log.e(TAG, "Document URI is null after selection");
                Toast.makeText(this, "Failed to select document", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "onActivityResult: Invalid result - requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
        }
    }

    private void sendVerificationCode() {
        if (smsRetryCount >= MAX_SMS_RETRIES) {
            Toast.makeText(this, "Maximum attempts reached. Try again later.", Toast.LENGTH_LONG).show();
            return;
        }
        smsRetryCount++;

        String phoneNumber = editTextPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            editTextPhone.setError("Phone number required");
            return;
        }

        String completePhoneNumber = COUNTRY_CODE + phoneNumber;
        Log.d(TAG, "Starting phone verification for: " + completePhoneNumber);

        try {
            // Check Play Services availability using GoogleApiAvailability
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int playServicesStatus = googleApiAvailability.isGooglePlayServicesAvailable(this);
            if (playServicesStatus != ConnectionResult.SUCCESS) {
                Log.e(TAG, "Google Play Services unavailable: " + playServicesStatus);
                Log.e(TAG, "Error message: " + googleApiAvailability.getErrorString(playServicesStatus));
                Toast.makeText(this, "Google Play Services not available: " + googleApiAvailability.getErrorString(playServicesStatus), Toast.LENGTH_LONG).show();
                return;
            }
            Log.d(TAG, "Google Play Services available");

            progressBar.setVisibility(View.VISIBLE);
            buttonSendOtp.setEnabled(false);

            PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber(completePhoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                            Log.d(TAG, "Phone verification completed: " + credential.toString());
                            progressBar.setVisibility(View.GONE);
                            signInWithCredential(credential);
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            Log.e(TAG, "Phone verification failed: " + e.getMessage(), e);
                            progressBar.setVisibility(View.GONE);
                            buttonSendOtp.setEnabled(true);
                            handleVerificationError(e);
                            if (e.getMessage().contains("Google Play Services")) {
                                Log.e(TAG, "Play Services issue during verification: " + e.getMessage());
                                Toast.makeText(RegisterActivity.this, "Google Play Services error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCodeSent(@NonNull String verificationId,
                                               @NonNull PhoneAuthProvider.ForceResendingToken token) {
                            Log.d(TAG, "Verification code sent: " + verificationId);
                            progressBar.setVisibility(View.GONE);
                            buttonSendOtp.setEnabled(true);
                            RegisterActivity.this.verificationId = verificationId;
                            resendingToken = token;
                            showOtpLayout();
                            Toast.makeText(RegisterActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                        }
                    });
            if (resendingToken != null) {
                builder.setForceResendingToken(resendingToken);
            }
            PhoneAuthProvider.verifyPhoneNumber(builder.build());
            Log.d(TAG, "PhoneAuthProvider.verifyPhoneNumber called successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting phone verification: " + e.getMessage(), e);
            progressBar.setVisibility(View.GONE);
            buttonSendOtp.setEnabled(true);
            Toast.makeText(this, "Error starting verification: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleVerificationError(FirebaseException e) {
        String errorMessage = "Verification failed. Please try again.";
        if (e instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            switch (errorCode) {
                case "ERROR_INVALID_PHONE_NUMBER":
                    errorMessage = "Invalid phone number format";
                    break;
                case "ERROR_SESSION_EXPIRED":
                    errorMessage = "OTP expired. Request new code";
                    break;
                case "ERROR_INVALID_VERIFICATION_CODE":
                    errorMessage = "Invalid OTP entered";
                    break;
                default:
                    errorMessage = "Verification error: " + e.getMessage();
            }
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Verification failed: " + e.getMessage(), e);
    }

    private void showOtpLayout() {
        findViewById(R.id.otpLayout).setVisibility(View.VISIBLE);
        buttonVerifyOtp.setVisibility(View.VISIBLE);
        buttonSendOtp.setVisibility(View.GONE);
        buttonResendOtp.setVisibility(View.VISIBLE);
    }

    private void verifyCode() {
        String code = editTextOtp.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            editTextOtp.setError("Enter OTP");
            return;
        }

        if (verificationId == null) {
            Toast.makeText(this, "Request OTP first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        isOtpVerified = true;
                        buttonSendOtp.setVisibility(View.GONE);
                        buttonVerifyOtp.setVisibility(View.GONE);
                        buttonResendOtp.setVisibility(View.GONE);
                        buttonRegister.setVisibility(View.VISIBLE); // Show Register button after OTP verification
                        if (isManagerSelected()) {
                            buttonUploadDoc.setVisibility(View.VISIBLE); // Show Upload button for managers
                            findViewById(R.id.uploadDocDescription).setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(this, "Verification failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser() {
        if (!validateRegistration()) return;

        long startTime = System.currentTimeMillis();
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String userType = isManagerSelected() ? USER_TYPE_MANAGER : USER_TYPE_CUSTOMER;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Registering... Please wait.")
                .setCancelable(false)
                .create();
        dialog.show();
        buttonRegister.setEnabled(false);

        checkPhoneNumberAvailability(phone, available -> {
            if (!available) {
                dialog.dismiss();
                buttonRegister.setEnabled(true);
                editTextPhone.setError("Phone number already registered");
                return;
            }
            performUserRegistration(fullName, email, password, phone, userType, dialog, startTime);
        });
    }

    private void checkPhoneNumberAvailability(String phone, PhoneCheckCallback callback) {
        if (BuildConfig.DEBUG) {
            callback.onResult(true);
            return;
        }
        db.collection("users")
                .whereEqualTo("phone", COUNTRY_CODE + phone)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onResult(task.getResult() == null || task.getResult().isEmpty());
                    } else {
                        Log.e(TAG, "Phone check failed: " + task.getException());
                        runOnUiThread(() -> {
                            Toast.makeText(RegisterActivity.this, "Failed to check phone availability: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            callback.onResult(false); // Allow registration to proceed or handle differently
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Phone check error: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "Network error during phone check", Toast.LENGTH_LONG).show();
                        callback.onResult(false);
                    });
                });
    }

    interface PhoneCheckCallback {
        void onResult(boolean available);
    }

    private void performUserRegistration(String fullName, String email, String password, String phone, String userType, AlertDialog dialog, long startTime) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "User created successfully: " + user.getUid());
                            saveUserToFirestore(user.getUid(), fullName, email, phone, userType, dialog, startTime);
                        } else {
                            dialog.dismiss();
                            buttonRegister.setEnabled(true);
                            Log.e(TAG, "User object is null after creation");
                            Toast.makeText(this, "User creation failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        dialog.dismiss();
                        buttonRegister.setEnabled(true);
                        Log.e(TAG, "Email/password registration failed: " + task.getException().getMessage());
                        handleRegistrationError(task.getException());
                    }
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    buttonRegister.setEnabled(true);
                    Log.e(TAG, "Registration failure: " + e.getMessage());
                    Toast.makeText(this, "Network error during registration", Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserToFirestore(String uid, String fullName, String email, String phone, String userType, AlertDialog dialog, long startTime) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("phone", COUNTRY_CODE + phone);
        userData.put("userType", userType);
        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("blocked", false);
        userData.put("points", USER_TYPE_CUSTOMER.equals(userType) ? 0 : null);

        if (USER_TYPE_MANAGER.equals(userType)) {
            userData.put("isApproved", false);
            if (documentUri != null) {
                uploadManagerDocument(uid, fullName, email, phone, userType, dialog, startTime);
                return;
            }
        }

        db.collection("users").document(uid)
                .set(userData)
                .addOnCompleteListener(task -> {
                    dialog.dismiss();
                    buttonRegister.setEnabled(true);
                    Log.d(TAG, "Registration completed in " + (System.currentTimeMillis() - startTime) + "ms");
                    if (task.isSuccessful()) {
                        completeRegistration(userType);
                    } else {
                        Log.e(TAG, "Firestore save failed: " + task.getException());
                        mAuth.getCurrentUser().delete().addOnCompleteListener(deleteTask -> {
                            if (deleteTask.isSuccessful()) {
                                Log.d(TAG, "User deleted due to registration failure");
                            }
                        });
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    buttonRegister.setEnabled(true);
                    Log.e(TAG, "Firestore save error: " + e.getMessage());
                    Toast.makeText(this, "Network error during registration", Toast.LENGTH_LONG).show();
                });
    }

    private void uploadManagerDocument(String uid, String fullName, String email, String phone, String userType, AlertDialog dialog, long startTime) {
        Log.d(TAG, "Starting upload with documentUri: " + (documentUri != null ? documentUri.toString() : "null"));
        if (documentUri == null) {
            Log.w(TAG, "No document selected, proceeding without upload");
            saveManagerWithDocument(uid, fullName, email, phone, null, dialog, startTime);
            return;
        }

        // Check if Activity is finishing
        if (isFinishing()) {
            Log.w(TAG, "Activity is finishing, aborting upload");
            dialog.dismiss();
            return;
        }

        // Use the default storage bucket
        StorageReference storageRef = storage.getReference();
        StorageReference docRef = storageRef.child("manager_docs").child(uid).child("license.pdf");

        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Uploading to: " + docRef.getPath());
        Log.d(TAG, "User authenticated: " + (mAuth.getCurrentUser() != null));

        // Create the UploadTask
        UploadTask uploadTask = docRef.putFile(documentUri);

        // Add progress listener
        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressBar.setProgress((int) progress);
            Log.d(TAG, "Upload progress: " + (int) progress + "%");
        });

        // Chain the operations
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null ? task.getException() : new Exception("Upload failed");
            }
            Log.d(TAG, "Upload successful, fetching download URL...");
            return docRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            Log.d(TAG, "Download URL fetched: " + uri.toString());
            progressBar.setVisibility(View.GONE);
            saveManagerWithDocument(uid, fullName, email, phone, uri.toString(), dialog, startTime);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Upload or getDownloadUrl failed: " + e.getMessage(), e);
            progressBar.setVisibility(View.GONE);
            // Add a delay before retrying
            new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                docRef.getDownloadUrl().addOnSuccessListener(retryUri -> {
                    Log.d(TAG, "Retry download URL fetched: " + retryUri.toString());
                    saveManagerWithDocument(uid, fullName, email, phone, retryUri.toString(), dialog, startTime);
                }).addOnFailureListener(retryException -> {
                    Log.e(TAG, "Retry getDownloadUrl failed: " + retryException.getMessage(), retryException);
                    // Fallback: Construct the URL manually
                    String bucket = storage.getReference().getBucket();
                    String path = "manager_docs/" + uid + "/license.pdf";
                    String manualUrl = "https://firebasestorage.googleapis.com/v0/b/" + bucket + "/o/" + Uri.encode(path) + "?alt=media";
                    Log.d(TAG, "Using manually constructed URL: " + manualUrl);
                    saveManagerWithDocument(uid, fullName, email, phone, manualUrl, dialog, startTime);
                });
            }, 2000); // 2-second delay
        });
    }

    private void saveManagerWithDocument(String uid, String fullName, String email, String phone, String documentUrl, AlertDialog dialog, long startTime) {
        Map<String, Object> managerData = new HashMap<>();
        managerData.put("uid", uid);
        managerData.put("fullName", fullName);
        managerData.put("email", email);
        managerData.put("phone", COUNTRY_CODE + phone);
        managerData.put("userType", USER_TYPE_MANAGER);
        managerData.put("isApproved", false);
        managerData.put("licenseDocument", documentUrl != null ? documentUrl : "");
        managerData.put("createdAt", FieldValue.serverTimestamp());
        managerData.put("blocked", false);
        managerData.put("points", null);

        db.collection("users").document(uid)
                .set(managerData)
                .addOnCompleteListener(task -> {
                    dialog.dismiss();
                    buttonRegister.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Log.d(TAG, "Registration completed in " + (System.currentTimeMillis() - startTime) + "ms");
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Manager registration submitted! Your account will be activated within 24 hours after admin verification.",
                                Toast.LENGTH_LONG).show();
                        // Delay transition to ensure Toast is visible
                        new android.os.Handler().postDelayed(() -> {
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        }, 5000); // 5 seconds delay
                    } else {
                        Log.e(TAG, "Firestore save failed: " + task.getException());
                        mAuth.getCurrentUser().delete();
                        Toast.makeText(this, "Failed to save manager data: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    buttonRegister.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Firestore save error: " + e.getMessage());
                    Toast.makeText(this, "Network error during manager registration", Toast.LENGTH_LONG).show();
                });
    }

    private void handleRegistrationError(Exception exception) {
        String errorMessage = "Registration failed";
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            switch (errorCode) {
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    errorMessage = "Email already registered";
                    break;
                case "ERROR_INVALID_EMAIL":
                    errorMessage = "Invalid email format";
                    break;
                case "ERROR_WEAK_PASSWORD":
                    errorMessage = "Password too weak (min 6 chars)";
                    break;
            }
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void completeRegistration(String userType) {
        Toast.makeText(this,
                USER_TYPE_MANAGER.equals(userType) ?
                        "Manager registration submitted! Your account will be activated within 24 hours after verification." :
                        "Registration successful!",
                Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 102 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "SMS permission needed for OTP", Toast.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}