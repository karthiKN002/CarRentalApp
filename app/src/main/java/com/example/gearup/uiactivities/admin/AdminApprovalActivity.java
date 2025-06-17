package com.example.gearup.uiactivities.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AdminApprovalActivity extends AppCompatActivity implements OnApproveClickListener {
    private static final String TAG = "AdminApprovalActivity";
    private RecyclerView recyclerView;
    private PendingApprovalAdapter adapter;
    private FirebaseFirestore db;
    private List<PendingApproval> pendingApprovals = new ArrayList<>();
    private ProgressBar progressBar;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approval);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingApprovalAdapter(pendingApprovals, this);
        recyclerView.setAdapter(adapter);

        loadPendingManagers();
    }

    private void loadPendingManagers() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        listenerRegistration = db.collection("users")
                .whereEqualTo("userType", "manager")
                .whereEqualTo("isApproved", false)
                .addSnapshotListener((querySnapshot, e) -> {
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    if (e != null) {
                        Log.e(TAG, "Failed to load pending managers: " + e.getMessage());
                        Toast.makeText(this, "Failed to load pending managers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    pendingApprovals.clear();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            PendingApproval approval = document.toObject(PendingApproval.class);
                            approval.setId(document.getId());
                            Log.d(TAG, "Loaded manager: " + approval.getFullName() + ", Document: " + approval.getDocumentUrl());
                            pendingApprovals.add(approval);
                        }
                    } else {
                        Log.w(TAG, "Query snapshot is null");
                    }
                    adapter.updateList(pendingApprovals);
                });
    }

    @Override
    public void onApproveClick(PendingApproval approval) {
        db.collection("users").document(approval.getId())
                .update("isApproved", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Manager approved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to approve manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRejectClick(String uid) {
        db.collection("users").document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Manager rejected", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to reject manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}