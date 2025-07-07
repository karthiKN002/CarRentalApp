package com.example.gearup.uiactivities.customer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.gearup.R;
import com.example.gearup.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView userNameTextView, userEmailTextView, userPhoneTextView, userTypeTextView;
    private ImageView userProfileImageView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        userNameTextView = view.findViewById(R.id.userNameTextView);
        userEmailTextView = view.findViewById(R.id.userEmailTextView);
        userPhoneTextView = view.findViewById(R.id.userPhoneTextView);
        userTypeTextView = view.findViewById(R.id.userTypeTextView);
        userProfileImageView = view.findViewById(R.id.userProfileImageView);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = getArguments() != null ? getArguments().getString("userId") : mAuth.getCurrentUser().getUid();

        Log.d("ProfileFragment", "Received userId: " + userId);

        if (userId == null) {
            Log.e("ProfileFragment", "No userId provided and no authenticated user");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Invalid user profile", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
            return view;
        }

        if (mAuth.getCurrentUser() == null) {
            Log.e("ProfileFragment", "User not authenticated");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Please sign in to view profile", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
            return view;
        }

        if (isNetworkAvailable()) {
            loadUserDetails();
        } else {
            if (isAdded()) {
                Toast.makeText(requireContext(), "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            }
        }

        return view;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void loadUserDetails() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isAdded() && doc.exists()) {
                        User user = doc.toObject(User.class);
                        String fullName = doc.getString("fullName");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");
                        String userType = doc.getString("userType");
                        String profileImageUrl = doc.getString("imgUrl");
                        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
                            profileImageUrl = doc.getString("photo");
                        }
                        userNameTextView.setText("Name: " + (fullName != null && !fullName.isEmpty() ? fullName : "Unknown User"));
                        userEmailTextView.setText("Email: " + (email != null && !email.isEmpty() ? email : "No email"));
                        if (phone != null && !phone.isEmpty()) {
                            userPhoneTextView.setText("Phone: " + phone);
                            userPhoneTextView.setVisibility(View.VISIBLE);
                        } else {
                            userPhoneTextView.setVisibility(View.GONE);
                        }
                        userTypeTextView.setText("User Type: " + (userType != null && !userType.isEmpty() ? userType : "Unknown type"));
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(userProfileImageView);
                        } else {
                            Glide.with(requireContext())
                                    .load(R.drawable.ic_user_avatar_placeholder)
                                    .circleCrop()
                                    .into(userProfileImageView);
                        }
                    } else {
                        if (isAdded()) {
                            userNameTextView.setText("Name: User not found");
                            userPhoneTextView.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Failed to load user: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        userNameTextView.setText("Name: Error loading user");
                        userPhoneTextView.setVisibility(View.GONE);
                    }
                });
    }
}