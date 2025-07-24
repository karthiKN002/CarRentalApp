package com.example.gearup.uiactivities.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.gearup.R;
import com.example.gearup.uiactivities.manager.ChatFragment;
import com.example.gearup.uiactivities.manager.ManagerDashboardActivity;
import com.example.gearup.uiactivities.mechanic.MechanicDashboardActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MechanicDetailsFragment extends Fragment implements OnMapReadyCallback {

    private TextView mechanicNameTextView, mechanicEmailTextView, mechanicPhoneTextView, mechanicAddressTextView;
    private ImageView mechanicProfileImageView;
    private Button chatButton, viewOnMapButton;
    private MapView mapView;
    private FirebaseFirestore db;
    private String mechanicId;
    private String mechanicName;
    private com.google.firebase.firestore.GeoPoint mechanicLocation;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mechanic_details, container, false);

        mechanicNameTextView = view.findViewById(R.id.mechanicNameTextView);
        mechanicEmailTextView = view.findViewById(R.id.mechanicEmailTextView);
        mechanicPhoneTextView = view.findViewById(R.id.mechanicPhoneTextView);
        mechanicAddressTextView = view.findViewById(R.id.mechanicAddressTextView);
        mechanicProfileImageView = view.findViewById(R.id.mechanicProfileImageView);
        chatButton = view.findViewById(R.id.chatButton);
        viewOnMapButton = view.findViewById(R.id.viewOnMapButton);
        mapView = view.findViewById(R.id.mapView);
        db = FirebaseFirestore.getInstance();

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        if (getArguments() != null) {
            mechanicId = getArguments().getString("mechanicId");
            if (mechanicId != null) {
                Log.d("MechanicDetailsFragment", "Loading mechanic ID: " + mechanicId);
                loadMechanicDetails();
            } else {
                Log.e("MechanicDetailsFragment", "No mechanic ID provided");
                Toast.makeText(requireContext(), "Mechanic data not available", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        } else {
            Log.e("MechanicDetailsFragment", "No arguments provided");
            Toast.makeText(requireContext(), "Mechanic data not available", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        }

        return view;
    }

    private void loadMechanicDetails() {
        db.collection("users").document(mechanicId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isAdded() && doc.exists()) {
                        String fullName = doc.getString("fullName");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");
                        String locationString = doc.getString("locationString");
                        com.google.firebase.firestore.GeoPoint geoPoint = doc.getGeoPoint("location");
                        String profileImageUrl = doc.getString("imgUrl");
                        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
                            profileImageUrl = doc.getString("photo");
                        }

                        mechanicName = fullName != null && !fullName.isEmpty() ? fullName : "Mechanic";
                        mechanicLocation = geoPoint;

                        mechanicNameTextView.setText("Name: " + mechanicName);
                        mechanicEmailTextView.setText("Email: " + (email != null ? email : "No email"));
                        mechanicPhoneTextView.setText("Phone: " + (phone != null ? phone : "No phone"));
                        mechanicAddressTextView.setText("Address: " + (locationString != null ? locationString : "No address"));

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(mechanicProfileImageView);
                        } else {
                            Glide.with(requireContext())
                                    .load(R.drawable.ic_user_avatar_placeholder)
                                    .circleCrop()
                                    .into(mechanicProfileImageView);
                        }

                        setupButtonListeners();
                    } else {
                        Log.e("MechanicDetailsFragment", "Mechanic document not found for ID: " + mechanicId);
                        Toast.makeText(requireContext(), "Mechanic data not found", Toast.LENGTH_SHORT).show();
                        Glide.with(requireContext())
                                .load(R.drawable.ic_user_avatar_placeholder)
                                .circleCrop()
                                .into(mechanicProfileImageView);
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MechanicDetailsFragment", "Failed to load mechanic: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to load mechanic: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Glide.with(requireContext())
                            .load(R.drawable.ic_user_avatar_placeholder)
                            .circleCrop()
                            .into(mechanicProfileImageView);
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
    }

    private void setupButtonListeners() {
        chatButton.setOnClickListener(v -> {
            if (mechanicId != null) {
                Log.d("MechanicDetailsFragment", "Opening chat for mechanic ID: " + mechanicId);
                Bundle args = new Bundle();
                args.putString("receiverId", mechanicId);
                ChatFragment chatFragment = new ChatFragment();
                chatFragment.setArguments(args);
                try {
                    getParentFragmentManager().beginTransaction()
                            .replace(getContainerId(), chatFragment)
                            .addToBackStack(null)
                            .commit();
                } catch (Exception e) {
                    Log.e("MechanicDetailsFragment", "Failed to open ChatFragment: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error opening chat", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("MechanicDetailsFragment", "Mechanic ID not available for chat");
                Toast.makeText(requireContext(), "Mechanic data not available", Toast.LENGTH_SHORT).show();
            }
        });

        viewOnMapButton.setOnClickListener(v -> {
            if (mechanicLocation != null) {
                Log.d("MechanicDetailsFragment", "Opening Google Maps for navigation to: " + mechanicLocation.getLatitude() + "," + mechanicLocation.getLongitude());
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + mechanicLocation.getLatitude() + "," + mechanicLocation.getLongitude() + "&mode=d");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Log.e("MechanicDetailsFragment", "Google Maps not installed");
                    Toast.makeText(requireContext(), "Google Maps not installed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("MechanicDetailsFragment", "Location not available for mechanic ID: " + mechanicId);
                Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getContainerId() {
        if (requireActivity() instanceof CustomerDashboardActivity) {
            return R.id.customerFragmentContainer;
        } else if (requireActivity() instanceof ManagerDashboardActivity) {
            return R.id.managerFragmentContainer;
        } else if (requireActivity() instanceof MechanicDashboardActivity) {
            return R.id.mechanicFragmentContainer;
        }
        return R.id.managerFragmentContainer; // Fallback for manager role
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mechanicLocation != null && mechanicName != null) {
            LatLng location = new LatLng(mechanicLocation.getLatitude(), mechanicLocation.getLongitude());
            googleMap.addMarker(new MarkerOptions().position(location).title(mechanicName));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
        } else {
            Log.e("MechanicDetailsFragment", "Cannot load map: invalid location or name for mechanic ID: " + mechanicId);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}