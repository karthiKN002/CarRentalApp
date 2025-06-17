package com.example.gearup.uiactivities.admin;

import android.content.Intent;
import android.net.Uri;
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

    private List<PendingApproval> pendingApprovals;
    private final OnApproveClickListener approveClickListener;

    public PendingApprovalAdapter(List<PendingApproval> pendingApprovals, OnApproveClickListener listener) {
        this.pendingApprovals = pendingApprovals != null ? pendingApprovals : new ArrayList<>();
        this.approveClickListener = listener;
    }

    public void updateList(List<PendingApproval> newList) {
        this.pendingApprovals = newList != null ? newList : new ArrayList<>();
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
        PendingApproval approval = pendingApprovals.get(position);
        holder.bind(approval, approveClickListener);
    }

    @Override
    public int getItemCount() {
        return pendingApprovals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView emailTextView;
        private final TextView phoneTextView;
        private final TextView documentStatusTextView;
        private final TextView videoStatusTextView;
        private final Button viewDetailsButton;
        private final Button approveButton;
        private final Button rejectButton;

        public ViewHolder(View view) {
            super(view);
            nameTextView = view.findViewById(R.id.textViewName);
            emailTextView = view.findViewById(R.id.textViewEmail);
            phoneTextView = view.findViewById(R.id.textViewPhone);
            documentStatusTextView = view.findViewById(R.id.textViewDocumentStatus);
            videoStatusTextView = view.findViewById(R.id.textViewVideoStatus);
            viewDetailsButton = view.findViewById(R.id.buttonViewDetails);
            approveButton = view.findViewById(R.id.buttonApprove);
            rejectButton = view.findViewById(R.id.buttonReject);
        }

        public void bind(PendingApproval approval, OnApproveClickListener listener) {
            nameTextView.setText(String.format("Name: %s", approval.getFullName()));
            emailTextView.setText(String.format("Email: %s", approval.getEmail()));
            phoneTextView.setText(String.format("Phone: %s", approval.getPhone()));

            documentStatusTextView.setText(approval.hasDocument() ? "Document: Available" : "Document: Not submitted");
            videoStatusTextView.setText(approval.hasVideo() ? "Video: Available" : "Video: Not submitted");

            viewDetailsButton.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ManagerDetailsActivity.class);
                intent.putExtra("manager_id", approval.getId());
                intent.putExtra("full_name", approval.getFullName());
                intent.putExtra("email", approval.getEmail());
                intent.putExtra("phone", approval.getPhone());
                intent.putExtra("document_url", approval.getDocumentUrl());
                intent.putExtra("video_url", approval.getStoreVideo());
                v.getContext().startActivity(intent);
            });

            approveButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApproveClick(approval);
                }
            });

            rejectButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRejectClick(approval.getId());
                }
            });
        }
    }
}