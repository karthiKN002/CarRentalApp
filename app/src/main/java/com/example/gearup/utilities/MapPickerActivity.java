package com.example.gearup.utilities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.model.Place;

import java.util.Arrays;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapPickerActivity";
    private GoogleMap mMap;
    private Marker selectedMarker;
    private PlacesClient placesClient;
    private Button buttonConfirmLocation;
    private LatLng selectedLatLng;
    private String selectedAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.GOOGLE_MAPS_API_KEY);
        }
        placesClient = Places.createClient(this);

        buttonConfirmLocation = findViewById(R.id.buttonConfirmLocation);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buttonConfirmLocation.setOnClickListener(v -> confirmLocation());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Set initial position from intent or default
        double latitude = getIntent().getDoubleExtra("latitude", 0.0);
        double longitude = getIntent().getDoubleExtra("longitude", 0.0);
        LatLng initialPosition = (latitude != 0.0 && longitude != 0.0) ?
                new LatLng(latitude, longitude) :
                new LatLng(12.9716, 77.5946); // Default: Bangalore

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 15));

        // Set click listener for pinning
        mMap.setOnMapClickListener(latLng -> {
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            selectedLatLng = latLng;
            selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            fetchAddressForLatLng(latLng);
        });

        // Try to get current place
        fetchCurrentPlace();
    }

    private void fetchCurrentPlace() {
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        placesClient.findCurrentPlace(request).addOnSuccessListener(response -> {
            if (!response.getPlaceLikelihoods().isEmpty()) {
                Place place = response.getPlaceLikelihoods().get(0).getPlace();
                if (place.getLatLng() != null) {
                    LatLng latLng = place.getLatLng();
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    if (selectedMarker == null) {
                        selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
                        selectedLatLng = latLng;
                        selectedAddress = place.getName();
                    }
                }
            }
        });
    }

    private void fetchAddressForLatLng(LatLng latLng) {
        // Simplified: Use lat/lng as address if Places API doesn't provide a name
        selectedAddress = "Lat: " + latLng.latitude + ", Lng: " + latLng.longitude;
        // Optionally, use Geocoder for reverse geocoding (requires additional permissions)
    }

    private void confirmLocation() {
        if (selectedLatLng == null) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra("latitude", selectedLatLng.latitude);
        result.putExtra("longitude", selectedLatLng.longitude);
        result.putExtra("address", selectedAddress);
        setResult(RESULT_OK, result);
        finish();
    }
}