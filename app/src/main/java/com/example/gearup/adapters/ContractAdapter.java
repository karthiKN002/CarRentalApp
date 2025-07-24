package com.example.gearup.adapters;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.models.Contract;
import com.example.gearup.states.contract.ContractState;
import com.example.gearup.uiactivities.customer.ViewContractDetailsFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ContractAdapter extends RecyclerView.Adapter<ContractAdapter.ContractViewHolder> {

    private Context context;
    private List<Contract> contractList;
    private FirebaseFirestore db;

    public ContractAdapter(Context context, List<Contract> contractList) {
        this.context = context;
        this.contractList = contractList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ContractViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contract, parent, false);
        return new ContractViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContractViewHolder holder, int position) {
        Contract contract = contractList.get(position);

        // Set car name to nameTextView
        String carName = contract.getCarName() != null ? contract.getCarName() : null;
        if (carName == null) {
            db.collection("Cars").document(contract.getCarId()).get()
                    .addOnSuccessListener(carSnapshot -> {
                        if (carSnapshot.exists()) {
                            Log.d("ContractAdapter", "Car document fields for carId " + contract.getCarId() + ": " + carSnapshot.getData());
                            String fetchedCarName = carSnapshot.getString("name");
                            if (fetchedCarName == null) fetchedCarName = carSnapshot.getString("model");
                            if (fetchedCarName == null) fetchedCarName = carSnapshot.getString("brandModel");
                            if (fetchedCarName == null) fetchedCarName = "N/A";
                            holder.nameTextView.setText("Car: " + fetchedCarName);
                            Log.d("ContractAdapter", "Fetched carName: " + fetchedCarName + " for carId: " + contract.getCarId());
                        } else {
                            holder.nameTextView.setText("Car: N/A");
                            Log.e("ContractAdapter", "Car document does not exist for carId: " + contract.getCarId());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ContractAdapter", "Error fetching car data for carId: " + contract.getCarId() + ", error: " + e.getMessage());
                        holder.nameTextView.setText("Car: N/A");
                    });
        } else {
            holder.nameTextView.setText("Car: " + carName);
            Log.d("ContractAdapter", "Used carName from contract: " + carName);
        }

        // Fetch user data for customer name and details
        db.collection("users").document(contract.getUserId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fullName = documentSnapshot.getString("fullName") != null ? documentSnapshot.getString("fullName") : "N/A";
                    String email = documentSnapshot.getString("email") != null ? documentSnapshot.getString("email") : "N/A";
                    holder.carNameTextView.setText("Customer Name: " + fullName);

                    // Set other fields
                    String dateRange = "Dates: From " + formatTimestamp(contract.getStartDate()) + " to " + formatTimestamp(contract.getEndDate());
                    holder.dateRangeTextView.setText(dateRange);
                    holder.totalPaymentTextView.setText(String.format(Locale.US, "Total: ₹%.2f", contract.getTotalPayment()));
                    holder.statusTextView.setText("Status: " + contract.getState().toString());

                    // Show status dot for ACTIVE status only
                    holder.statusDot.setVisibility(contract.getState() == ContractState.ACTIVE ? View.VISIBLE : View.GONE);

                    // Show paid tick
                    holder.paidTick.setVisibility(View.VISIBLE);

                    // Set click listener
                    holder.itemView.setOnClickListener(v -> {
                        FragmentActivity fragmentActivity = (FragmentActivity) context;
                        ViewContractDetailsFragment detailsFragment = new ViewContractDetailsFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("contractId", contract.getId());
                        bundle.putString("eventId", contract.getEventId());
                        bundle.putString("userId", contract.getUserId());
                        bundle.putString("fullName", fullName);
                        bundle.putString("email", email);
                        bundle.putString("carId", contract.getCarId());
                        bundle.putString("carName", carName != null ? carName : "N/A");
                        bundle.putParcelable("startDate", contract.getStartDate());
                        bundle.putParcelable("endDate", contract.getEndDate());
                        bundle.putParcelable("createdAt", contract.getCreatedAt());
                        bundle.putDouble("totalPayment", contract.getTotalPayment());
                        bundle.putString("status", contract.getState().toString());
                        bundle.putString("managerId", contract.getManagerId());
                        Log.d("ContractAdapter", "Passing to ViewContractDetails: " + bundle.toString());
                        detailsFragment.setArguments(bundle);

                        fragmentActivity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(getContainerId(fragmentActivity), detailsFragment)
                                .addToBackStack(null)
                                .commit();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("ContractAdapter", "Error fetching user data: " + e.getMessage());
                    holder.carNameTextView.setText("Customer Name: N/A");
                    // Still allow navigation with fallback data
                    holder.itemView.setOnClickListener(v -> {
                        FragmentActivity fragmentActivity = (FragmentActivity) context;
                        ViewContractDetailsFragment detailsFragment = new ViewContractDetailsFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("contractId", contract.getId());
                        bundle.putString("eventId", contract.getEventId());
                        bundle.putString("userId", contract.getUserId());
                        bundle.putString("fullName", "N/A");
                        bundle.putString("email", "N/A");
                        bundle.putString("carId", contract.getCarId());
                        bundle.putString("carName", contract.getCarName() != null ? contract.getCarName() : "N/A");
                        bundle.putParcelable("startDate", contract.getStartDate());
                        bundle.putParcelable("endDate", contract.getEndDate());
                        bundle.putParcelable("createdAt", contract.getCreatedAt());
                        bundle.putDouble("totalPayment", contract.getTotalPayment());
                        bundle.putString("status", contract.getState().toString());
                        bundle.putString("managerId", contract.getManagerId());
                        Log.d("ContractAdapter", "Passing to ViewContractDetails (fallback): " + bundle.toString());
                        detailsFragment.setArguments(bundle);

                        fragmentActivity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(getContainerId(fragmentActivity), detailsFragment)
                                .addToBackStack(null)
                                .commit();
                    });
                });
    }

    private int getContainerId(FragmentActivity activity) {
        if (activity instanceof com.example.gearup.uiactivities.manager.ManagerDashboardActivity) {
            return R.id.managerFragmentContainer;
        } else if (activity instanceof com.example.gearup.uiactivities.customer.CustomerDashboardActivity) {
            return R.id.customerFragmentContainer;
        } else if (activity instanceof com.example.gearup.uiactivities.mechanic.MechanicDashboardActivity) {
            return R.id.mechanicFragmentContainer;
        }
        return R.id.customerFragmentContainer; // Fallback
    }

    @Override
    public int getItemCount() {
        return contractList.size();
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    static class ContractViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, carNameTextView, dateRangeTextView, totalPaymentTextView, statusTextView;
        View statusDot, paidTick;

        ContractViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            carNameTextView = itemView.findViewById(R.id.carNameTextView);
            dateRangeTextView = itemView.findViewById(R.id.dateRangeTextView);
            totalPaymentTextView = itemView.findViewById(R.id.totalPaymentTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            statusDot = itemView.findViewById(R.id.statusDot);
            paidTick = itemView.findViewById(R.id.paidTick);
        }
    }
}