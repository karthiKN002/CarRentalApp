package com.example.gearup.uiactivities.customer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.adapters.ChatAdapter;
import com.example.gearup.models.Message;
import com.example.gearup.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView userNameTextView, userEmailTextView, userPhoneTextView, userTypeTextView;
    private RecyclerView messagesRecyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
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
        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView);
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(requireContext(), messageList);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        messagesRecyclerView.setAdapter(chatAdapter);
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
            loadMessages();
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
                        if (user != null) {
                            userNameTextView.setText(user.getFullName() != null ? user.getFullName() : "Unknown User");
                            userEmailTextView.setText(user.getEmail() != null ? user.getEmail() : "No email");
                            userPhoneTextView.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "No phone");
                            userTypeTextView.setText(user.getUserType() != null ? user.getUserType() : "Unknown type");
                        }
                    } else {
                        if (isAdded()) {
                            userNameTextView.setText("User not found");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Failed to load user: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        userNameTextView.setText("Error loading user");
                    }
                });
    }

    private void loadMessages() {
        String managerId = mAuth.getCurrentUser().getUid();
        db.collection("Messages")
                .whereIn("senderId", Arrays.asList(userId, managerId))
                .whereIn("receiverId", Arrays.asList(userId, managerId))
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("ProfileFragment", "Error loading messages: " + e.getMessage());
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to load messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    messageList.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots) {
                            try {
                                Message message = doc.toObject(Message.class);
                                if (message != null) {
                                    messageList.add(message);
                                }
                            } catch (Exception ex) {
                                Log.e("ProfileFragment", "Error deserializing message: " + ex.getMessage());
                            }
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                });
    }
}