package com.example.gearup.uiactivities.manager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.adapters.ChatAdapter;
import com.example.gearup.models.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatFragment extends Fragment {

    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String receiverId, carId, contractId;
    private OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        messageList = new ArrayList<>();
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        chatAdapter = new ChatAdapter(requireContext(), messageList, currentUserId);
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        messageEditText = view.findViewById(R.id.messageEditText);
        sendButton = view.findViewById(R.id.sendButton);

        if (chatRecyclerView == null || messageEditText == null || sendButton == null) {
            Log.e("ChatFragment", "UI elements not found");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Chat UI failed to load", Toast.LENGTH_SHORT).show();
            }
        } else {
            chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            chatRecyclerView.setAdapter(chatAdapter);
            messageEditText.setVisibility(View.VISIBLE);
            sendButton.setVisibility(View.VISIBLE);
            sendButton.setEnabled(true);
            sendButton.setOnClickListener(v -> sendMessage());
        }

        receiverId = getArguments() != null ? getArguments().getString("receiverId") : null;
        carId = getArguments() != null ? getArguments().getString("carId") : null;
        contractId = getArguments() != null ? getArguments().getString("contractId") : null;

        if ((receiverId != null || carId != null) && isNetworkAvailable() && mAuth.getCurrentUser() != null) {
            loadMessages();
        } else {
            if (!isNetworkAvailable() && isAdded()) {
                Toast.makeText(requireContext(), "No internet. Messages will be queued if possible.", Toast.LENGTH_LONG).show();
            }
            if (mAuth.getCurrentUser() == null && isAdded()) {
                Toast.makeText(requireContext(), "Please sign in to view messages", Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (!messageText.isEmpty()) {
            if (mAuth.getCurrentUser() == null) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Please sign in to send messages", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            String userId = mAuth.getCurrentUser().getUid();
            if (receiverId != null) {
                sendMessageToUser(userId, receiverId, messageText, carId, contractId);
            } else if (carId != null) {
                db.collection("Cars").document(carId)
                        .get()
                        .addOnSuccessListener(carDoc -> {
                            String managerId = carDoc.getString("managerId");
                            if (managerId != null && !managerId.isEmpty()) {
                                sendMessageToUser(userId, managerId, messageText, carId, contractId);
                            } else {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Cannot send: No manager for this car", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    private void sendMessageToUser(String userId, String receiverId, String messageText, String carId, String contractId) {
        Message message = new Message(userId, receiverId, messageText, Timestamp.now());
        if (carId != null) {
            message.setCarId(carId);
        }
        db.collection("Messages").add(message)
                .addOnSuccessListener(doc -> {
                    messageEditText.setText("");
                    db.collection("Users").document(receiverId)
                            .get()
                            .addOnSuccessListener(document -> {
                                String token = document.getString("fcmToken");
                                if (token != null && !token.isEmpty()) {
                                    sendFcmNotification(token, "New Message", messageText);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendFcmNotification(String token, String title, String body) {
        JSONObject json = new JSONObject();
        try {
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);
            json.put("notification", notification);
            json.put("to", token);
            RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .post(requestBody)
                    .addHeader("Authorization", "key=YOUR_FCM_SERVER_KEY")
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("ChatFragment", "Failed to send notification: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e("ChatFragment", "Notification send failed: " + response.code());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e("ChatFragment", "JSON error in notification: " + e.getMessage());
        }
    }

    private void loadMessages() {
        if (mAuth.getCurrentUser() == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Please sign in to view messages", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        String otherUserId = receiverId != null ? receiverId : null;
        if (otherUserId == null && carId != null) {
            db.collection("Cars").document(carId)
                    .get()
                    .addOnSuccessListener(carDoc -> {
                        String managerId = carDoc.getString("managerId");
                        if (managerId != null && !managerId.isEmpty()) {
                            queryMessages(userId, managerId);
                        } else {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Failed to load: No manager found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            queryMessages(userId, otherUserId);
        }
    }

    private void queryMessages(String userId, String otherUserId) {
        if (otherUserId == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Invalid recipient for messages", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        Query query = db.collection("Messages")
                .whereIn("senderId", Arrays.asList(userId, otherUserId))
                .whereIn("receiverId", Arrays.asList(userId, otherUserId));
        query.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to load messages: Check network", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    messageList.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots) {
                            try {
                                Message message = doc.toObject(Message.class);
                                if (message != null) {
                                    message.setId(doc.getId()); // Fixed: id field now exists
                                    messageList.add(message);
                                    if (message.getReceiverId().equals(userId) && !message.isRead() &&
                                            "BOOKING_NOTIFICATION".equals(message.getMessageType())) {
                                        doc.getReference().update("isRead", true)
                                                .addOnFailureListener(ex -> Log.e("ChatFragment", "Failed to mark message as read: " + ex.getMessage()));
                                    }
                                }
                            } catch (Exception ex) {
                                Log.e("ChatFragment", "Error deserializing message: " + ex.getMessage());
                            }
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    if (chatRecyclerView != null) {
                        chatRecyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    public void sendConfirmationMessage(String messageId, String receiverId, String carId) {
        String confirmationText = "Thank you for booking! You will receive the car at the specified location.";
        Map<String, Object> confirmationData = new HashMap<>();
        confirmationData.put("senderId", mAuth.getCurrentUser().getUid());
        confirmationData.put("receiverId", receiverId);
        confirmationData.put("message", confirmationText);
        confirmationData.put("timestamp", Timestamp.now());
        confirmationData.put("carId", carId);
        confirmationData.put("messageType", "CONFIRMATION");
        confirmationData.put("isRead", false);
        confirmationData.put("confirmed", false);

        db.collection("Messages").add(confirmationData)
                .addOnSuccessListener(doc -> {
                    db.collection("Messages").document(messageId)
                            .update("confirmed", true)
                            .addOnSuccessListener(aVoid -> Log.d("ChatFragment", "Confirmation message sent and marked as confirmed"))
                            .addOnFailureListener(e -> Log.e("ChatFragment", "Failed to mark as confirmed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to send confirmation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}