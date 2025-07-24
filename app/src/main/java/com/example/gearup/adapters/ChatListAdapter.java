package com.example.gearup.adapters;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.models.ChatUser;
import com.example.gearup.uiactivities.customer.MechanicDetailsFragment;
import com.example.gearup.uiactivities.customer.ProfileFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatUserViewHolder> {

    private Context context;
    private List<ChatUser> chatUsers;
    private OnChatItemClickListener customClickListener;

    public ChatListAdapter(Context context, List<ChatUser> chatUsers) {
        this.context = context;
        this.chatUsers = chatUsers;
    }

    public interface OnChatItemClickListener {
        void onChatItemClicked(String userId);
    }

    public void setCustomClickListener(OnChatItemClickListener listener) {
        this.customClickListener = listener;
    }

    @NonNull
    @Override
    public ChatUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_user, parent, false);
        return new ChatUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatUserViewHolder holder, int position) {
        ChatUser chatUser = chatUsers.get(position);
        holder.userName.setText(Html.fromHtml("<b>" + chatUser.getName() + "</b>"));
        String email = chatUser.getEmail() != null ? chatUser.getEmail() : "No email";
        holder.email.setText("Email: " + email);
        holder.carName.setText(""); // Remove car name display
        if (chatUser.getTimestamp() != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.timestamp.setText(sdf.format(chatUser.getTimestamp()));
        } else {
            holder.timestamp.setText("");
        }
        holder.itemView.setOnClickListener(v -> {
            String userId = chatUser.getUserId();
            Log.d("ChatListAdapter", "Item clicked with userId: " + userId);
            if (customClickListener != null) {
                customClickListener.onChatItemClicked(userId);
            } else {
                // Check user type to decide navigation
                FirebaseFirestore.getInstance().collection("users").document(userId)
                        .get()
                        .addOnSuccessListener(document -> {
                            String userType = document.getString("userType");
                            FragmentActivity activity = (FragmentActivity) context;
                            Bundle bundle = new Bundle();
                            bundle.putString("userId", userId);
                            if ("mechanic".equals(userType)) {
                                MechanicDetailsFragment fragment = new MechanicDetailsFragment();
                                bundle.putString("mechanicId", userId);
                                fragment.setArguments(bundle);
                                activity.getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.customerFragmentContainer, fragment)
                                        .addToBackStack(null)
                                        .commit();
                            } else {
                                ProfileFragment fragment = new ProfileFragment();
                                fragment.setArguments(bundle);
                                activity.getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.customerFragmentContainer, fragment)
                                        .addToBackStack(null)
                                        .commit();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ChatListAdapter", "Error fetching user type: " + e.getMessage());
                        });
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatUsers.size();
    }

    static class ChatUserViewHolder extends RecyclerView.ViewHolder {
        TextView userName, email, carName, timestamp;

        public ChatUserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            email = itemView.findViewById(R.id.email);
            carName = itemView.findViewById(R.id.carName);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
}