package com.example.gearup.uiactivities.customer;

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
import com.example.gearup.uiactivities.manager.ChatFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerChatListFragment extends Fragment {

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
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setCustomClickListener(new ChatListAdapter.OnChatItemClickListener() {
            @Override
            public void onChatItemClicked(String userId) {
                Bundle args = new Bundle();
                args.putString("receiverId", userId);
                ChatFragment chatFragment = new ChatFragment();
                chatFragment.setArguments(args);
                FragmentActivity activity = getActivity();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.customerFragmentContainer, chatFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        if (mAuth.getCurrentUser() == null) {
            Log.e("CustomerChatListFragment", "User not authenticated");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Please sign in to view chats", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
            return view;
        }

        if (isNetworkAvailable()) {
            Log.d("CustomerChatListFragment", "User authenticated, UID: " + mAuth.getCurrentUser().getUid());
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
        String customerId = mAuth.getCurrentUser().getUid();
        Log.d("CustomerChatListFragment", "Loading chats for customerId: " + customerId);

        // Test authentication state
        db.collection("users").document(customerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("CustomerChatListFragment", "Test read success for user: " + customerId);
                })
                .addOnFailureListener(e -> {
                    Log.e("CustomerChatListFragment", "Test read failed: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Auth test failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        db.collection("Messages")
                .whereIn("senderId", Arrays.asList(customerId))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((senderSnapshots, senderError) -> {
                    if (senderError != null) {
                        Log.e("CustomerChatListFragment", "Error loading sender messages: " + senderError.getMessage());
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to load chats: " + senderError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    Log.d("CustomerChatListFragment", "Sender snapshots size: " + (senderSnapshots != null ? senderSnapshots.size() : 0));

                    db.collection("Messages")
                            .whereIn("receiverId", Arrays.asList(customerId))
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener((receiverSnapshots, receiverError) -> {
                                if (receiverError != null) {
                                    Log.e("CustomerChatListFragment", "Error loading receiver messages: " + receiverError.getMessage());
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Failed to load chats: " + receiverError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                    return;
                                }
                                Log.d("CustomerChatListFragment", "Receiver snapshots size: " + (receiverSnapshots != null ? receiverSnapshots.size() : 0));

                                Map<String, String> userCarMap = new HashMap<>();
                                Map<String, Long> userTimestampMap = new HashMap<>();
                                if (senderSnapshots != null) {
                                    for (DocumentSnapshot doc : senderSnapshots) {
                                        String receiverId = doc.getString("receiverId");
                                        String carId = doc.getString("carId");
                                        com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                                        Log.d("CustomerChatListFragment", "Sender message - receiverId: " + receiverId + ", carId: " + carId + ", timestamp: " + timestamp);
                                        if (receiverId != null && !receiverId.equals(customerId)) {
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
                                        Log.d("CustomerChatListFragment", "Receiver message - senderId: " + senderId + ", carId: " + carId + ", timestamp: " + timestamp);
                                        if (senderId != null && !senderId.equals(customerId)) {
                                            userCarMap.put(senderId, carId != null ? carId : "");
                                            if (timestamp != null) {
                                                userTimestampMap.put(senderId, timestamp.toDate().getTime());
                                            }
                                        }
                                    }
                                }
                                Log.d("CustomerChatListFragment", "Found " + userCarMap.size() + " unique sender/receiver IDs");
                                fetchUserDetails(new ArrayList<>(userCarMap.keySet()), userCarMap, userTimestampMap);
                            });
                });
    }

    private void fetchUserDetails(List<String> userIds, Map<String, String> userCarMap, Map<String, Long> userTimestampMap) {
        chatUsers.clear();
        Log.d("CustomerChatListFragment", "Fetching user details for userIds: " + userIds);
        if (userIds.isEmpty()) {
            adapter.notifyDataSetChanged();
            Log.d("CustomerChatListFragment", "No users to display");
            if (isAdded()) {
                Toast.makeText(requireContext(), "No chats found", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        db.collection("users")
                .whereIn("uid", userIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("CustomerChatListFragment", "User query returned " + querySnapshot.size() + " documents");
                    for (DocumentSnapshot doc : querySnapshot) {
                        String uid = doc.getString("uid");
                        String fullName = doc.getString("fullName");
                        String email = doc.getString("email");
                        String userType = doc.getString("userType");
                        Log.d("CustomerChatListFragment", "User - uid: " + uid + ", fullName: " + fullName + ", email: " + email + ", userType: " + userType);
                        ChatUser user = new ChatUser(uid, fullName != null && !fullName.isEmpty() ? fullName : "Unknown User");
                        user.setEmail(email != null ? email : "No email");
                        user.setTimestamp(userTimestampMap.getOrDefault(uid, 0L));
                        String carId = userCarMap.get(uid);
                        Log.d("CustomerChatListFragment", "Processing user - uid: " + uid + ", carId: " + carId);
                        if (carId != null && !carId.isEmpty()) {
                            db.collection("cars").document(carId)
                                    .get()
                                    .addOnSuccessListener(carDoc -> {
                                        String carName = carDoc.getString("name");
                                        user.setCarName(carName != null ? carName : "No car selected");
                                        if (!chatUsers.contains(user)) {
                                            chatUsers.add(user);
                                            Log.d("CustomerChatListFragment", "Added ChatUser: " + user.getName() + ", carName: " + user.getCarName());
                                        }
                                        sortAndUpdateChatList();
                                    })
                                    .addOnFailureListener(e -> {
                                        user.setCarName("No car selected");
                                        if (!chatUsers.contains(user)) {
                                            chatUsers.add(user);
                                            Log.d("CustomerChatListFragment", "Added ChatUser (no car): " + user.getName());
                                        }
                                        sortAndUpdateChatList();
                                        Log.e("CustomerChatListFragment", "Error fetching car: " + e.getMessage());
                                    });
                        } else {
                            user.setCarName("No car selected");
                            if (!chatUsers.contains(user)) {
                                chatUsers.add(user);
                                Log.d("CustomerChatListFragment", "Added ChatUser (no car): " + user.getName());
                            }
                            sortAndUpdateChatList();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CustomerChatListFragment", "Error fetching users: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sortAndUpdateChatList() {
        chatUsers.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        adapter.notifyDataSetChanged();
        Log.d("CustomerChatListFragment", "Chat list updated, size: " + chatUsers.size());
    }
}