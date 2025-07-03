package com.example.gearup.uiactivities.admin;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import java.util.ArrayList;
import java.util.List;

public class PendingApprovalAdapter extends RecyclerView.Adapter<PendingApprovalAdapter.ViewHolder> {
    private static final String TAG = "PendingApprovalAdapter";
    private List<PendingApproval> pendingUsers;
    private final OnViewDetailsClickListener listener;

    public interface OnViewDetailsClickListener {
        void onViewDetailsClick(PendingApproval user);
    }

    public PendingApprovalAdapter(List<PendingApproval> pendingUsers, OnViewDetailsClickListener listener) {
        this.pendingUsers = pendingUsers != null ? pendingUsers : new ArrayList<>();
        this.listener = listener;
        Log.d(TAG, "Adapter initialized with " + this.pendingUsers.size() + " users");
    }

    public void updateList(List<PendingApproval> newUsers) {
        Log.d(TAG, "Updating list with " + (newUsers != null ? newUsers.size() : 0) + " users");
        this.pendingUsers = newUsers != null ? new ArrayList<>(newUsers) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_approval, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingApproval user = pendingUsers.get(position);
        Log.d(TAG, "Binding user: " + user.getFullName() + ", ID: " + user.getId() + " at position " + position);
        holder.textViewName.setText(user.getFullName());
        holder.textViewEmail.setText(user.getEmail());
        holder.textViewUserType.setText(user.getUserType());
        holder.buttonViewDetails.setOnClickListener(v -> {
            Log.d(TAG, "View details clicked for user: " + user.getId());
            listener.onViewDetailsClick(user);
        });
    }

    @Override
    public int getItemCount() {
        int count = pendingUsers.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewEmail, textViewUserType;
        Button buttonViewDetails;

        ViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewUserType = itemView.findViewById(R.id.textViewUserType);
            buttonViewDetails = itemView.findViewById(R.id.buttonViewDetails);
        }
    }
}