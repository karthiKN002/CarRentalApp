package com.example.gearup.uiactivities.manager;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gearup.R;
import com.example.gearup.adapters.ImagePreviewAdapter;
import com.example.gearup.states.car.CarAvailabilityState;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AddCarFragment extends Fragment {

    private static final int AUTOCOMPLETE_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int IMAGE_PICKER_REQUEST_CODE = 1;
    private static final int PICK_FC_REQUEST = 2;
    private static final int PICK_RC_REQUEST = 3;
    private static final int PICK_INSURANCE_REQUEST = 4;
    private static final int PICK_PUC_REQUEST = 5;

    private EditText inputCarModel, inputCarBrand, inputCarSeats, inputCarLocation, inputCarPrice, inputCarDescription;
    private Button buttonSelectImages, buttonSubmitCarDetails, buttonUploadFc, buttonUploadRc, buttonUploadInsurance, buttonUploadPuc;
    private ArrayList<Uri> imageUris;
    private ArrayList<String> imageUrls;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private RecyclerView imageRecyclerView;
    private ImagePreviewAdapter imagePreviewAdapter;
    private PlacesClient placesClient;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private Uri fcUri, rcUri, insuranceUri, pucUri;
    private boolean isFcUploaded, isRcUploaded, isInsuranceUploaded, isPucUploaded;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof ManagerDashboardActivity) {
            placesClient = ((ManagerDashboardActivity) getActivity()).getPlacesClient();
        } else {
            Log.e("AddCarFragment", "Parent activity is not ManagerDashboardActivity");
            Toast.makeText(getContext(), "Error: Unable to initialize location services", Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        checkLocationPermission();
        View view = inflater.inflate(R.layout.fragment_add_car, container, false);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        inputCarModel = view.findViewById(R.id.inputCarModel);
        inputCarBrand = view.findViewById(R.id.inputCarBrand);
        inputCarDescription = view.findViewById(R.id.inputCarDescription);
        inputCarSeats = view.findViewById(R.id.inputCarSeats);
        inputCarLocation = view.findViewById(R.id.inputCarLocation);
        inputCarPrice = view.findViewById(R.id.inputCarPrice);
        buttonSelectImages = view.findViewById(R.id.buttonSelectImages);
        buttonSubmitCarDetails = view.findViewById(R.id.buttonSubmitCarDetails);
        imageRecyclerView = view.findViewById(R.id.imageRecyclerView);
        buttonUploadFc = view.findViewById(R.id.buttonUploadFc);
        buttonUploadRc = view.findViewById(R.id.buttonUploadRc);
        buttonUploadInsurance = view.findViewById(R.id.buttonUploadInsurance);
        buttonUploadPuc = view.findViewById(R.id.buttonUploadPuc);

        imageUris = new ArrayList<>();
        imageUrls = new ArrayList<>();
        imagePreviewAdapter = new ImagePreviewAdapter(requireContext(), imageUrls, this::confirmImageRemoval);
        imageRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        imageRecyclerView.setAdapter(imagePreviewAdapter);

        setupClickListeners();
        updateButtonText();

        if (savedInstanceState != null) {
            fcUri = savedInstanceState.getParcelable("fcUri");
            rcUri = savedInstanceState.getParcelable("rcUri");
            insuranceUri = savedInstanceState.getParcelable("insuranceUri");
            pucUri = savedInstanceState.getParcelable("pucUri");
            isFcUploaded = savedInstanceState.getBoolean("isFcUploaded");
            isRcUploaded = savedInstanceState.getBoolean("isRcUploaded");
            isInsuranceUploaded = savedInstanceState.getBoolean("isInsuranceUploaded");
            isPucUploaded = savedInstanceState.getBoolean("isPucUploaded");
            ArrayList<String> savedImageUrls = savedInstanceState.getStringArrayList("imageUrls");
            ArrayList<Uri> savedImageUris = savedInstanceState.getParcelableArrayList("imageUris");
            if (savedImageUrls != null && savedImageUris != null) {
                imageUrls.clear();
                imageUris.clear();
                imageUrls.addAll(savedImageUrls);
                imageUris.addAll(savedImageUris);
                imagePreviewAdapter.updateImages(new ArrayList<>(imageUrls));
            }
            updateButtonText();
        }

        return view;
    }

    private void setupClickListeners() {
        buttonSelectImages.setOnClickListener(v -> selectImages());
        buttonUploadFc.setOnClickListener(v -> openDocumentPicker(PICK_FC_REQUEST));
        buttonUploadRc.setOnClickListener(v -> openDocumentPicker(PICK_RC_REQUEST));
        buttonUploadInsurance.setOnClickListener(v -> openDocumentPicker(PICK_INSURANCE_REQUEST));
        buttonUploadPuc.setOnClickListener(v -> openDocumentPicker(PICK_PUC_REQUEST));
        buttonSubmitCarDetails.setOnClickListener(v -> showDisclaimerDialog());
        inputCarLocation.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                openPlaceAutocomplete();
            }
        });
    }

    private void updateButtonText() {
        buttonUploadFc.setText(isFcUploaded ? "FC Uploaded ✓" : "Upload FC");
        buttonUploadRc.setText(isRcUploaded ? "RC Uploaded ✓" : "Upload RC");
        buttonUploadInsurance.setText(isInsuranceUploaded ? "Insurance Uploaded ✓" : "Upload Insurance");
        buttonUploadPuc.setText(isPucUploaded ? "PUC Uploaded ✓" : "Upload PUC");
    }

    private void selectImages() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, IMAGE_PICKER_REQUEST_CODE);
        } catch (Exception e) {
            Log.e("AddCarFragment", "Failed to open image picker: " + e.getMessage());
            Toast.makeText(requireContext(), "Cannot open image picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDisclaimerDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Disclaimer")
                .setMessage("Please upload photos of the car from all sides (front, back, left, right) and interior. Ensure all documents (FC, RC, Commercial Insurance, PUC) are valid.")
                .setPositiveButton("Agree", (dialog, which) -> uploadCarDetails())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openDocumentPicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Log.d("AddCarFragment", "Opening document picker for requestCode=" + requestCode);
        try {
            startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Log.e("AddCarFragment", "Failed to open document picker: " + e.getMessage());
            Toast.makeText(requireContext(), "Cannot open document picker", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            Log.w("AddCarFragment", "Invalid result: requestCode=" + requestCode + ", resultCode=" + resultCode);
            Toast.makeText(requireContext(), "Selection failed", Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            inputCarLocation.setText(place.getName());
            if (place.getLatLng() != null) {
                latitude = place.getLatLng().latitude;
                longitude = place.getLatLng().longitude;
            }
            Log.d("AddCarFragment", "Autocomplete: place=" + place.getName() + ", latLng=" + place.getLatLng());
            return;
        }

        if (requestCode == IMAGE_PICKER_REQUEST_CODE) {
            ArrayList<Uri> newUris = new ArrayList<>();
            ArrayList<String> newUrls = new ArrayList<>();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    if (isUriAccessible(imageUri)) {
                        newUris.add(imageUri);
                        newUrls.add(imageUri.toString());
                        Log.d("AddCarFragment", "Selected image URI: " + imageUri);
                    } else {
                        Log.w("AddCarFragment", "Inaccessible image URI: " + imageUri);
                        Toast.makeText(requireContext(), "Cannot access selected image", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                if (isUriAccessible(imageUri)) {
                    newUris.add(imageUri);
                    newUrls.add(imageUri.toString());
                    Log.d("AddCarFragment", "Selected image URI: " + imageUri);
                } else {
                    Log.w("AddCarFragment", "Inaccessible image URI: " + imageUri);
                    Toast.makeText(requireContext(), "Cannot access selected image", Toast.LENGTH_SHORT).show();
                }
            }
            if (!newUris.isEmpty()) {
                imageUris.addAll(newUris);
                imageUrls.addAll(newUrls);
                imagePreviewAdapter.addImages(newUrls);
                Log.d("AddCarFragment", "Added " + newUrls.size() + " images, total: " + imageUrls.size());
            } else {
                Log.w("AddCarFragment", "No valid images selected");
            }
        }

        Uri uri = data.getData();
        if (uri == null) {
            Log.w("AddCarFragment", "Null URI for requestCode=" + requestCode);
            Toast.makeText(requireContext(), "Failed to select file", Toast.LENGTH_SHORT).show();
            return;
        }

        String mimeType = requireActivity().getContentResolver().getType(uri);
        if (!"application/pdf".equals(mimeType)) {
            Toast.makeText(requireContext(), "Please select a PDF file", Toast.LENGTH_SHORT).show();
            Log.e("AddCarFragment", "Invalid MIME type: " + mimeType);
            return;
        }

        try {
            requireActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Log.d("AddCarFragment", "Persisted URI permission for: " + uri);
        } catch (SecurityException e) {
            Log.w("AddCarFragment", "Failed to persist URI permission: " + e.getMessage());
            if (!isUriAccessible(uri)) {
                Toast.makeText(requireContext(), "Cannot access selected document", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        switch (requestCode) {
            case PICK_FC_REQUEST:
                fcUri = uri;
                isFcUploaded = isUriAccessible(uri);
                buttonUploadFc.setText(isFcUploaded ? "FC Uploaded ✓" : "Upload FC");
                Toast.makeText(requireContext(), isFcUploaded ? "FC document selected" : "FC document access failed", Toast.LENGTH_SHORT).show();
                break;
            case PICK_RC_REQUEST:
                rcUri = uri;
                isRcUploaded = isUriAccessible(uri);
                buttonUploadRc.setText(isRcUploaded ? "RC Uploaded ✓" : "Upload RC");
                Toast.makeText(requireContext(), isRcUploaded ? "RC document selected" : "RC document access failed", Toast.LENGTH_SHORT).show();
                break;
            case PICK_INSURANCE_REQUEST:
                insuranceUri = uri;
                isInsuranceUploaded = isUriAccessible(uri);
                buttonUploadInsurance.setText(isInsuranceUploaded ? "Insurance Uploaded ✓" : "Upload Insurance");
                Toast.makeText(requireContext(), isInsuranceUploaded ? "Insurance document selected" : "Insurance document access failed", Toast.LENGTH_SHORT).show();
                break;
            case PICK_PUC_REQUEST:
                pucUri = uri;
                isPucUploaded = isUriAccessible(uri);
                buttonUploadPuc.setText(isPucUploaded ? "PUC Uploaded ✓" : "Upload PUC");
                Toast.makeText(requireContext(), isPucUploaded ? "PUC document selected" : "PUC document access failed", Toast.LENGTH_SHORT).show();
                break;
            default:
                Log.w("AddCarFragment", "Unhandled requestCode: " + requestCode);
                Toast.makeText(requireContext(), "Unknown selection", Toast.LENGTH_SHORT).show();
        }
        updateButtonText();
    }

    private boolean isUriAccessible(Uri uri) {
        if (uri == null) return false;
        try {
            requireActivity().getContentResolver().openInputStream(uri).close();
            Log.d("AddCarFragment", "URI is accessible: " + uri);
            return true;
        } catch (Exception e) {
            Log.w("AddCarFragment", "URI access failed: " + uri + ", error: " + e.getMessage());
            return false;
        }
    }

    private void uploadCarDetails() {
        String model = inputCarModel.getText().toString().trim();
        String brand = inputCarBrand.getText().toString().trim();
        String description = inputCarDescription.getText().toString().trim();
        String seatsStr = inputCarSeats.getText().toString().trim();
        String priceStr = inputCarPrice.getText().toString().trim();
        String location = inputCarLocation.getText().toString().trim();

        if (TextUtils.isEmpty(model) || TextUtils.isEmpty(brand) || TextUtils.isEmpty(seatsStr) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(location)) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isFcUploaded || !isUriAccessible(fcUri)) {
            isFcUploaded = false;
            buttonUploadFc.setText("Upload FC");
            Toast.makeText(requireContext(), "Please upload a valid FC document", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isRcUploaded || !isUriAccessible(rcUri)) {
            isRcUploaded = false;
            buttonUploadRc.setText("Upload RC");
            Toast.makeText(requireContext(), "Please upload a valid RC document", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isInsuranceUploaded || !isUriAccessible(insuranceUri)) {
            isInsuranceUploaded = false;
            buttonUploadInsurance.setText("Upload Insurance");
            Toast.makeText(requireContext(), "Please upload a valid Insurance document", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isPucUploaded || !isUriAccessible(pucUri)) {
            isPucUploaded = false;
            buttonUploadPuc.setText("Upload PUC");
            Toast.makeText(requireContext(), "Please upload a valid PUC document", Toast.LENGTH_SHORT).show();
            return;
        }

        int seats;
        double price;
        try {
            seats = Integer.parseInt(seatsStr);
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid seats or price format", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("AddCarFragment", "FC: " + isFcUploaded + ", URI: " + fcUri);
        Log.d("AddCarFragment", "RC: " + isRcUploaded + ", URI: " + rcUri);
        Log.d("AddCarFragment", "Insurance: " + isInsuranceUploaded + ", URI: " + insuranceUri);
        Log.d("AddCarFragment", "PUC: " + isPucUploaded + ", URI: " + pucUri);

        uploadDocumentsAndImages(model, brand, seats, price, location, description);
    }

    private void uploadDocumentsAndImages(String model, String brand, int seats, double price, String location, String description) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please sign in to upload", Toast.LENGTH_SHORT).show();
            return;
        }

        String carId = UUID.randomUUID().toString();
        StorageReference fcRef = storageRef.child("cars/" + carId + "/fc.pdf");
        StorageReference rcRef = storageRef.child("cars/" + carId + "/rc.pdf");
        StorageReference insuranceRef = storageRef.child("cars/" + carId + "/insurance.pdf");
        StorageReference pucRef = storageRef.child("cars/" + carId + "/puc.pdf");

        Map<String, String> documentUrls = new HashMap<>();
        ArrayList<StorageReference> docRefs = new ArrayList<>(Arrays.asList(fcRef, rcRef, insuranceRef, pucRef));
        ArrayList<Uri> docUris = new ArrayList<>(Arrays.asList(fcUri, rcUri, insuranceUri, pucUri));
        ArrayList<String> docTypes = new ArrayList<>(Arrays.asList("fc", "rc", "insurance", "puc"));

        final AtomicInteger uploadedDocs = new AtomicInteger(0);
        for (int i = 0; i < docRefs.size(); i++) {
            StorageReference ref = docRefs.get(i);
            Uri uri = docUris.get(i);
            String docType = docTypes.get(i);
            ref.putFile(uri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(url -> {
                        Log.d("AddCarFragment", "Uploaded " + docType + ": " + url);
                        documentUrls.put(ref.getName(), url.toString());
                        if (uploadedDocs.incrementAndGet() == docRefs.size()) {
                            uploadImages(carId, model, brand, seats, price, location, description, documentUrls);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AddCarFragment", "Failed to upload " + docType + ": " + e.getMessage());
                        Toast.makeText(requireContext(), "Failed to upload " + docType.toUpperCase() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        switch (docType) {
                            case "fc":
                                isFcUploaded = false;
                                buttonUploadFc.setText("Upload FC");
                                break;
                            case "rc":
                                isRcUploaded = false;
                                buttonUploadRc.setText("Upload RC");
                                break;
                            case "insurance":
                                isInsuranceUploaded = false;
                                buttonUploadInsurance.setText("Upload Insurance");
                                break;
                            case "puc":
                                isPucUploaded = false;
                                buttonUploadPuc.setText("Upload PUC");
                                break;
                        }
                        updateButtonText();
                    });
        }
    }

    private void uploadImages(String carId, String model, String brand, int seats, double price, String location, String description, Map<String, String> documentUrls) {
        ArrayList<String> uploadedImageUrls = new ArrayList<>();
        final int totalImages = imageUris.size();
        final AtomicInteger uploadedImages = new AtomicInteger(0);
        final AtomicInteger failedImages = new AtomicInteger(0);

        if (totalImages == 0) {
            Toast.makeText(requireContext(), "No images selected", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Uri uri : imageUris) {
            String fileName = UUID.randomUUID().toString() + "_image_" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageRef.child("cars/" + carId + "/" + fileName);
            imageRef.putFile(uri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return imageRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            uploadedImageUrls.add(task.getResult().toString());
                            Log.d("AddCarFragment", "Uploaded image: " + task.getResult());
                        } else {
                            failedImages.incrementAndGet();
                            Log.e("AddCarFragment", "Image upload failed for URI: " + uri);
                        }
                        if (uploadedImages.incrementAndGet() == totalImages) {
                            if (failedImages.get() > 0 || uploadedImageUrls.size() < 2) {
                                Toast.makeText(requireContext(), "Failed to upload " + failedImages.get() + " images. At least 2 images required.", Toast.LENGTH_LONG).show();
                            } else {
                                saveCarToDatabase(carId, model, brand, seats, price, location, uploadedImageUrls, description, documentUrls);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AddCarFragment", "Image upload failed: " + e.getMessage());
                        Toast.makeText(requireContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        failedImages.incrementAndGet();
                        if (uploadedImages.incrementAndGet() == totalImages) {
                            if (failedImages.get() > 0 || uploadedImageUrls.size() < 2) {
                                Toast.makeText(requireContext(), "Failed to upload " + failedImages.get() + " images. At least 2 images required.", Toast.LENGTH_LONG).show();
                            } else {
                                saveCarToDatabase(carId, model, brand, seats, price, location, uploadedImageUrls, description, documentUrls);
                            }
                        }
                    });
        }
    }

    private void saveCarToDatabase(String carId, String model, String brand, int seats, double price, String location, ArrayList<String> images, String description, Map<String, String> documentUrls) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> carData = new HashMap<>();
        carData.put("id", carId);
        carData.put("model", model);
        carData.put("brand", brand);
        carData.put("description", description);
        carData.put("seats", seats);
        carData.put("price", price);
        carData.put("location", location);
        carData.put("latitude", latitude);
        carData.put("longitude", longitude);
        carData.put("images", images);
        carData.put("rating", 0f);
        carData.put("ratingCount", 0);
        carData.put("ratedBy", new ArrayList<>());
        carData.put("createdAt", FieldValue.serverTimestamp());
        carData.put("state", CarAvailabilityState.PENDING.toString());
        carData.put("managerId", currentUser.getUid());
        carData.put("fcDocument", documentUrls.get("fc.pdf"));
        carData.put("rcDocument", documentUrls.get("rc.pdf"));
        carData.put("insuranceDocument", documentUrls.get("insurance.pdf"));
        carData.put("pucDocument", documentUrls.get("puc.pdf"));

        db.collection("pending_cars").document(carId).set(carData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Car submitted for approval", Toast.LENGTH_SHORT).show();
                    clearFields();
                    navigateToViewCarsFragment();
                })
                .addOnFailureListener(e -> {
                    Log.e("AddCarFragment", "Failed to submit car: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to submit car: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearFields() {
        inputCarModel.setText("");
        inputCarBrand.setText("");
        inputCarSeats.setText("");
        inputCarLocation.setText("");
        inputCarPrice.setText("");
        inputCarDescription.setText("");
        imageUris.clear();
        imageUrls.clear();
        isFcUploaded = false;
        isRcUploaded = false;
        isInsuranceUploaded = false;
        isPucUploaded = false;
        fcUri = null;
        rcUri = null;
        insuranceUri = null;
        pucUri = null;
        buttonUploadFc.setText("Upload FC");
        buttonUploadRc.setText("Upload RC");
        buttonUploadInsurance.setText("Upload Insurance");
        buttonUploadPuc.setText("Upload PUC");
        imagePreviewAdapter.updateImages(new ArrayList<>());
    }

    private void confirmImageRemoval(int position) {
        if (position < 0 || position >= imageUrls.size()) {
            Log.w("AddCarFragment", "Invalid position for removal: " + position);
            Toast.makeText(requireContext(), "Cannot remove image", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageUrl = imageUrls.get(position);
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Image")
                .setMessage("Are you sure you want to remove this image?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Remove from both lists and update adapter atomically
                    imageUrls.remove(position);
                    for (int i = 0; i < imageUris.size(); i++) {
                        if (imageUris.get(i).toString().equals(imageUrl)) {
                            imageUris.remove(i);
                            break;
                        }
                    }
                    imagePreviewAdapter.removeImage(position);
                    Log.d("AddCarFragment", "Removed image at position " + position + ", remaining: " + imageUrls.size());
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Location permission needed for autocomplete", Toast.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void openPlaceAutocomplete() {
        if (placesClient == null) {
            Toast.makeText(requireContext(), "Location services unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(requireContext());
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    private void navigateToViewCarsFragment() {
        if (getActivity() == null) {
            Toast.makeText(requireContext(), "Activity not available", Toast.LENGTH_SHORT).show();
            return;
        }
        ViewCarsFragment viewCarsFragment = new ViewCarsFragment();
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        try {
            fragmentManager.beginTransaction()
                    .replace(R.id.managerFragmentContainer, viewCarsFragment)
                    .addToBackStack(null)
                    .commit();
        } catch (IllegalArgumentException e) {
            Toast.makeText(requireContext(), "Invalid container ID", Toast.LENGTH_SHORT).show();
            Log.e("AddCarFragment", "Fragment transaction failed", e);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("fcUri", fcUri);
        outState.putParcelable("rcUri", rcUri);
        outState.putParcelable("insuranceUri", insuranceUri);
        outState.putParcelable("pucUri", pucUri);
        outState.putBoolean("isFcUploaded", isFcUploaded);
        outState.putBoolean("isRcUploaded", isRcUploaded);
        outState.putBoolean("isInsuranceUploaded", isInsuranceUploaded);
        outState.putBoolean("isPucUploaded", isPucUploaded);
        outState.putStringArrayList("imageUrls", imageUrls);
        outState.putParcelableArrayList("imageUris", imageUris);
    }
}