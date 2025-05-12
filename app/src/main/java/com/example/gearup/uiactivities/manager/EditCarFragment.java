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
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.example.gearup.adapters.ImagePreviewAdapter;
import com.example.gearup.states.car.CarAvailabilityState;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
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

public class EditCarFragment extends Fragment {

    private static final int AUTOCOMPLETE_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int IMAGE_PICKER_REQUEST_CODE = 1;

    private EditText brandEditText, modelEditText, seatsEditText, priceEditText, locationEditText, descriptionEditText;
    private SwitchCompat availabilitySwitch;
    private Button buttonEditImage, saveButton;
    private ImageView carImage;
    private RatingBar carRatingBar;
    private TextView textViewRatingCount;
    private RecyclerView imageRecyclerView;
    private ImagePreviewAdapter imagePreviewAdapter;
    private ArrayList<Uri> newImageUris;
    private ArrayList<String> existingImageUrls;
    private ArrayList<String> imagesToDelete;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private PlacesClient placesClient;
    private String carId;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TextUtils.isEmpty(BuildConfig.GOOGLE_MAPS_API_KEY)) {
            Log.e("EditCarFragment", "Google Maps API key is missing");
            Toast.makeText(getContext(), "Error: Google Maps API key is missing.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.GOOGLE_MAPS_API_KEY);
        }
        placesClient = Places.createClient(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        checkLocationPermission();
        View view = inflater.inflate(R.layout.fragment_edit_car, container, false);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Initialize UI components
        brandEditText = view.findViewById(R.id.brandEditText);
        modelEditText = view.findViewById(R.id.modelEditText);
        seatsEditText = view.findViewById(R.id.seatsEditText);
        priceEditText = view.findViewById(R.id.priceEditText);
        locationEditText = view.findViewById(R.id.locationEditText);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        availabilitySwitch = view.findViewById(R.id.availabilitySwitch);
        buttonEditImage = view.findViewById(R.id.buttonEditImage);
        saveButton = view.findViewById(R.id.saveButton);
        carImage = view.findViewById(R.id.carImage);
        carRatingBar = view.findViewById(R.id.carRatingBar);
        textViewRatingCount = view.findViewById(R.id.textViewRatingCount);
        imageRecyclerView = view.findViewById(R.id.imageRecyclerView);

        // Setup image handling
        newImageUris = new ArrayList<>();
        existingImageUrls = new ArrayList<>();
        imagesToDelete = new ArrayList<>();
        imagePreviewAdapter = new ImagePreviewAdapter(requireContext(), new ArrayList<>(), position -> confirmImageRemoval(position));
        imageRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        imageRecyclerView.setAdapter(imagePreviewAdapter);

        // Load car details from bundle
        Bundle bundle = getArguments();
        if (bundle != null) {
            carId = bundle.getString("carId", "");
            brandEditText.setText(bundle.getString("carBrand", ""));
            modelEditText.setText(bundle.getString("carModel", ""));
            seatsEditText.setText(String.valueOf(bundle.getInt("carSeats", 0)));
            locationEditText.setText(bundle.getString("carLocation", ""));
            priceEditText.setText(String.format("%.2f", bundle.getDouble("carPrice", 0.0)));
            descriptionEditText.setText(bundle.getString("carDescription", ""));
            existingImageUrls = bundle.getStringArrayList("carImageUrls");
            String state = bundle.getString("state", CarAvailabilityState.AVAILABLE.toString());
            float rating = bundle.getFloat("rating", 0f);
            int ratingCount = bundle.getInt("ratingCount", 0);

            // Set rating and rating count (read-only)
            carRatingBar.setRating(rating);
            textViewRatingCount.setText(ratingCount + " ratings");

            // Set availability switch
            availabilitySwitch.setChecked(state.equals(CarAvailabilityState.AVAILABLE.toString()));

            // Load primary image and previews
            if (existingImageUrls != null && !existingImageUrls.isEmpty()) {
                Glide.with(requireContext())
                        .load(existingImageUrls.get(0))
                        .placeholder(R.drawable.car_placeholder)
                        .into(carImage);
                imagePreviewAdapter.updateImages(new ArrayList<>(existingImageUrls));
            } else {
                carImage.setImageResource(R.drawable.car_placeholder);
            }
        } else {
            Toast.makeText(requireContext(), "Failed to load car details", Toast.LENGTH_SHORT).show();
            navigateToViewCarsFragment();
        }

        // Setup listeners
        buttonEditImage.setOnClickListener(v -> selectImages());
        saveButton.setOnClickListener(v -> updateCarDetails());
        locationEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                openPlaceAutocomplete();
            }
        });

        return view;
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
            locationEditText.setText(place.getName());
            if (place.getLatLng() != null) {
                latitude = place.getLatLng().latitude;
                longitude = place.getLatLng().longitude;
            }
        } else if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    newImageUris.add(uri);
                    imagePreviewAdapter.addImage(uri.toString());
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                newImageUris.add(uri);
                imagePreviewAdapter.addImage(uri.toString());
            }
            // Update primary image if new images are added
            if (!newImageUris.isEmpty()) {
                Glide.with(requireContext())
                        .load(newImageUris.get(0))
                        .placeholder(R.drawable.car_placeholder)
                        .into(carImage);
            }
            imagePreviewAdapter.notifyDataSetChanged();
        }
    }

    private void updateCarDetails() {
        String brand = brandEditText.getText().toString().trim();
        String model = modelEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String seatsStr = seatsEditText.getText().toString().trim();
        String priceStr = priceEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String state = availabilitySwitch.isChecked() ? CarAvailabilityState.AVAILABLE.toString() : CarAvailabilityState.UNAVAILABLE.toString();

        // Validate inputs
        if (TextUtils.isEmpty(brand) || TextUtils.isEmpty(model) || TextUtils.isEmpty(seatsStr) ||
                TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(location)) {
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

        // Proceed with image deletion and updates
        deleteRemovedImages(() -> {
            if (!newImageUris.isEmpty()) {
                uploadNewImages(brand, model, seats, price, location, description, state);
            } else {
                saveCarToDatabase(brand, model, seats, price, location, existingImageUrls, description, state);
            }
        });
    }

    private void deleteRemovedImages(Runnable onComplete) {
        if (imagesToDelete.isEmpty()) {
            onComplete.run();
            return;
        }

        int[] deletedCount = {0};
        int totalToDelete = imagesToDelete.size();

        for (String imageUrl : imagesToDelete) {
            StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);
            imageRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        deletedCount[0]++;
                        if (deletedCount[0] == totalToDelete) {
                            existingImageUrls.removeAll(imagesToDelete);
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to delete image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        deletedCount[0]++;
                        if (deletedCount[0] == totalToDelete) {
                            existingImageUrls.removeAll(imagesToDelete);
                            onComplete.run();
                        }
                    });
        }
    }

    private void uploadNewImages(String brand, String model, int seats, double price, String location, String description, String state) {
        ArrayList<String> newImageUrls = new ArrayList<>(existingImageUrls);
        final int totalImages = newImageUris.size();
        final int[] uploadedImages = {0};

        for (Uri uri : newImageUris) {
            String fileName = UUID.randomUUID().toString();
            StorageReference imageRef = storageRef.child("cars/" + fileName);
            imageRef.putFile(uri)
                    .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                newImageUrls.add(downloadUri.toString());
                                uploadedImages[0]++;
                                if (uploadedImages[0] == totalImages) {
                                    saveCarToDatabase(brand, model, seats, price, location, newImageUrls, description, state);
                                }
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        uploadedImages[0]++;
                        if (uploadedImages[0] == totalImages) {
                            saveCarToDatabase(brand, model, seats, price, location, newImageUrls, description, state);
                        }
                    });
        }
    }

    private void saveCarToDatabase(String brand, String model, int seats, double price, String location,
                                   ArrayList<String> images, String description, String state) {
        Map<String, Object> carData = new HashMap<>();
        carData.put("brand", brand);
        carData.put("model", model);
        carData.put("description", description);
        carData.put("seats", seats);
        carData.put("price", price);
        carData.put("location", location);
        carData.put("latitude", latitude);
        carData.put("longitude", longitude);
        carData.put("images", images);
        carData.put("state", state);
        carData.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("Cars").document(carId)
                .update(carData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Car updated successfully", Toast.LENGTH_SHORT).show();
                    navigateToViewCarsFragment();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to update car: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmImageRemoval(int position) {
        String imageUrl = imagePreviewAdapter.getImageAt(position);
        if (imageUrl == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Image")
                .setMessage("Are you sure you want to remove this image?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                        Iterator<Uri> iterator = newImageUris.iterator();
                        while (iterator.hasNext()) {
                            Uri uri = iterator.next();
                            if (uri.toString().equals(imageUrl)) {
                                iterator.remove();
                            }
                        }
                    } else {
                        imagesToDelete.add(imageUrl);
                        existingImageUrls.remove(imageUrl);
                    }
                    imagePreviewAdapter.removeImage(position);
                    // Update primary image if removed
                    if (!existingImageUrls.isEmpty()) {
                        Glide.with(requireContext())
                                .load(existingImageUrls.get(0))
                                .placeholder(R.drawable.car_placeholder)
                                .into(carImage);
                    } else if (!newImageUris.isEmpty()) {
                        Glide.with(requireContext())
                                .load(newImageUris.get(0))
                                .placeholder(R.drawable.car_placeholder)
                                .into(carImage);
                    } else {
                        carImage.setImageResource(R.drawable.car_placeholder);
                    }
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
            Log.e("EditCarFragment", "Fragment transaction failed", e);
        }
    }
}