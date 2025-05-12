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
import com.google.android.gms.tasks.Tasks;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AddCarFragment extends Fragment {

    private static final int AUTOCOMPLETE_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int IMAGE_PICKER_REQUEST_CODE = 1;

    private EditText inputCarModel, inputCarBrand, inputCarSeats, inputCarLocation, inputCarPrice, inputCarDescription;
    private ArrayList<Uri> imageUris; // For Firebase Storage uploads
    private ArrayList<String> imageUrls; // For ImagePreviewAdapter
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Button buttonSelectImages, buttonSubmitCarDetails;
    private RecyclerView imageRecyclerView;
    private ImagePreviewAdapter imagePreviewAdapter;
    private PlacesClient placesClient;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve PlacesClient from the activity
        if (getActivity() instanceof ManagerDashboardActivity) {
            placesClient = ((ManagerDashboardActivity) getActivity()).getPlacesClient();
        } else {
            Log.e("AddCarFragment", "Parent activity is not ManagerDashboardActivity, cannot get PlacesClient");
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

        imageUris = new ArrayList<>();
        imageUrls = new ArrayList<>();
        imagePreviewAdapter = new ImagePreviewAdapter(requireContext(), imageUrls, position -> confirmImageRemoval(position));
        imageRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        imageRecyclerView.setAdapter(imagePreviewAdapter);

        buttonSelectImages.setOnClickListener(v -> selectImages());
        buttonSubmitCarDetails.setOnClickListener(v -> uploadCarDetails());

        inputCarLocation.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                openPlaceAutocomplete();
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // No need to set placesClient to null since it's managed by the activity
    }

    private void selectImages() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), IMAGE_PICKER_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            inputCarLocation.setText(place.getName());
            if (place.getLatLng() != null) {
                latitude = place.getLatLng().latitude;
                longitude = place.getLatLng().longitude;
            }
        } else if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    imageUris.add(uri);
                    imageUrls.add(uri.toString());
                    imagePreviewAdapter.addImage(uri.toString());
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                imageUris.add(uri);
                imageUrls.add(uri.toString());
                imagePreviewAdapter.addImage(uri.toString());
            }
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

        int seats;
        double price;
        try {
            seats = Integer.parseInt(seatsStr);
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid seats or price format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!imageUris.isEmpty()) {
            uploadImages(model, brand, seats, price, location, description);
        } else {
            saveCarToDatabase(model, brand, seats, price, location, new ArrayList<>(), description);
        }
    }

    private void uploadImages(String model, String brand, int seats, double price, String location, String description) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("AddCarFragment", "User not authenticated");
            Toast.makeText(requireContext(), "Please sign in to upload images", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("AddCarFragment", "Authenticated user: " + currentUser.getUid() + ", Email: " + currentUser.getEmail());

        ArrayList<String> uploadedImageUrls = new ArrayList<>();
        final int totalImages = imageUris.size();
        final AtomicInteger uploadedImages = new AtomicInteger(0);

        if (totalImages == 0) {
            saveCarToDatabase(model, brand, seats, price, location, uploadedImageUrls, description);
            return;
        }

        // Use the class-level storageRef
        for (Uri uri : imageUris) {
            if (uri == null) {
                Log.e("AddCarError", "Invalid image URI: null");
                uploadedImages.incrementAndGet();
                continue;
            }
            try {
                // Test if the URI is accessible
                getContext().getContentResolver().openInputStream(uri).close();
            } catch (Exception e) {
                Log.e("AddCarError", "Invalid image URI: " + uri, e);
                uploadedImages.incrementAndGet();
                continue;
            }

            String fileName = UUID.randomUUID().toString() + "_image_" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageRef.child("cars/" + fileName);
            Log.d("AddCarFragment", "Uploading to: " + imageRef.getPath());

            imageRef.putFile(uri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Log.e("AddCarError", "Upload failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                            return Tasks.forResult(null);
                        }
                        return imageRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            uploadedImageUrls.add(task.getResult().toString());
                            Log.d("AddCarFragment", "Uploaded image: " + task.getResult().toString());
                        } else {
                            Log.w("AddCarError", "Failed to get download URL for image");
                        }
                        if (uploadedImages.incrementAndGet() == totalImages) {
                            saveCarToDatabase(model, brand, seats, price, location, uploadedImageUrls, description);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AddCarError", "Upload failure: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        if (uploadedImages.incrementAndGet() == totalImages) {
                            saveCarToDatabase(model, brand, seats, price, location, uploadedImageUrls, description);
                        }
                    });
        }
    }

    private void saveCarToDatabase(String model, String brand, int seats, double price, String location, ArrayList<String> images, String description) {
        Map<String, Object> carData = new HashMap<>();
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
        carData.put("state", CarAvailabilityState.AVAILABLE.toString());

        db.collection("Cars").add(carData)
                .addOnSuccessListener(documentReference -> {
                    String carId = documentReference.getId();
                    documentReference.update("id", carId); // Match Car.java's id field
                    Toast.makeText(requireContext(), "Car added successfully", Toast.LENGTH_SHORT).show();
                    clearFields();
                    navigateToViewCarsFragment();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to add car: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        imagePreviewAdapter.updateImages(new ArrayList<>());
    }

    private void confirmImageRemoval(int position) {
        String imageUrl = imagePreviewAdapter.getImageAt(position);
        if (imageUrl == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Image")
                .setMessage("Are you sure you want to remove this image?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    imageUrls.remove(position);
                    Iterator<Uri> iterator = imageUris.iterator();
                    while (iterator.hasNext()) {
                        Uri uri = iterator.next();
                        if (uri.toString().equals(imageUrl)) {
                            iterator.remove();
                        }
                    }
                    imagePreviewAdapter.removeImage(position);
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
}