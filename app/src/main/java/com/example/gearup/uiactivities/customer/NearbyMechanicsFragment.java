package com.example.gearup.uiactivities.customer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NearbyMechanicsFragment extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private RecyclerView mechanicsRecyclerView;
    private MechanicsAdapter mechanicsAdapter;
    private List<Place> mechanicsList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.GOOGLE_MAPS_API_KEY);
        }
        placesClient = Places.createClient(requireContext());
        mechanicsList = new ArrayList<>();
        mechanicsAdapter = new MechanicsAdapter(mechanicsList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby_mechanics, container, false);
        mechanicsRecyclerView = view.findViewById(R.id.mechanicsRecyclerView);
        mechanicsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mechanicsRecyclerView.setAdapter(mechanicsAdapter);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchNearbyMechanics();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        return view;
    }

    private void fetchNearbyMechanics() {
        // Double-check permission to satisfy Lint
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(location -> {
            if (location != null) {
                FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(
                        Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.TYPES)
                );
                placesClient.findCurrentPlace(request)
                        .addOnSuccessListener(response -> {
                            for (com.google.android.libraries.places.api.model.PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                                Place place = placeLikelihood.getPlace();
                                List<Place.Type> types = place.getTypes();
                                if (types != null && types.contains(Place.Type.CAR_REPAIR)) {
                                    mechanicsList.add(place);
                                }
                            }
                            mechanicsAdapter.notifyDataSetChanged();
                            if (mechanicsList.isEmpty()) {
                                Toast.makeText(getContext(), "No mechanics found nearby", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to fetch places: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(getContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchNearbyMechanics();
        } else {
            Toast.makeText(getContext(), "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }
}