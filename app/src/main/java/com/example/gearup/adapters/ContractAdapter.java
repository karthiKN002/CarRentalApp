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

        holder.contractIdTextView.setText("Contract ID: " + contract.getId());
        holder.carIdTextView.setText("Car ID: " + contract.getCarId());
        holder.totalPaymentTextView.setText("Total: $" + contract.getTotalPayment());
        holder.statusTextView.setText("Status: " + contract.getState().toString());
        holder.startDateTextView.setText("Start: " + formatTimestamp(contract.getStartDate()));
        holder.endDateTextView.setText("End: " + formatTimestamp(contract.getEndDate()));

        // Fetch user data
        db.collection("users").document(contract.getUserId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fullName = documentSnapshot.getString("fullName") != null ? documentSnapshot.getString("fullName") : "N/A";
                    String email = documentSnapshot.getString("email") != null ? documentSnapshot.getString("email") : "N/A";

                    holder.itemView.setOnClickListener(v -> {
                        FragmentActivity fragmentActivity = (FragmentActivity) context;
                        ViewContractDetailsFragment detailsFragment = new ViewContractDetailsFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("contractId", contract.getId());
                        bundle.putString("eventId", contract.getEventId());
                        bundle.putString("fullName", fullName);
                        bundle.putString("email", email);
                        bundle.putString("carId", contract.getCarId());
                        bundle.putParcelable("startDate", contract.getStartDate());
                        bundle.putParcelable("endDate", contract.getEndDate());
                        bundle.putParcelable("createdAt", contract.getCreatedAt());
                        bundle.putDouble("totalPayment", contract.getTotalPayment());
                        bundle.putString("status", contract.getState().toString());
                        detailsFragment.setArguments(bundle);

                        fragmentActivity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.customerFragmentContainer, detailsFragment)
                                .addToBackStack(null)
                                .commit();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("ContractAdapter", "Error fetching user data: " + e.getMessage());
                    holder.itemView.setOnClickListener(v -> {
                        FragmentActivity fragmentActivity = (FragmentActivity) context;
                        ViewContractDetailsFragment detailsFragment = new ViewContractDetailsFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("contractId", contract.getId());
                        bundle.putString("eventId", contract.getEventId());
                        bundle.putString("fullName", "N/A");
                        bundle.putString("email", "N/A");
                        bundle.putString("carId", contract.getCarId());
                        bundle.putParcelable("startDate", contract.getStartDate());
                        bundle.putParcelable("endDate", contract.getEndDate());
                        bundle.putParcelable("createdAt", contract.getCreatedAt());
                        bundle.putDouble("totalPayment", contract.getTotalPayment());
                        bundle.putString("status", contract.getState().toString());
                        detailsFragment.setArguments(bundle);

                        fragmentActivity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.customerFragmentContainer, detailsFragment)
                                .addToBackStack(null)
                                .commit();
                    });
                });
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
        TextView contractIdTextView, carIdTextView, totalPaymentTextView, statusTextView, startDateTextView, endDateTextView;

        ContractViewHolder(View itemView) {
            super(itemView);
            contractIdTextView = itemView.findViewById(R.id.contractIdTextView);
            carIdTextView = itemView.findViewById(R.id.carIdTextView);
            totalPaymentTextView = itemView.findViewById(R.id.totalPaymentTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            startDateTextView = itemView.findViewById(R.id.startDateTextView);
            endDateTextView = itemView.findViewById(R.id.endDateTextView);
        }
    }
}