package com.example.gearup.uiactivities.customer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.adapters.MechanicsAdapter;
import com.example.gearup.models.Mechanic;
import com.example.gearup.uiactivities.manager.ManagerDashboardActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class NearbyMechanicsFragment extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private RecyclerView recyclerView;
    private MechanicsAdapter mechanicsAdapter;
    private List<Mechanic> mechanicList;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private Location currentLocation;
    private TextView noMechanicsTextView;

    public NearbyMechanicsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mechanicList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mechanics_list, container, false);
        recyclerView = view.findViewById(R.id.mechanicsRecyclerView);
        noMechanicsTextView = view.findViewById(R.id.noMechanicsTextView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mechanicsAdapter = new MechanicsAdapter(requireContext(), mechanicList, mechanic -> {
            MechanicDetailsFragment detailsFragment = new MechanicDetailsFragment();
            Bundle args = new Bundle();
            args.putString("mechanicId", mechanic.getId());
            detailsFragment.setArguments(args);
            FragmentActivity activity = getActivity();
            int containerId = activity instanceof ManagerDashboardActivity
                    ? R.id.managerFragmentContainer
                    : R.id.customerFragmentContainer;
            getParentFragmentManager().beginTransaction()
                    .replace(containerId, detailsFragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerView.setAdapter(mechanicsAdapter);
        checkLocationPermission();
        return view;
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                    noMechanicsTextView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;
                        Log.d("NearbyMechanicsFragment", "Current location: " + location.getLatitude() + ", " + location.getLongitude());
                        loadMechanics();
                    } else {
                        Log.w("NearbyMechanicsFragment", "Location is null");
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
                            noMechanicsTextView.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("NearbyMechanicsFragment", "Failed to get location: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        noMechanicsTextView.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void loadMechanics() {
        db.collection("users")
                .whereEqualTo("userType", "mechanic")
                .whereEqualTo("isApproved", true)
                .whereEqualTo("isActive", true)
                .orderBy("fullName", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("NearbyMechanicsFragment", "Failed to load mechanics: " + e.getMessage());
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to load mechanics: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            noMechanicsTextView.setVisibility(View.VISIBLE);
                        }
                        return;
                    }
                    mechanicList.clear();
                    Log.d("NearbyMechanicsFragment", "Snapshots size: " + (snapshots != null ? snapshots.size() : 0));
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots) {
                            Mechanic mechanic = doc.toObject(Mechanic.class);
                            if (mechanic != null) {
                                mechanic.setId(doc.getId());
                                String fullName = doc.getString("fullName");
                                if (fullName == null || fullName.isEmpty()) {
                                    Log.e("NearbyMechanicsFragment", "Firestore fullName is null or empty for ID: " + doc.getId());
                                    continue; // Skip invalid mechanic
                                }
                                mechanic.setFullName(fullName);
                                if (mechanic.getLocation() != null && currentLocation != null) {
                                    float[] results = new float[1];
                                    Location.distanceBetween(
                                            currentLocation.getLatitude(), currentLocation.getLongitude(),
                                            mechanic.getLocation().getLatitude(), mechanic.getLocation().getLongitude(),
                                            results);
                                    mechanic.setDistance(results[0] / 1000); // Convert to kilometers
                                } else {
                                    mechanic.setDistance(0.0f);
                                    Log.w("NearbyMechanicsFragment", "Location missing for mechanic ID: " + doc.getId());
                                }
                                mechanicList.add(mechanic);
                                Log.d("NearbyMechanicsFragment", "Mechanic added: ID=" + mechanic.getId() + ", Name=" + mechanic.getFullName() + ", Distance=" + mechanic.getDistance());
                            } else {
                                Log.w("NearbyMechanicsFragment", "Failed to parse mechanic for doc: " + doc.getId());
                            }
                        }
                        mechanicList.sort((m1, m2) -> Float.compare(m1.getDistance(), m2.getDistance()));
                    }
                    mechanicsAdapter.notifyDataSetChanged();
                    noMechanicsTextView.setVisibility(mechanicList.isEmpty() ? View.VISIBLE : View.GONE);
                    Log.d("NearbyMechanicsFragment", "Mechanic list size: " + mechanicList.size());
                });
    }
}