package com.example.gearup.uiactivities.admin;

import android.content.Intent;
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

public class AdminApprovalActivity extends AppCompatActivity implements PendingApprovalAdapter.OnViewDetailsClickListener {
    private static final String TAG = "AdminApprovalActivity";
    private RecyclerView recyclerView;
    private PendingApprovalAdapter adapter;
    private FirebaseFirestore db;
    private List<PendingApproval> pendingUsers = new ArrayList<>();
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
        adapter = new PendingApprovalAdapter(pendingUsers, this);
        recyclerView.setAdapter(adapter);

        String userType = getIntent().getStringExtra("userType");
        Log.d(TAG, "Received userType: " + userType);
        if (userType == null || userType.isEmpty()) {
            Log.e(TAG, "userType is null or empty, defaulting to 'manager'");
            userType = "manager";
        }
        loadPendingUsers(userType);

        // Debug: Fetch specific document to test deserialization
        db.collection("users").document("jrN5l7cBmhNqNyL8T3ZL93hBTHd2").get()
                .addOnSuccessListener(document -> {
                    Log.d(TAG, "Manual fetch data: " + document.getData());
                    try {
                        PendingApproval user = document.toObject(PendingApproval.class);
                        if (user != null) {
                            user.setId(document.getId());
                            Log.d(TAG, "Manual deserialization success: " + user.getFullName() + ", ID: " + user.getId());
                        } else {
                            Log.e(TAG, "Manual deserialization returned null");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Manual deserialization error: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Manual fetch error: " + e.getMessage()));
    }

    private void loadPendingUsers(String userType) {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        listenerRegistration = db.collection("users")
                .whereEqualTo("isApproved", false)
                .whereEqualTo("userType", userType)
                .addSnapshotListener((querySnapshot, e) -> {
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    if (e != null) {
                        Log.e(TAG, "Firestore error: " + e.getMessage());
                        Toast.makeText(this, "Failed to load users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    pendingUsers.clear();
                    Log.d(TAG, "Query snapshot size: " + (querySnapshot != null ? querySnapshot.size() : 0));
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Log.d(TAG, "Raw document data: " + document.getData());
                            try {
                                PendingApproval user = document.toObject(PendingApproval.class);
                                user.setId(document.getId());
                                pendingUsers.add(user);
                                Log.d(TAG, "Added user: " + user.getFullName() + ", ID: " + user.getId());
                            } catch (Exception ex) {
                                Log.e(TAG, "Deserialization error for doc " + document.getId() + ": " + ex.getMessage());
                            }
                        }
                    }
                    Log.d(TAG, "Total users loaded: " + pendingUsers.size());
                    adapter.updateList(pendingUsers);
                    adapter.notifyDataSetChanged(); // Ensure adapter is notified
                    Log.d(TAG, "Adapter item count: " + adapter.getItemCount());

                    // Handle empty state
                    View emptyView = findViewById(R.id.emptyView);
                    if (emptyView != null) {
                        emptyView.setVisibility(pendingUsers.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(pendingUsers.isEmpty() ? View.GONE : View.VISIBLE);
                        Log.d(TAG, "Empty view visibility: " + (emptyView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                        Log.d(TAG, "RecyclerView visibility: " + (recyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                    }
                });
    }

    @Override
    public void onViewDetailsClick(PendingApproval user) {
        Log.d(TAG, "View details clicked for user: " + user.getId());
        Intent intent = new Intent(this, ManagerDetailsActivity.class);
        intent.putExtra("managerId", user.getId());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}