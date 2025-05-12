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
        private final TextView emailTextView;
        private final TextView phoneTextView;
        private final TextView documentStatusTextView;
        private final Button approveButton;
        private final Button rejectButton;
        private final Button viewDocumentButton;

        public ViewHolder(View view) {
            super(view);
            emailTextView = view.findViewById(R.id.textViewEmail);
            phoneTextView = view.findViewById(R.id.textViewPhone);
            documentStatusTextView = view.findViewById(R.id.textViewDocumentStatus);
            approveButton = view.findViewById(R.id.buttonApprove);
            rejectButton = view.findViewById(R.id.buttonReject);
            viewDocumentButton = view.findViewById(R.id.buttonViewDocument);
        }

        public void bind(PendingApproval approval, OnApproveClickListener listener) {
            emailTextView.setText(String.format("Email: %s", approval.getEmail()));
            phoneTextView.setText(String.format("Phone: %s", approval.getPhone()));

            if (approval.hasDocument()) {
                documentStatusTextView.setText("Document: Available");
                viewDocumentButton.setVisibility(View.VISIBLE);
                viewDocumentButton.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(approval.getDocumentUrl()));
                        v.getContext().startActivity(intent);
                    } catch (Exception e) {
                        Log.e("PendingApprovalAdapter", "Failed to open document URL: " + approval.getDocumentUrl(), e);
                        documentStatusTextView.setText("Document: Cannot open (install a PDF viewer)");
                        viewDocumentButton.setVisibility(View.GONE);
                    }
                });
            } else {
                documentStatusTextView.setText("Document: Not submitted");
                viewDocumentButton.setVisibility(View.GONE);
            }

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