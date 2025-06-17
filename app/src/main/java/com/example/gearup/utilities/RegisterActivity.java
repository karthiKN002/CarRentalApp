package com.example.gearup.utilities;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.gearup.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = "RegisterActivity";
    private static final String COUNTRY_CODE = "+91";
    private static final int PICK_DOCUMENT_REQUEST = 1;
    private static final int REQUEST_VIDEO_PERMISSIONS = 103;

    // Views
    private EditText editTextFullName, editTextEmail, editTextPassword, editTextPhone, editTextOtp;
    private RadioGroup radioGroupUserType;
    private Button buttonSendOtp, buttonVerifyOtp, buttonRegister, buttonUploadDoc, buttonResendOtp;
    private Button  buttonShowCamera;

    private View buttonStartRecording, buttonStopRecording;
    private TextView uploadDocDescription, textViewVideoStatus;
    private LinearLayout otpLayout, videoRecordingLayout, videoActionLayout;
    private ProgressBar progressBar;
    private SurfaceView surfaceViewCamera;

    // State Variables
    private boolean isOtpVerified = false;
    private boolean isDocumentUploaded = false;
    private boolean isVideoRecorded = false;
    private boolean isRecording = false;
    private Uri documentUri;
    private File videoFile;

    // Firebase & Camera
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private Camera camera;
    private MediaRecorder mediaRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        initializeFirebase();
        setupClickListeners();
        updateUiVisibility();
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
        buttonUploadDoc = findViewById(R.id.buttonUploadDoc);
        uploadDocDescription = findViewById(R.id.uploadDocDescription);
        videoActionLayout = findViewById(R.id.videoActionLayout);
        buttonShowCamera = findViewById(R.id.buttonShowCamera);
        videoRecordingLayout = findViewById(R.id.videoRecordingLayout);
        surfaceViewCamera = findViewById(R.id.surfaceViewCamera);
        buttonStartRecording = findViewById(R.id.buttonStartRecording);
        buttonStopRecording = findViewById(R.id.buttonStopRecording);
        textViewVideoStatus = findViewById(R.id.textViewVideoStatus);
        surfaceViewCamera.getHolder().addCallback(this);
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
        buttonUploadDoc.setOnClickListener(v -> openDocumentPicker());
        buttonRegister.setOnClickListener(v -> registerUser());
        buttonResendOtp.setOnClickListener(v -> resendVerificationCode());
        buttonShowCamera.setOnClickListener(v -> handleShowCameraClick());
        buttonStartRecording.setOnClickListener(v -> {
            if (!isRecording) {
                if (!areVideoPermissionsGranted()) {
                    requestVideoPermissions();
                    return;
                }
                buttonStartRecording.setVisibility(View.GONE);
                buttonStopRecording.setVisibility(View.VISIBLE);
                startRecordingUI();
                startRecording(); // your existing method
            }
        });

        buttonStopRecording.setOnClickListener(v -> {
            if (isRecording) {
                buttonStopRecording.setVisibility(View.GONE);
                buttonStartRecording.setVisibility(View.VISIBLE);
                stopRecordingUI();
                stopRecording(); // your existing method
            }
        });


    }
    private void startRecordingUI() {
        View buttonStartRecording = findViewById(R.id.buttonStartRecording);
        View buttonStopRecording = findViewById(R.id.buttonStopRecording);

        buttonStartRecording.setVisibility(View.GONE);
        buttonStopRecording.setVisibility(View.VISIBLE);
    }

    private void stopRecordingUI() {
        View buttonStartRecording = findViewById(R.id.buttonStartRecording);
        View buttonStopRecording = findViewById(R.id.buttonStopRecording);

        buttonStopRecording.setVisibility(View.GONE);
        buttonStartRecording.setVisibility(View.VISIBLE);
    }


    private void updateUiVisibility() {
        boolean managerFlowActive = isManagerSelected() && isOtpVerified;
        uploadDocDescription.setVisibility(managerFlowActive ? View.VISIBLE : View.GONE);
        buttonUploadDoc.setVisibility(managerFlowActive ? View.VISIBLE : View.GONE);
        videoActionLayout.setVisibility(managerFlowActive ? View.VISIBLE : View.GONE);
        if (videoRecordingLayout.getVisibility() == View.VISIBLE && !managerFlowActive) {
            videoRecordingLayout.setVisibility(View.GONE);
            releaseCamera();
        }
        if (isVideoRecorded) {
            textViewVideoStatus.setVisibility(View.VISIBLE);
            buttonShowCamera.setText("Re-record Video");
        } else {
            textViewVideoStatus.setVisibility(View.GONE);
            buttonShowCamera.setText("Record Verification Video");
        }
        buttonRegister.setVisibility(isOtpVerified ? View.VISIBLE : View.GONE);
    }

    private void handleShowCameraClick() {
        Log.d(TAG, "Camera permission: " + (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED));
        Log.d(TAG, "Audio permission: " + (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.d(TAG, "Storage permission: " + (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
        }
        if (areVideoPermissionsGranted()) {
            showCameraPreview();
        } else {
            requestVideoPermissions();
        }
    }

    private void showCameraPreview() {
        videoActionLayout.setVisibility(View.GONE);
        videoRecordingLayout.setVisibility(View.VISIBLE);
        initializeCamera();
    }

    private boolean areVideoPermissionsGranted() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean storageGranted = true; // Default to true for Android 10+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            storageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        Log.d(TAG, "Permissions - Camera: " + cameraGranted + ", Audio: " + audioGranted + ", Storage: " + storageGranted);
        return cameraGranted && audioGranted && storageGranted;
    }

    private void requestVideoPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        ActivityCompat.requestPermissions(this,
                permissions.toArray(new String[0]),
                REQUEST_VIDEO_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (areVideoPermissionsGranted()) {
                Log.d(TAG, "All required permissions granted, showing camera preview");
                showCameraPreview();
            } else {
                Log.d(TAG, "Permissions denied");
                Toast.makeText(this, "Camera and Audio permissions are required to record video.", Toast.LENGTH_LONG).show();
            }
        }
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
                        Toast.makeText(RegisterActivity.this, "Verification failed. Check your number or network. " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Phone Auth Failed", e);
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

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_DOCUMENT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_DOCUMENT_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            documentUri = data.getData();
            isDocumentUploaded = true;
            buttonUploadDoc.setText("License Uploaded ✓");
        }
    }

    private void initializeCamera() {
        if (camera != null) {
            Log.d(TAG, "Camera already initialized");
            return;
        }
        if (!areVideoPermissionsGranted()) {
            Log.e(TAG, "Permissions not granted for camera initialization");
            Toast.makeText(this, "Permissions required to access camera.", Toast.LENGTH_SHORT).show();
            requestVideoPermissions();
            return;
        }
        try {
            camera = Camera.open();
            if (camera == null) {
                Log.e(TAG, "No camera available");
                Toast.makeText(this, "No camera available on this device.", Toast.LENGTH_SHORT).show();
                return;
            }
            camera.setDisplayOrientation(90);
            if (surfaceViewCamera.getHolder() != null && surfaceViewCamera.getHolder().getSurface().isValid()) {
                camera.setPreviewDisplay(surfaceViewCamera.getHolder());
                camera.startPreview();
                Log.d(TAG, "Camera preview started");
            } else {
                Log.e(TAG, "Surface not valid for camera preview");
                Toast.makeText(this, "Camera preview surface not ready.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize camera", e);
            Toast.makeText(this, "Failed to initialize camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            releaseCamera();
        }
    }

    private void startRecording() {
        if (isRecording || camera == null) {
            Log.e(TAG, "Cannot start recording: isRecording=" + isRecording + ", camera=" + (camera == null));
            Toast.makeText(this, "Cannot start recording. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!areVideoPermissionsGranted()) {
            Log.e(TAG, "Permissions not granted for recording");
            Toast.makeText(this, "Permissions required to record video.", Toast.LENGTH_SHORT).show();
            requestVideoPermissions();
            return;
        }
        try {
            camera.unlock();
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setCamera(camera);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setOrientationHint(90);
            File moviesDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (moviesDir != null && !moviesDir.exists()) {
                moviesDir.mkdirs();
                Log.d(TAG, "Created directory: " + moviesDir.getAbsolutePath());
            }
            videoFile = new File(moviesDir, "verification_" + System.currentTimeMillis() + ".mp4");
            mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
            mediaRecorder.setPreviewDisplay(surfaceViewCamera.getHolder().getSurface());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            buttonStartRecording.setEnabled(false);
            buttonStopRecording.setEnabled(true);
            Log.d(TAG, "Recording started, output file: " + videoFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            Toast.makeText(this, "Failed to start recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            releaseMediaRecorder();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting recording", e);
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            releaseMediaRecorder();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                isRecording = false;
                isVideoRecorded = true;

                videoRecordingLayout.setVisibility(View.GONE); // ⬅️ hide camera preview
                videoActionLayout.setVisibility(View.GONE);    // ⬅️ optional: hide button layout

                textViewVideoStatus.setVisibility(View.VISIBLE);

                Toast.makeText(this, "Video recorded successfully!", Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Log.e(TAG, "Error stopping recording", e);
                Toast.makeText(this, "Recording failed. Try again.", Toast.LENGTH_SHORT).show();
            } finally {
                releaseMediaRecorder();
                if (camera != null) {
                    try {
                        camera.lock();
                        camera.startPreview();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to lock camera", e);
                    }
                }
            }
        }
    }


    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            Log.d(TAG, "MediaRecorder released");
        }
    }

    private void releaseCamera() {
        releaseMediaRecorder();
        if (camera != null) {
            try {
                camera.stopPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping camera preview", e);
            }
            camera.release();
            camera = null;
            Log.d(TAG, "Camera released");
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (camera != null) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
                Log.d(TAG, "Surface created, camera preview started");
            } catch (IOException e) {
                Log.e(TAG, "Error setting camera preview", e);
                Toast.makeText(this, "Failed to set camera preview.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "Surface created but camera is null, initializing");
            initializeCamera();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface changed");
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        releaseCamera();
        Log.d(TAG, "Surface destroyed, camera released");
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        Log.d(TAG, "Activity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isManagerSelected() && isOtpVerified) {
            updateUiVisibility();
        }
        Log.d(TAG, "Activity resumed");
    }

    private boolean isManagerSelected() {
        return radioGroupUserType.getCheckedRadioButtonId() == R.id.radioManager;
    }

    private void registerUser() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters.");
            return;
        }
        if (isManagerSelected()) {
            if (!isDocumentUploaded) {
                Toast.makeText(this, "Please upload the store license.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isVideoRecorded) {
                Toast.makeText(this, "Please record the verification video.", Toast.LENGTH_SHORT).show();
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
                                uploadFilesAndSaveManagerData(user.getUid(), fullName, email, phone);
                            } else {
                                saveCustomerData(user.getUid(), fullName, email, phone);
                            }
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        buttonRegister.setEnabled(true);
                        if (task.getException() instanceof FirebaseAuthException) {
                            String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(errorCode)) {
                                Toast.makeText(this, "The email address is already in use.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void saveCustomerData(String uid, String fullName, String email, String phone) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("phone", COUNTRY_CODE + phone);
        userData.put("userType", "customer");
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

    private void uploadFilesAndSaveManagerData(String uid, String fullName, String email, String phone) {
        StorageReference docRef = storage.getReference().child("manager_docs/" + uid + "/license.pdf");
        StorageReference videoRef = storage.getReference().child("manager_videos/" + uid + "/verification_video.mp4");
        docRef.putFile(documentUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) { throw task.getException(); }
                    return docRef.getDownloadUrl();
                }).continueWithTask(task -> {
                    String docUrl = task.getResult().toString();
                    return videoRef.putFile(Uri.fromFile(videoFile)).continueWithTask(videoTask -> {
                        if (!videoTask.isSuccessful()) { throw videoTask.getException(); }
                        return videoRef.getDownloadUrl().addOnSuccessListener(videoUri -> {
                            saveManagerData(uid, fullName, email, phone, docUrl, videoUri.toString());
                        });
                    });
                }).addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonRegister.setEnabled(true);
                    Toast.makeText(this, "File upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveManagerData(String uid, String fullName, String email, String phone, String docUrl, String videoUrl) {
        Map<String, Object> managerData = new HashMap<>();
        managerData.put("uid", uid);
        managerData.put("fullName", fullName);
        managerData.put("email", email);
        managerData.put("phone", COUNTRY_CODE + phone);
        managerData.put("userType", "manager");
        managerData.put("licenseDocument", docUrl);
        managerData.put("storeVideo", videoUrl);
        managerData.put("isApproved", false);
        managerData.put("blocked", false);
        managerData.put("createdAt", FieldValue.serverTimestamp());
        db.collection("users").document(uid).set(managerData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Registration submitted for approval!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonRegister.setEnabled(true);
                    Toast.makeText(this, "Failed to save manager data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}