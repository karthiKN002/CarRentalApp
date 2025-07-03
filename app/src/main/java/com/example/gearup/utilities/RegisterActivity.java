package com.example.gearup.utilities;

import com.google.firebase.firestore.GeoPoint;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private GeoPoint geoPoint;
    private static final String TAG = "RegisterActivity";
    private static final String COUNTRY_CODE = "+91";
    private static final int PICK_AADHAR_REQUEST = 1;
    private static final int PICK_PAN_REQUEST = 2;
    private static final int PICK_LICENSE_REQUEST = 3;
    private static final int PICK_PASSBOOK_REQUEST = 4;
    private static final int PICK_PHOTO_REQUEST = 5;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 100;
    private static final int MAP_PICKER_REQUEST_CODE = 101;

    // Views
    private EditText editTextFullName, editTextEmail, editTextPassword, editTextPhone, editTextOtp, editTextLocation, editTextBankAccount, editTextIfscCode, editTextBusinessAddress;
    private RadioGroup radioGroupUserType;
    private Button buttonSendOtp, buttonVerifyOtp, buttonRegister, buttonUploadAadhar, buttonUploadPan, buttonUploadLicense, buttonUploadPassbook, buttonUploadMechanicPhoto, buttonSelectLocation;
    private Button buttonResendOtp;
    private TextView uploadAadharDescription, uploadPanDescription, uploadLicenseDescription, uploadPassbookDescription, uploadPhotoDescription;
    private LinearLayout otpLayout;
    private ProgressBar progressBar;
    private CheckBox checkBoxAgreement;

    // State Variables
    private boolean isOtpVerified = false;
    private boolean isAadharUploaded = false;
    private boolean isPanUploaded = false;
    private boolean isLicenseUploaded = false;
    private boolean isPassbookUploaded = false;
    private boolean isMechanicPhotoUploaded = false;
    private Uri aadharUri, panUri, licenseUri, passbookUri, mechanicPhotoUri;
    private GeoPoint userLocation;
    private String locationString;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;

    // Places API
    private PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Places API
        if (!Places.isInitialized()) {
            if (TextUtils.isEmpty(BuildConfig.GOOGLE_MAPS_API_KEY)) {
                Log.e(TAG, "Google Maps API key is missing");
                Toast.makeText(this, "Error: Google Maps API key is missing.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            Places.initialize(getApplicationContext(), BuildConfig.GOOGLE_MAPS_API_KEY);
        }
        placesClient = Places.createClient(this);

        initializeViews();
        initializeFirebase();
        setupClickListeners();
        if (savedInstanceState != null) {
            aadharUri = savedInstanceState.getParcelable("aadharUri");
            panUri = savedInstanceState.getParcelable("panUri");
            licenseUri = savedInstanceState.getParcelable("licenseUri");
            passbookUri = savedInstanceState.getParcelable("passbookUri");
            mechanicPhotoUri = savedInstanceState.getParcelable("mechanicPhotoUri");
            isAadharUploaded = savedInstanceState.getBoolean("isAadharUploaded");
            isPanUploaded = savedInstanceState.getBoolean("isPanUploaded");
            isLicenseUploaded = savedInstanceState.getBoolean("isLicenseUploaded");
            isPassbookUploaded = savedInstanceState.getBoolean("isPassbookUploaded");
            isMechanicPhotoUploaded = savedInstanceState.getBoolean("isMechanicPhotoUploaded");
            isOtpVerified = savedInstanceState.getBoolean("isOtpVerified");
            userLocation = (GeoPoint) savedInstanceState.getSerializable("userLocation");
            locationString = savedInstanceState.getString("locationString");
            if (savedInstanceState.containsKey("location_lat") && savedInstanceState.containsKey("location_lng")) {
                double latitude = savedInstanceState.getDouble("location_lat");
                double longitude = savedInstanceState.getDouble("location_lng");
                geoPoint = new GeoPoint(latitude, longitude);
            }
        }
        updateUiVisibility();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (geoPoint != null) {
            outState.putDouble("location_lat", geoPoint.getLatitude());
            outState.putDouble("location_lng", geoPoint.getLongitude());
        }
        outState.putParcelable("aadharUri", aadharUri);
        outState.putParcelable("panUri", panUri);
        outState.putParcelable("licenseUri", licenseUri);
        outState.putParcelable("passbookUri", passbookUri);
        outState.putParcelable("mechanicPhotoUri", mechanicPhotoUri);
        outState.putBoolean("isAadharUploaded", isAadharUploaded);
        outState.putBoolean("isPanUploaded", isPanUploaded);
        outState.putBoolean("isLicenseUploaded", isLicenseUploaded);
        outState.putBoolean("isPassbookUploaded", isPassbookUploaded);
        outState.putBoolean("isMechanicPhotoUploaded", isMechanicPhotoUploaded);
        outState.putBoolean("isOtpVerified", isOtpVerified);
        outState.putSerializable("location", (Serializable) geoPoint);
        outState.putString("locationString", locationString);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down Places client to prevent leaks
        if (placesClient != null) {
            Log.d(TAG, "PlacesClient cleaned up");
        }
    }

    private void initializeViews() {
        radioGroupUserType = findViewById(R.id.radioGroupUserType);
        buttonSendOtp = findViewById(R.id.buttonSendOtp);
        buttonVerifyOtp = findViewById(R.id.buttonVerifyOtp);
        buttonRegister = findViewById(R.id.buttonRegister);
        otpLayout = findViewById(R.id.otpLayout);
        progressBar = findViewById(R.id.progressBar);
        buttonResendOtp = findViewById(R.id.buttonResendOtp);
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextOtp = findViewById(R.id.editTextOtp);
        buttonUploadAadhar = findViewById(R.id.buttonUploadAadhar);
        buttonUploadPan = findViewById(R.id.buttonUploadPan);
        buttonUploadLicense = findViewById(R.id.buttonUploadLicense);
        buttonUploadPassbook = findViewById(R.id.buttonUploadPassbook);
        buttonUploadMechanicPhoto = findViewById(R.id.buttonUploadMechanicPhoto);
        uploadAadharDescription = findViewById(R.id.uploadAadharDescription);
        uploadPanDescription = findViewById(R.id.uploadPanDescription);
        uploadLicenseDescription = findViewById(R.id.uploadLicenseDescription);
        uploadPassbookDescription = findViewById(R.id.uploadPassbookDescription);
        uploadPhotoDescription = findViewById(R.id.uploadPhotoDescription);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextBankAccount = findViewById(R.id.editTextBankAccount);
        editTextIfscCode = findViewById(R.id.editTextIfscCode);
        editTextBusinessAddress = findViewById(R.id.editTextBusinessAddress);
        checkBoxAgreement = findViewById(R.id.checkBoxAgreement);
        buttonSelectLocation = findViewById(R.id.buttonSelectLocation);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void setupClickListeners() {
        radioGroupUserType.setOnCheckedChangeListener((group, checkedId) -> updateUiVisibility());
        buttonSendOtp.setOnClickListener(v -> sendVerificationCode());
        buttonVerifyOtp.setOnClickListener(v -> verifyCode());
        buttonUploadAadhar.setOnClickListener(v -> openDocumentPicker(PICK_AADHAR_REQUEST));
        buttonUploadPan.setOnClickListener(v -> openDocumentPicker(PICK_PAN_REQUEST));
        buttonUploadLicense.setOnClickListener(v -> openDocumentPicker(PICK_LICENSE_REQUEST));
        buttonUploadPassbook.setOnClickListener(v -> openImagePicker(PICK_PASSBOOK_REQUEST));
        buttonUploadMechanicPhoto.setOnClickListener(v -> openImagePicker(PICK_PHOTO_REQUEST));
        buttonRegister.setOnClickListener(v -> showAgreementDialog());
        buttonResendOtp.setOnClickListener(v -> resendVerificationCode());
        checkBoxAgreement.setOnCheckedChangeListener((buttonView, isChecked) -> buttonRegister.setEnabled(isChecked));
        editTextLocation.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                openPlaceAutocomplete();
            }
        });
        buttonSelectLocation.setOnClickListener(v -> openMapPicker());
    }

    private void showAgreementDialog() {
        String message = isManagerSelected() ?
                "Required Documents for Manager:\n- Aadhar Card (PDF)\n- PAN Card (PDF)\n- Rent-a-Cab License (PDF)\n- Passbook Image (JPEG/PNG)\n- Business Address\n- Bank Account Details\n\nBy registering, you agree to provide valid documents for verification." :
                isMechanicSelected() ?
                        "Required Documents for Mechanic:\n- Aadhar Card or PAN Card (PDF)\n- Passport-like Photo (JPEG/PNG)\n- Bank Account Details\n\nBy registering, you agree to provide valid documents for verification." :
                        "Required for Customer:\n- Valid Email and Phone\n\nBy registering, you agree to our terms.";

        new AlertDialog.Builder(this)
                .setTitle("Registration Agreement")
                .setMessage(message)
                .setPositiveButton("Agree", (dialog, which) -> {
                    checkBoxAgreement.setChecked(true);
                    registerUser();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUiVisibility() {
        boolean isManagerOrMechanic = isManagerSelected() || isMechanicSelected();
        boolean isManager = isManagerSelected() && isOtpVerified;
        boolean isMechanic = isMechanicSelected() && isOtpVerified;

        uploadAadharDescription.setVisibility(isManagerOrMechanic ? View.VISIBLE : View.GONE);
        buttonUploadAadhar.setVisibility(isManagerOrMechanic ? View.VISIBLE : View.GONE);
        uploadPanDescription.setVisibility(isManager ? View.VISIBLE : View.GONE);
        buttonUploadPan.setVisibility(isManager ? View.VISIBLE : View.GONE);
        uploadLicenseDescription.setVisibility(isManager ? View.VISIBLE : View.GONE);
        buttonUploadLicense.setVisibility(isManager ? View.VISIBLE : View.GONE);
        uploadPassbookDescription.setVisibility(isManager ? View.VISIBLE : View.GONE);
        buttonUploadPassbook.setVisibility(isManager ? View.VISIBLE : View.GONE);
        uploadPhotoDescription.setVisibility(isMechanic ? View.VISIBLE : View.GONE);
        buttonUploadMechanicPhoto.setVisibility(isMechanic ? View.VISIBLE : View.GONE);
        editTextLocation.setVisibility(isManagerOrMechanic ? View.VISIBLE : View.GONE);
        editTextBankAccount.setVisibility(isManagerOrMechanic ? View.VISIBLE : View.GONE);
        editTextIfscCode.setVisibility(isManagerOrMechanic ? View.VISIBLE : View.GONE);
        editTextBusinessAddress.setVisibility(isManager ? View.VISIBLE : View.GONE);
        checkBoxAgreement.setVisibility(View.VISIBLE);
        buttonRegister.setVisibility(isOtpVerified ? View.VISIBLE : View.GONE);
        buttonRegister.setEnabled(checkBoxAgreement.isChecked());
        buttonSelectLocation.setVisibility(isManagerOrMechanic ? View.VISIBLE : View.GONE);

        // Update button text based on flags
        buttonUploadAadhar.setText(isAadharUploaded ? "Aadhar Uploaded ✓" : "Upload Aadhar");
        buttonUploadPan.setText(isPanUploaded ? "PAN Uploaded ✓" : "Upload PAN");
        buttonUploadLicense.setText(isLicenseUploaded ? "License Uploaded ✓" : "Upload License");
        buttonUploadPassbook.setText(isPassbookUploaded ? "Passbook Uploaded ✓" : "Upload Passbook");
        buttonUploadMechanicPhoto.setText(isMechanicPhotoUploaded ? "Photo Uploaded ✓" : "Upload Photo");
        Log.d(TAG, "UI updated: Aadhar=" + isAadharUploaded + ", Pan=" + isPanUploaded + ", License=" + isLicenseUploaded + ", Passbook=" + isPassbookUploaded + ", Photo=" + isMechanicPhotoUploaded);
    }

    private void openPlaceAutocomplete() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    private void openMapPicker() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        if (userLocation != null) {
            intent.putExtra("latitude", userLocation.getLatitude());
            intent.putExtra("longitude", userLocation.getLongitude());
        }
        startActivityForResult(intent, MAP_PICKER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + (data != null ? data.toString() : "null"));

        if (resultCode != RESULT_OK || data == null) {
            Log.w(TAG, "onActivityResult: Invalid result, requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
            Toast.makeText(this, "Selection failed", Toast.LENGTH_SHORT).show();
            return;
        }

        // Handle non-URI results (Autocomplete and Map Picker)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            editTextLocation.setText(place.getName());
            if (place.getLatLng() != null) {
                userLocation = new GeoPoint(place.getLatLng().latitude, place.getLatLng().longitude);
                locationString = place.getAddress();
            }
            Log.d(TAG, "Autocomplete: place=" + place.getName() + ", latLng=" + place.getLatLng());
            updateUiVisibility();
            return;
        } else if (requestCode == MAP_PICKER_REQUEST_CODE) {
            double latitude = data.getDoubleExtra("latitude", 0.0);
            double longitude = data.getDoubleExtra("longitude", 0.0);
            String address = data.getStringExtra("address");
            if (latitude != 0.0 && longitude != 0.0) {
                userLocation = new GeoPoint(latitude, longitude);
                locationString = address;
                editTextLocation.setText(address);
            } else {
                Log.w(TAG, "MapPicker: Invalid coordinates, lat=" + latitude + ", lng=" + longitude);
                Toast.makeText(this, "Invalid location selected", Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "MapPicker: lat=" + latitude + ", lng=" + longitude + ", address=" + address);
            updateUiVisibility();
            return;
        }

        // Handle URI-based results (documents and images)
        Uri uri = data.getData();
        if (uri == null) {
            Log.w(TAG, "onActivityResult: Null URI for requestCode=" + requestCode);
            Toast.makeText(this, "Failed to select file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Attempt to persist URI permission
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Log.d(TAG, "Persisted URI permission for: " + uri);
        } catch (SecurityException e) {
            Log.w(TAG, "Failed to persist URI permission: " + e.getMessage());
            // Proceed without persisting, as temporary access is available
        }

        // Process document/image URIs
        switch (requestCode) {
            case PICK_AADHAR_REQUEST:
                aadharUri = uri;
                isAadharUploaded = true;
                buttonUploadAadhar.setText("Aadhar Uploaded ✓");
                Toast.makeText(this, "Aadhar document selected", Toast.LENGTH_SHORT).show();
                break;
            case PICK_PAN_REQUEST:
                panUri = uri;
                isPanUploaded = true;
                buttonUploadPan.setText("PAN Uploaded ✓");
                Toast.makeText(this, "PAN document selected", Toast.LENGTH_SHORT).show();
                break;
            case PICK_LICENSE_REQUEST:
                licenseUri = uri;
                isLicenseUploaded = true;
                buttonUploadLicense.setText("License Uploaded ✓");
                Toast.makeText(this, "License document selected", Toast.LENGTH_SHORT).show();
                break;
            case PICK_PASSBOOK_REQUEST:
                passbookUri = uri;
                isPassbookUploaded = true;
                buttonUploadPassbook.setText("Passbook Uploaded ✓");
                Toast.makeText(this, "Passbook image selected", Toast.LENGTH_SHORT).show();
                break;
            case PICK_PHOTO_REQUEST:
                mechanicPhotoUri = uri;
                isMechanicPhotoUploaded = true;
                buttonUploadMechanicPhoto.setText("Photo Uploaded ✓");
                Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show();
                break;
            default:
                Log.w(TAG, "Unhandled requestCode: " + requestCode);
                Toast.makeText(this, "Unknown selection", Toast.LENGTH_SHORT).show();
        }
        updateUiVisibility();
    }

    private void sendVerificationCode() {
        String phoneNumber = editTextPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() != 10) {
            editTextPhone.setError("A valid 10-digit phone number is required.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSendOtp.setEnabled(false);

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                COUNTRY_CODE + phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        progressBar.setVisibility(View.GONE);
                        buttonSendOtp.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        progressBar.setVisibility(View.GONE);
                        buttonSendOtp.setEnabled(true);
                        verificationId = verId;
                        resendingToken = token;
                        otpLayout.setVisibility(View.VISIBLE);
                        buttonSendOtp.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void resendVerificationCode() {
        String phoneNumber = editTextPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() != 10) {
            editTextPhone.setError("A valid 10-digit phone number is required.");
            return;
        }
        if (resendingToken == null) {
            Toast.makeText(this, "Cannot resend code at this moment.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                COUNTRY_CODE + phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) { signInWithCredential(credential); }
                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onCodeSent(@NonNull String verId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        progressBar.setVisibility(View.GONE);
                        verificationId = verId;
                        resendingToken = token;
                        Toast.makeText(RegisterActivity.this, "OTP Resent", Toast.LENGTH_SHORT).show();
                    }
                },
                resendingToken
        );
    }

    private void verifyCode() {
        String code = editTextOtp.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            editTextOtp.setError("Enter OTP");
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                isOtpVerified = true;
                Toast.makeText(this, "Phone Verified!", Toast.LENGTH_SHORT).show();
                otpLayout.setVisibility(View.GONE);
                updateUiVisibility();
            } else {
                Toast.makeText(this, "Incorrect OTP.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isManagerSelected() {
        return radioGroupUserType.getCheckedRadioButtonId() == R.id.radioManager;
    }

    private boolean isMechanicSelected() {
        return radioGroupUserType.getCheckedRadioButtonId() == R.id.radioMechanic;
    }

    private boolean isUriAccessible(Uri uri) {
        if (uri == null) return false;
        try {
            getContentResolver().openInputStream(uri).close();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "URI access failed: " + uri + ", error: " + e.getMessage());
            return false;
        }
    }

    private void registerUser() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String bankAccount = editTextBankAccount.getText().toString().trim();
        String ifscCode = editTextIfscCode.getText().toString().trim();
        String businessAddress = editTextBusinessAddress.getText().toString().trim();

        // Log current state
        Log.d(TAG, "Registering user: Manager=" + isManagerSelected() + ", Mechanic=" + isMechanicSelected());
        Log.d(TAG, "Documents: Aadhar=" + isAadharUploaded + ", Pan=" + isPanUploaded + ", License=" + isLicenseUploaded + ", Passbook=" + isPassbookUploaded + ", Photo=" + isMechanicPhotoUploaded);
        Log.d(TAG, "URIs: Aadhar=" + aadharUri + ", Pan=" + panUri + ", License=" + licenseUri + ", Passbook=" + passbookUri + ", Photo=" + mechanicPhotoUri);

        // Basic validation
        if (TextUtils.isEmpty(fullName)) {
            editTextFullName.setError("Full name is required.");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required.");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required.");
            return;
        }
        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters.");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            editTextPhone.setError("Phone number is required.");
            return;
        }
        if (TextUtils.isEmpty(location)) {
            editTextLocation.setError("Location is required.");
            return;
        }
        if ((isManagerSelected() || isMechanicSelected()) && (TextUtils.isEmpty(bankAccount) || TextUtils.isEmpty(ifscCode))) {
            editTextBankAccount.setError("Bank details are required.");
            return;
        }
        if (userLocation == null) {
            Toast.makeText(this, "Please select a valid location.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkBoxAgreement.isChecked()) {
            Toast.makeText(this, "Please agree to the terms.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Manager document validation
        if (isManagerSelected()) {
            if (!isAadharUploaded || !isUriAccessible(aadharUri)) {
                Toast.makeText(this, "Please upload a valid Aadhar card.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isPanUploaded || !isUriAccessible(panUri)) {
                Toast.makeText(this, "Please upload a valid PAN card.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isLicenseUploaded || !isUriAccessible(licenseUri)) {
                Toast.makeText(this, "Please upload a valid Rent-a-Cab License.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isPassbookUploaded || !isUriAccessible(passbookUri)) {
                Toast.makeText(this, "Please upload a valid Passbook image.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(businessAddress)) {
                editTextBusinessAddress.setError("Business address is required.");
                return;
            }
        }

        // Mechanic document validation
        if (isMechanicSelected()) {
            if ((!isAadharUploaded && !isPanUploaded) || (isAadharUploaded && !isUriAccessible(aadharUri)) || (isPanUploaded && !isUriAccessible(panUri))) {
                Toast.makeText(this, "Please upload a valid Aadhar or PAN card.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isMechanicPhotoUploaded || !isUriAccessible(mechanicPhotoUri)) {
                Toast.makeText(this, "Please upload a valid passport-like photo.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonRegister.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (isManagerSelected()) {
                                uploadManagerFilesAndSaveData(user.getUid(), fullName, email, phone, location, bankAccount, ifscCode, businessAddress);
                            } else if (isMechanicSelected()) {
                                uploadMechanicFilesAndSaveData(user.getUid(), fullName, email, phone, location, bankAccount, ifscCode);
                            } else {
                                saveCustomerData(user.getUid(), fullName, email, phone, location);
                            }
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        buttonRegister.setEnabled(true);
                        Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveCustomerData(String uid, String fullName, String email, String phone, String location) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("phone", COUNTRY_CODE + phone);
        userData.put("userType", "customer");
        userData.put("location", userLocation);
        userData.put("locationString", locationString);
        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("blocked", false);
        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonRegister.setEnabled(true);
                    Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadManagerFilesAndSaveData(String uid, String fullName, String email, String phone, String location, String bankAccount, String ifscCode, String businessAddress) {
        StorageReference aadharRef = storage.getReference().child("manager_docs/" + uid + "/aadhar.pdf");
        StorageReference panRef = storage.getReference().child("manager_docs/" + uid + "/pan.pdf");
        StorageReference licenseRef = storage.getReference().child("manager_docs/" + uid + "/rent_a_cab_license.pdf");
        StorageReference passbookRef = storage.getReference().child("manager_docs/" + uid + "/passbook.jpg");

        Map<String, String> urls = new HashMap<>();
        ArrayList<StorageReference> refs = new ArrayList<>();
        ArrayList<Uri> uris = new ArrayList<>();

        refs.add(aadharRef);
        refs.add(panRef);
        refs.add(licenseRef);
        refs.add(passbookRef);
        uris.add(aadharUri);
        uris.add(panUri);
        uris.add(licenseUri);
        uris.add(passbookUri);

        for (int i = 0; i < refs.size(); i++) {
            StorageReference ref = refs.get(i);
            Uri uri = uris.get(i);
            ref.putFile(uri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) { throw task.getException(); }
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(url -> {
                        urls.put(ref.getName(), url.toString());
                        if (urls.size() == refs.size()) {
                            saveManagerData(uid, fullName, email, phone, location, bankAccount, ifscCode, businessAddress, urls.get("aadhar.pdf"), urls.get("pan.pdf"), urls.get("rent_a_cab_license.pdf"), urls.get("passbook.jpg"));
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        buttonRegister.setEnabled(true);
                        Toast.makeText(this, "File upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void saveManagerData(String uid, String fullName, String email, String phone, String location, String bankAccount, String ifscCode, String businessAddress, String aadharUrl, String panUrl, String licenseUrl, String passbookUrl) {
        Map<String, Object> managerData = new HashMap<>();
        managerData.put("uid", uid);
        managerData.put("fullName", fullName);
        managerData.put("email", email);
        managerData.put("phone", COUNTRY_CODE + phone);
        managerData.put("userType", "manager");
        managerData.put("aadharDocument", aadharUrl);
        managerData.put("panDocument", panUrl);
        managerData.put("licenseDocument", licenseUrl);
        managerData.put("passbookImage", passbookUrl);
        managerData.put("businessAddress", businessAddress);
        managerData.put("bankAccount", bankAccount);
        managerData.put("ifscCode", ifscCode);
        managerData.put("location", userLocation);
        managerData.put("locationString", locationString);
        managerData.put("isApproved", false);
        managerData.put("blocked", false);
        managerData.put("createdAt", FieldValue.serverTimestamp());
        db.collection("users").document(uid).set(managerData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Registration submitted for approval!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonRegister.setEnabled(true);
                    Toast.makeText(this, "Failed to save manager data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadMechanicFilesAndSaveData(String uid, String fullName, String email, String phone, String location, String bankAccount, String ifscCode) {
        StorageReference identityRef = storage.getReference().child("mechanic_docs/" + uid + "/" + (isAadharUploaded ? "aadhar.pdf" : "pan.pdf"));
        StorageReference photoRef = storage.getReference().child("mechanic_docs/" + uid + "/photo.jpg");

        Map<String, String> urls = new HashMap<>();
        identityRef.putFile(isAadharUploaded ? aadharUri : panUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) { throw task.getException(); }
                    return identityRef.getDownloadUrl();
                })
                .addOnSuccessListener(identityUrl -> {
                    urls.put("identity", identityUrl.toString());
                    photoRef.putFile(mechanicPhotoUri)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) { throw task.getException(); }
                                return photoRef.getDownloadUrl();
                            })
                            .addOnSuccessListener(url -> {
                                urls.put("photo", url.toString());
                                saveMechanicData(uid, fullName, email, phone, location, bankAccount, ifscCode, urls.get("identity"), urls.get("photo"));
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                buttonRegister.setEnabled(true);
                                Toast.makeText(this, "Photo upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonRegister.setEnabled(true);
                    Toast.makeText(this, "Identity document upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveMechanicData(String uid, String fullName, String email, String phone, String location, String bankAccount, String ifscCode, String identityUrl, String photoUrl) {
        Map<String, Object> mechanicData = new HashMap<>();
        mechanicData.put("uid", uid);
        mechanicData.put("fullName", fullName);
        mechanicData.put("email", email);
        mechanicData.put("phone", COUNTRY_CODE + phone);
        mechanicData.put("userType", "mechanic");
        mechanicData.put("identityDocument", identityUrl);
        mechanicData.put("photo", photoUrl);
        mechanicData.put("bankAccount", bankAccount);
        mechanicData.put("ifscCode", ifscCode);
        mechanicData.put("location", userLocation);
        mechanicData.put("locationString", locationString);
        mechanicData.put("isApproved", false);
        mechanicData.put("blocked", false);
        mechanicData.put("rating", 0.0);
        mechanicData.put("createdAt", FieldValue.serverTimestamp());
        db.collection("users").document(uid).set(mechanicData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Registration submitted for approval!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonRegister.setEnabled(true);
                    Toast.makeText(this, "Failed to save mechanic data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void openDocumentPicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Log.d(TAG, "Opening document picker for requestCode=" + requestCode);
        try {
            startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open document picker: " + e.getMessage());
            Toast.makeText(this, "Cannot open document picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Log.d(TAG, "Opening image picker for requestCode=" + requestCode);
        try {
            startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open image picker: " + e.getMessage());
            Toast.makeText(this, "Cannot open image picker", Toast.LENGTH_SHORT).show();
        }
    }
}