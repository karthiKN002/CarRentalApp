package com.example.gearup.uiactivities.customer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gearup.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ViewContractDetailsFragment extends Fragment {

    private TextView carNameTextView, customerNameTextView, textViewManagerName, startDateTextView, endDateTextView,
            userEmailTextView, totalPaymentTextView, statusTextView, contractIdTextView, createdAtTextView;
    private String contractId, carId, userId, managerId;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_contract_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize TextViews
        carNameTextView = view.findViewById(R.id.textViewCarName);
        customerNameTextView = view.findViewById(R.id.customerNameTextView);
        textViewManagerName = view.findViewById(R.id.textViewManagerName);
        startDateTextView = view.findViewById(R.id.textViewStartDate);
        endDateTextView = view.findViewById(R.id.textViewEndDate);
        userEmailTextView = view.findViewById(R.id.textViewUserEmail);
        totalPaymentTextView = view.findViewById(R.id.textViewTotalPayment);
        statusTextView = view.findViewById(R.id.textViewStatus);
        contractIdTextView = view.findViewById(R.id.contractIdTextView);
        createdAtTextView = view.findViewById(R.id.textViewCreatedAt);

        // Retrieve contract data from arguments
        Bundle bundle = getArguments();
        if (bundle != null) {
            contractId = bundle.getString("contractId", "N/A");
            carId = bundle.getString("carId", "N/A");
            userId = bundle.getString("userId", "N/A");
            managerId = bundle.getString("managerId", "N/A");
            String userFullName = bundle.getString("fullName", "N/A");
            String userEmail = bundle.getString("email", "N/A");
            String carName = bundle.getString("carName", "N/A");
            Timestamp startDate = bundle.getParcelable("startDate");
            Timestamp endDate = bundle.getParcelable("endDate");
            Timestamp createdAt = bundle.getParcelable("createdAt");
            double totalPayment = bundle.getDouble("totalPayment", 0.0);
            String status = bundle.getString("status", "N/A");

            // Log bundle data for debugging
            Log.d("ViewContractDetails", "Bundle data: contractId=" + contractId + ", carId=" + carId +
                    ", userId=" + userId + ", managerId=" + managerId + ", carName=" + carName +
                    ", fullName=" + userFullName + ", email=" + userEmail + ", status=" + status);

            // Set initial values
            contractIdTextView.setText("Contract ID: " + contractId);
            startDateTextView.setText("Start Date: " + formatTimestamp(startDate));
            endDateTextView.setText("End Date: " + formatTimestamp(endDate));
            userEmailTextView.setText("Email: " + userEmail);
            totalPaymentTextView.setText(String.format(Locale.getDefault(), "Total Payment: ₹%.2f", totalPayment));
            statusTextView.setText("Status: " + status);
            createdAtTextView.setText("Created At: " + formatTimestamp(createdAt));
            customerNameTextView.setText("Customer Name: " + userFullName);

            // Fetch carName if "N/A"
            if ("N/A".equals(carName)) {
                db.collection("Contracts").document(contractId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String contractCarName = documentSnapshot.getString("carName");
                                if (contractCarName != null && !contractCarName.isEmpty()) {
                                    carNameTextView.setText("Car Name: " + contractCarName);
                                    Log.d("ViewContractDetails", "Fetched carName from Contracts: " + contractCarName);
                                } else {
                                    db.collection("Cars").document(carId).get()
                                            .addOnSuccessListener(carSnapshot -> {
                                                if (carSnapshot.exists()) {
                                                    Log.d("ViewContractDetails", "Car document fields: " + carSnapshot.getData());
                                                    String fetchedCarName = carSnapshot.getString("name");
                                                    if (fetchedCarName == null) fetchedCarName = carSnapshot.getString("model");
                                                    if (fetchedCarName == null) fetchedCarName = carSnapshot.getString("brandModel");
                                                    if (fetchedCarName == null) fetchedCarName = "N/A";
                                                    carNameTextView.setText("Car Name: " + fetchedCarName);
                                                    Log.d("ViewContractDetails", "Fetched carName from Cars: " + fetchedCarName);
                                                } else {
                                                    carNameTextView.setText("Car Name: N/A");
                                                    Log.e("ViewContractDetails", "Car document does not exist for carId: " + carId);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                carNameTextView.setText("Car Name: N/A");
                                                Log.e("ViewContractDetails", "Error fetching car data: " + e.getMessage());
                                            });
                                }
                            } else {
                                carNameTextView.setText("Car Name: N/A");
                                Log.e("ViewContractDetails", "Contract document does not exist: " + contractId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            carNameTextView.setText("Car Name: N/A");
                            Log.e("ViewContractDetails", "Error fetching contract: " + e.getMessage());
                        });
            } else {
                carNameTextView.setText("Car Name: " + carName);
            }

            // Fetch manager name from users collection
            if (managerId != null && !managerId.isEmpty() && !"N/A".equals(managerId)) {
                Log.d("ViewContractDetails", "Querying users collection for managerId: " + managerId);
                db.collection("users").document(managerId).get()
                        .addOnSuccessListener(userSnapshot -> {
                            if (userSnapshot.exists()) {
                                Log.d("ViewContractDetails", "Manager document data: " + userSnapshot.getData());
                                String managerName = userSnapshot.getString("fullName");
                                String userType = userSnapshot.getString("userType");
                                if (managerName != null && "manager".equals(userType)) {
                                    textViewManagerName.setText("Manager Name: " + managerName);
                                    Log.d("ViewContractDetails", "Fetched manager name: " + managerName + ", userType: " + userType);
                                } else {
                                    textViewManagerName.setText("Manager Name: N/A");
                                    Log.e("ViewContractDetails", "Manager document invalid: fullName=" + managerName + ", userType=" + userType);
                                }
                            } else {
                                textViewManagerName.setText("Manager Name: N/A");
                                Log.e("ViewContractDetails", "Manager document does not exist for managerId: " + managerId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            textViewManagerName.setText("Manager Name: N/A");
                            Log.e("ViewContractDetails", "Error fetching manager data: " + e.getMessage());
                        });
            } else {
                textViewManagerName.setText("Manager Name: N/A");
                Log.e("ViewContractDetails", "Invalid managerId: " + managerId);
            }
        } else {
            Toast.makeText(getContext(), "Failed to load contract details", Toast.LENGTH_SHORT).show();
            carNameTextView.setText("Car Name: N/A");
            customerNameTextView.setText("Customer Name: N/A");
            textViewManagerName.setText("Manager Name: N/A");
            startDateTextView.setText("Start Date: N/A");
            endDateTextView.setText("End Date: N/A");
            userEmailTextView.setText("Email: N/A");
            totalPaymentTextView.setText("Total Payment: ₹0.00");
            statusTextView.setText("Status: N/A");
            contractIdTextView.setText("Contract ID: N/A");
            createdAtTextView.setText("Created At: N/A");
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(date);
    }
}