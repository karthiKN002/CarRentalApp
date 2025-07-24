package com.example.gearup.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.models.Message;
import com.example.gearup.uiactivities.customer.CustomerDashboardActivity;
import com.example.gearup.uiactivities.manager.ChatFragment;
import com.example.gearup.uiactivities.manager.ManagerDashboardActivity;
import com.example.gearup.uiactivities.mechanic.MechanicDashboardActivity;
import com.example.gearup.utilities.MapPickerActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_BOOKING_NOTIFICATION = 3;
    private final List<Message> messages;
    private final String currentUserId;
    private final Context context;
    private final FirebaseFirestore db;

    public ChatAdapter(Context context, List<Message> messages, String currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if ("BOOKING_NOTIFICATION".equals(message.getMessageType())) {
            return VIEW_TYPE_BOOKING_NOTIFICATION;
        }
        return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_BOOKING_NOTIFICATION) {
            View view = inflater.inflate(R.layout.item_message_booking, parent, false);
            return new BookingNotificationViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, MMM dd", Locale.getDefault());
        String timestamp = sdf.format(message.getTimestamp().toDate());

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message, timestamp);
        } else if (holder.getItemViewType() == VIEW_TYPE_BOOKING_NOTIFICATION) {
            ((BookingNotificationViewHolder) holder).bind(message, timestamp);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message, timestamp);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timestampText;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_sent);
            timestampText = itemView.findViewById(R.id.timestamp_sent);
        }

        void bind(Message message, String timestamp) {
            messageText.setText(message.getMessage());
            timestampText.setText(timestamp);
        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, senderName, timestampText;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_received);
            senderName = itemView.findViewById(R.id.sender_name);
            timestampText = itemView.findViewById(R.id.timestamp_received);
        }

        void bind(Message message, String timestamp) {
            messageText.setText(message.getMessage());
            timestampText.setText(timestamp);
            db.collection("users").document(message.getSenderId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String fullName = documentSnapshot.getString("fullName");
                        senderName.setText(fullName != null && !fullName.isEmpty() ? fullName : "Unknown User");
                    })
                    .addOnFailureListener(e -> {
                        senderName.setText("Unknown User");
                        Log.e("ChatAdapter", "Error fetching sender name: " + e.getMessage());
                    });
        }
    }

    class BookingNotificationViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, senderName, timestampText;
        Button getLocationButton, sendConfirmationButton;

        BookingNotificationViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.bookingMessageTextView);
            senderName = itemView.findViewById(R.id.sender_name);
            timestampText = itemView.findViewById(R.id.timestamp_booking);
            getLocationButton = itemView.findViewById(R.id.getLocationButton);
            sendConfirmationButton = itemView.findViewById(R.id.sendConfirmationButton);
        }

        void bind(Message message, String timestamp) {
            messageText.setText(message.getMessage());
            timestampText.setText(timestamp);
            db.collection("users").document(message.getSenderId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String fullName = documentSnapshot.getString("fullName");
                        senderName.setText(fullName != null && !fullName.isEmpty() ? fullName : "Unknown User");
                    })
                    .addOnFailureListener(e -> {
                        senderName.setText("Unknown User");
                        Log.e("ChatAdapter", "Error fetching sender name: " + e.getMessage());
                    });

            // Show Send Confirmation button only for managers if not confirmed
            SharedPreferences prefs = context.getSharedPreferences("CarRentalAppPrefs", Context.MODE_PRIVATE);
            String role = prefs.getString("user_role", "customer");
            if ("manager".equals(role) && !message.isConfirmed()) {
                sendConfirmationButton.setVisibility(View.VISIBLE);
                sendConfirmationButton.setOnClickListener(v -> {
                    FragmentActivity activity = (FragmentActivity) context;
                    ChatFragment chatFragment = (ChatFragment) activity.getSupportFragmentManager()
                            .findFragmentById(getContainerId(activity));
                    if (chatFragment != null) {
                        chatFragment.sendConfirmationMessage(message.getId(), message.getSenderId(), message.getCarId());
                        sendConfirmationButton.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(context, "Error sending confirmation", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                sendConfirmationButton.setVisibility(View.GONE);
            }

            getLocationButton.setOnClickListener(v -> {
                String pickupLocation = message.getPickupLocation();
                if (pickupLocation != null && pickupLocation.contains(",")) {
                    String[] parts = pickupLocation.split(",");
                    try {
                        double latitude = Double.parseDouble(parts[parts.length - 2].trim());
                        double longitude = Double.parseDouble(parts[parts.length - 1].trim());
                        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude + "&mode=d");
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                            context.startActivity(mapIntent);
                        } else {
                            Toast.makeText(context, "Google Maps not installed", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Invalid location format", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private int getContainerId(FragmentActivity activity) {
            if (activity instanceof CustomerDashboardActivity) {
                return R.id.customerFragmentContainer;
            } else if (activity instanceof ManagerDashboardActivity) {
                return R.id.managerFragmentContainer;
            } else if (activity instanceof MechanicDashboardActivity) {
                return R.id.mechanicFragmentContainer;
            }
            return R.id.managerFragmentContainer; // Fallback
        }
    }
}