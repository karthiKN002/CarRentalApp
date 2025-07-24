package com.example.gearup.uiactivities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gearup.R;
import com.example.gearup.utilities.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CarApprovalActivity extends AppCompatActivity implements OnCarApproveClickListener {
    private static final String TAG = "CarApprovalActivity";
    private RecyclerView recyclerView;
    private CarPendingApprovalAdapter adapter;
    private FirebaseFirestore db;
    private final List<CarPendingApproval> pendingCars = Collections.synchronizedList(new ArrayList<>());
    private ProgressBar progressBar;
    private TextView emptyView;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_approval);

        // Check authentication
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(this, "Please log in as admin", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        Log.d(TAG, "Authenticated user: " + auth.getCurrentUser().getUid());

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CarPendingApprovalAdapter(pendingCars, this);
        recyclerView.setAdapter(adapter);

        // Debug: Fetch specific document
        db.collection("pending_cars").document("56b04351-8f87-4aa4-8662-387fdaa61e2e").get()
                .addOnSuccessListener(document -> {
                    Log.d(TAG, "Manual fetch data: " + document.getData());
                    try {
                        CarPendingApproval car = document.toObject(CarPendingApproval.class);
                        if (car != null) {
                            car.setId(document.getId());
                            Log.d(TAG, "Manual deserialization success: " + car.getBrand() + " " + car.getModel() + ", ID: " + car.getId());
                        } else {
                            Log.e(TAG, "Manual deserialization returned null");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Manual deserialization error: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Manual fetch error: " + e.getMessage()));

        loadPendingCars();
    }

    private void loadPendingCars() {
        Log.d(TAG, "Loading pending cars");
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        listenerRegistration = db.collection("pending_cars")
                .whereEqualTo("state", "PENDING")
                .addSnapshotListener((querySnapshot, e) -> {
                    Log.d(TAG, "Snapshot listener triggered");
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        Log.e(TAG, "Query failed: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Failed to load cars: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("Error loading cars: " + e.getMessage());
                        });
                        return;
                    }

                    List<CarPendingApproval> tempCars = new ArrayList<>();
                    Log.d(TAG, "Query snapshot size: " + (querySnapshot != null ? querySnapshot.size() : 0));
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Log.d(TAG, "Raw document data: " + document.getData());
                            try {
                                CarPendingApproval car = document.toObject(CarPendingApproval.class);
                                car.setId(document.getId());
                                tempCars.add(car);
                                Log.d(TAG, "Added car: " + car.getBrand() + " " + car.getModel() + ", ID: " + car.getId());
                            } catch (Exception ex) {
                                Log.e(TAG, "Deserialization error for doc " + document.getId() + ": " + ex.getMessage());
                            }
                        }
                    }

                    synchronized (pendingCars) {
                        pendingCars.clear();
                        pendingCars.addAll(tempCars);
                        Log.d(TAG, "Total cars loaded: " + pendingCars.size());
                        runOnUiThread(() -> {
                            adapter.updateList(new ArrayList<>(pendingCars));
                            adapter.notifyDataSetChanged();
                            Log.d(TAG, "Adapter item count after update: " + adapter.getItemCount());
                            updateEmptyState();
                        });
                    }
                });
    }

    private void updateEmptyState() {
        synchronized (pendingCars) {
            Log.d(TAG, "Updating empty state, pendingCars size: " + pendingCars.size());
            Log.d(TAG, "Adapter item count: " + adapter.getItemCount());
            if (pendingCars.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("No pending cars for approval");
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
            Log.d(TAG, "Empty view visibility: " + (emptyView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
            Log.d(TAG, "RecyclerView visibility: " + (recyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
        }
    }

    @Override
    public void onViewDetailsClick(CarPendingApproval car) {
        Log.d(TAG, "View details clicked for car ID: " + car.getId());
        Intent intent = new Intent(this, CarDetailsActivity.class);
        intent.putExtra("carId", car.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Reloading pending cars");
        loadPendingCars();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            Log.d(TAG, "Snapshot listener removed");
        }
    }
}