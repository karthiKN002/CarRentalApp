package com.example.gearup.uiactivities.manager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.adapters.ChatListAdapter;
import com.example.gearup.models.ChatUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<ChatUser> chatUsers;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        chatUsers = new ArrayList<>();
        adapter = new ChatListAdapter(requireContext(), chatUsers);
        recyclerView = view.findViewById(R.id.chatListRecyclerView);

        if (recyclerView == null) {
            Log.e("ChatListFragment", "RecyclerView not found in layout");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Chat list UI initialization failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(adapter);
            setupClickListener();
        }

        if (isNetworkAvailable()) {
            loadChatUsers();
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

    private void loadChatUsers() {
        String userId = mAuth.getCurrentUser().getUid();
        Log.d("ChatListFragment", "Loading chats for userId: " + userId);

        db.collection("Messages")
                .whereIn("senderId", Arrays.asList(userId))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((senderSnapshots, senderError) -> {
                    if (senderError != null) {
                        Log.e("ChatListFragment", "Error loading sender messages: " + senderError.getMessage());
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to load chats: " + senderError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    db.collection("Messages")
                            .whereIn("receiverId", Arrays.asList(userId))
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener((receiverSnapshots, receiverError) -> {
                                if (receiverError != null) {
                                    Log.e("ChatListFragment", "Error loading receiver messages: " + receiverError.getMessage());
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Failed to load chats: " + receiverError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                    return;
                                }

                                Map<String, String> userCarMap = new HashMap<>();
                                Map<String, Long> userTimestampMap = new HashMap<>();
                                if (senderSnapshots != null) {
                                    for (DocumentSnapshot doc : senderSnapshots) {
                                        String receiverId = doc.getString("receiverId");
                                        String carId = doc.getString("carId");
                                        com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                                        if (receiverId != null && !receiverId.equals(userId)) {
                                            userCarMap.put(receiverId, carId != null ? carId : "");
                                            if (timestamp != null) {
                                                userTimestampMap.put(receiverId, timestamp.toDate().getTime());
                                            }
                                        }
                                    }
                                }
                                if (receiverSnapshots != null) {
                                    for (DocumentSnapshot doc : receiverSnapshots) {
                                        String senderId = doc.getString("senderId");
                                        String carId = doc.getString("carId");
                                        com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                                        if (senderId != null && !senderId.equals(userId)) {
                                            userCarMap.put(senderId, carId != null ? carId : "");
                                            if (timestamp != null) {
                                                userTimestampMap.put(senderId, timestamp.toDate().getTime());
                                            }
                                        }
                                    }
                                }
                                Log.d("ChatListFragment", "Found " + userCarMap.size() + " unique sender/receiver IDs");
                                fetchUserDetails(new ArrayList<>(userCarMap.keySet()), userCarMap, userTimestampMap);
                            });
                });
    }

    private void fetchUserDetails(List<String> userIds, Map<String, String> userCarMap, Map<String, Long> userTimestampMap) {
        chatUsers.clear();
        if (userIds.isEmpty()) {
            adapter.notifyDataSetChanged();
            Log.d("ChatListFragment", "No users to display");
            return;
        }
        db.collection("users")
                .whereIn("uid", userIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        String uid = doc.getString("uid");
                        String fullName = doc.getString("fullName");
                        String email = doc.getString("email");
                        String role = doc.getString("userType");
                        Log.d("ChatListFragment", "User - uid: " + uid + ", fullName: " + fullName + ", email: " + email + ", userType: " + role);
                        ChatUser user = new ChatUser(uid, fullName != null && !fullName.isEmpty() ? fullName : "Unknown User");
                        user.setEmail(email != null ? email : "No email");
                        user.setTimestamp(userTimestampMap.getOrDefault(uid, 0L));
                        String carId = userCarMap.get(uid);
                        if (carId != null && !carId.isEmpty()) {
                            db.collection("cars").document(carId)
                                    .get()
                                    .addOnSuccessListener(carDoc -> {
                                        String carName = carDoc.getString("name");
                                        user.setCarName(carName != null ? carName : "No car selected");
                                        if (!chatUsers.contains(user)) {
                                            chatUsers.add(user);
                                        }
                                        adapter.notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e -> {
                                        user.setCarName("No car selected");
                                        if (!chatUsers.contains(user)) {
                                            chatUsers.add(user);
                                        }
                                        adapter.notifyDataSetChanged();
                                        Log.e("ChatListFragment", "Error fetching car: " + e.getMessage());
                                    });
                        } else {
                            user.setCarName("No car selected");
                            if (!chatUsers.contains(user)) {
                                chatUsers.add(user);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatListFragment", "Error fetching users: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickListener() {
        adapter.setCustomClickListener(new ChatListAdapter.OnChatItemClickListener() {
            @Override
            public void onChatItemClicked(String userId) {
                Bundle args = new Bundle();
                args.putString("receiverId", userId);
                ChatFragment chatFragment = new ChatFragment();
                chatFragment.setArguments(args);
                FragmentActivity activity = getActivity();
                int containerId = activity instanceof com.example.gearup.uiactivities.mechanic.MechanicDashboardActivity
                        ? R.id.mechanicFragmentContainer
                        : R.id.managerFragmentContainer;
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(containerId, chatFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }
}