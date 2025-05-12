package com.example.gearup.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.gearup.R;
import com.example.gearup.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private Context context;
    private List<User> userList;
    private FirebaseFirestore db;
    private boolean isAdmin;

    public UserAdapter(Context context, List<User> userList, boolean isAdmin) {
        this.context = context;
        this.userList = userList;
        this.db = FirebaseFirestore.getInstance();
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textViewUserName.setText(user.getFullName() != null ? user.getFullName() : "Unknown");
        holder.textViewUserEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
        holder.textViewUserType.setText(user.getUserType() != null ? user.getUserType() : "Unknown");

        // Load profile image with Glide
        String profileImageUrl = user.getImgUrl();
        Glide.with(context)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_user_avatar_placeholder)
                .error(R.drawable.ic_user_avatar_placeholder)
                .into(holder.imageViewAvatar);

        // Admin-specific controls
        if (isAdmin) {
            // Block/Unblock button
            holder.buttonBlockUnblock.setVisibility(View.VISIBLE);
            holder.buttonBlockUnblock.setText(user.isBlocked() ? "Unblock" : "Block");
            holder.buttonBlockUnblock.setBackgroundTintList(ContextCompat.getColorStateList(
                    context, user.isBlocked() ? R.color.colorAccent : R.color.colorMoneyGreen));
            holder.buttonBlockUnblock.setOnClickListener(v -> toggleBlockStatus(user, position));

            // Approve button and PDF view for managers
            if ("manager".equals(user.getUserType()) && !user.isApproved()) {
                holder.buttonApprove.setVisibility(View.VISIBLE);
                holder.buttonApprove.setOnClickListener(v -> approveManager(user, position));
                holder.buttonViewDocument.setVisibility(View.VISIBLE);
                holder.buttonViewDocument.setOnClickListener(v -> viewDocument(user.getLicenseDocument()));
            } else {
                holder.buttonApprove.setVisibility(View.GONE);
                holder.buttonViewDocument.setVisibility(View.GONE);
            }
        } else {
            holder.buttonBlockUnblock.setVisibility(View.GONE);
            holder.buttonApprove.setVisibility(View.GONE);
            holder.buttonViewDocument.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void toggleBlockStatus(User user, int position) {
        boolean newBlockedStatus = !user.isBlocked();
        db.collection("users").document(user.getUid())
                .update("blocked", newBlockedStatus)
                .addOnSuccessListener(aVoid -> {
                    user.setBlocked(newBlockedStatus);
                    notifyItemChanged(position);
                    Toast.makeText(context, user.getFullName() + " has been " + (newBlockedStatus ? "blocked" : "unblocked"), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update block status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void approveManager(User user, int position) {
        db.collection("users").document(user.getUid())
                .update("isApproved", true)
                .addOnSuccessListener(aVoid -> {
                    user.setIsApproved(true);
                    notifyItemChanged(position);
                    Toast.makeText(context, user.getFullName() + " has been approved as a manager", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to approve manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void viewDocument(String documentUrl) {
        if (documentUrl == null || documentUrl.isEmpty()) {
            Toast.makeText(context, "No document available", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(documentUrl));
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewAvatar;
        TextView textViewUserName, textViewUserEmail, textViewUserType;
        Button buttonBlockUnblock, buttonApprove, buttonViewDocument;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewUserName = itemView.findViewById(R.id.textViewUserName);
            textViewUserEmail = itemView.findViewById(R.id.textViewUserEmail);
            textViewUserType = itemView.findViewById(R.id.textViewUserType);
            buttonBlockUnblock = itemView.findViewById(R.id.buttonBlockUnblock);
            buttonApprove = itemView.findViewById(R.id.buttonApprove);
            buttonViewDocument = itemView.findViewById(R.id.buttonViewDocument);
        }
    }
}