package com.example.gearup.uiactivities.customer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.Arrays;
import java.util.List;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private PlacesClient placesClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        // Initialize Places API
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(requireContext(), BuildConfig.GOOGLE_MAPS_API_KEY);
        }
        placesClient = com.google.android.libraries.places.api.Places.createClient(requireContext());

        // Get the SupportMapFragment and request notification when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Check location permission
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        // Enable my location button and blue dot if permission is granted
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Fetch current location and nearby mechanics
        fetchNearbyMechanics();
    }

    private void fetchNearbyMechanics() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES));
        placesClient.findCurrentPlace(request)
                .addOnSuccessListener(response -> {
                    LatLng currentLocation = null;
                    for (com.google.android.libraries.places.api.model.PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                        Place place = placeLikelihood.getPlace();
                        if (place.getLatLng() != null) {
                            currentLocation = place.getLatLng();
                            break;
                        }
                    }
                    if (currentLocation != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12f));
                        addMechanicsMarkers(response.getPlaceLikelihoods());
                    } else {
                        Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("MapsFragment", "Failed to fetch place: " + e.getMessage()));
    }

    private void addMechanicsMarkers(List<com.google.android.libraries.places.api.model.PlaceLikelihood> placeLikelihoods) {
        for (com.google.android.libraries.places.api.model.PlaceLikelihood placeLikelihood : placeLikelihoods) {
            Place place = placeLikelihood.getPlace();
            if (place.getTypes() != null && place.getTypes().contains(Place.Type.CAR_REPAIR) && place.getLatLng() != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(place.getLatLng())
                        .title(place.getName()));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                fetchNearbyMechanics();
            }
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}