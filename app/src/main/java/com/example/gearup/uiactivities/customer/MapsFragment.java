package com.example.gearup.uiactivities.customer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.gearup.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Arrays;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        placesClient = ((CustomerDashboardActivity) getActivity()).getPlacesClient();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        enableLocationAndFetchMechanics();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationAndFetchMechanics();
            } else {
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableLocationAndFetchMechanics() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        fetchCurrentLocationAndMechanics();
    }

    private void fetchCurrentLocationAndMechanics() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12f));
                        fetchMechanics(currentLocation);
                        searchNearbyMechanics(currentLocation);
                    } else {
                        Toast.makeText(getContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchMechanics(LatLng currentLocation) {
        Location current = new Location("");
        current.setLatitude(currentLocation.latitude);
        current.setLongitude(currentLocation.longitude);

        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("userType", "mechanic")
                .whereEqualTo("isApproved", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        GeoPoint geoPoint = doc.getGeoPoint("location");
                        Double rating = doc.getDouble("rating");
                        if (geoPoint != null) {
                            Location mechanicLocation = new Location("");
                            mechanicLocation.setLatitude(geoPoint.getLatitude());
                            mechanicLocation.setLongitude(geoPoint.getLongitude());
                            float distance = current.distanceTo(mechanicLocation) / 1000; // km
                            if (distance <= 15) {
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()))
                                        .title(doc.getString("fullName"))
                                        .snippet("Rating: " + (rating != null ? rating : 0.0)));
                                marker.setTag(doc.getId());
                            }
                        }
                    }
                });
    }

    private void searchNearbyMechanics(LatLng currentLocation) {
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG));
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        placesClient.findCurrentPlace(request).addOnSuccessListener(response -> {
            for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                Place place = placeLikelihood.getPlace();
                if (place.getTypes() != null && place.getTypes().contains(Place.Type.CAR_REPAIR)) {
                    LatLng latLng = place.getLatLng();
                    if (latLng != null) {
                        mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(place.getName())
                                .snippet("Car Repair"));
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("MapsFragment", "Failed to find nearby mechanics: " + e.getMessage());
        });
    }
}