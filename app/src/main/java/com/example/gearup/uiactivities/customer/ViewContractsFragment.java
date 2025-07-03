package com.example.gearup.uiactivities.customer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.gearup.R;
import com.example.gearup.adapters.ContractAdapter;
import com.example.gearup.models.Contract;
import com.example.gearup.states.contract.ContractState;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ViewContractsFragment extends Fragment {

    private RecyclerView recyclerViewContracts;
    private ContractAdapter contractAdapter;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ArrayList<Contract> contractList;
    private static final String PREFS_NAME = "CarRentalAppPrefs";
    private static final String ROLE_KEY = "user_role";
    private ListenerRegistration contractListener;
    private TextView noContractsTextView;

    public ViewContractsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_contract, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        contractList = new ArrayList<>();

        recyclerViewContracts = view.findViewById(R.id.recyclerViewContracts);
        recyclerViewContracts.setLayoutManager(new LinearLayoutManager(getContext()));
        contractAdapter = new ContractAdapter(getActivity(), contractList);
        recyclerViewContracts.setAdapter(contractAdapter);

        noContractsTextView = view.findViewById(R.id.noContractsTextView);

        loadContractsBasedOnRole();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (contractListener != null) {
            contractListener.remove();
            contractListener = null;
        }
    }

    private void loadContractsBasedOnRole() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String role = sharedPreferences.getString(ROLE_KEY, "customer");
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e("ViewContractsFragment", "Error: User ID is null.");
            Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("admin".equals(role)) {
            loadAllContracts();
        } else if ("manager".equals(role)) {
            loadManagerContracts(userId);
        } else if ("customer".equals(role)) {
            loadCustomerContracts(userId);
        } else {
            Log.e("ViewContractsFragment", "Error: Role not recognized.");
            Toast.makeText(getContext(), "Error: Invalid role", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAllContracts() {
        if (contractListener != null) {
            contractListener.remove();
        }

        contractListener = db.collection("Contracts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("ViewContractsFragment", "Error loading contracts: " + e.getMessage());
                        Toast.makeText(getContext(), "Error loading contracts", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot != null) {
                        contractList.clear();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            Contract contract = document.toObject(Contract.class);
                            contract.setId(document.getId());
                            contractList.add(contract);
                        }
                        updateUI();
                    }
                });
    }

    private void loadCustomerContracts(String userId) {
        if (contractListener != null) {
            contractListener.remove();
        }

        contractListener = db.collection("Contracts")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("ViewContractsFragment", "Error loading user contracts: " + e.getMessage());
                        Toast.makeText(getContext(), "Error loading user contracts", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot != null) {
                        contractList.clear();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            Contract contract = document.toObject(Contract.class);
                            contract.setId(document.getId());
                            // Check if contract is expired
                            if (contract.getState() == ContractState.ACTIVE && contract.getEndDate() != null && isContractExpired(contract.getEndDate())) {
                                updateContractStatusToCompleted(contract.getId());
                            } else {
                                contractList.add(contract);
                            }
                        }
                        updateUI();
                    }
                });
    }

    private void loadManagerContracts(String managerId) {
        if (contractListener != null) {
            contractListener.remove();
        }

        contractListener = db.collection("Contracts")
                .whereEqualTo("managerId", managerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("ViewContractsFragment", "Error loading manager contracts: " + e.getMessage());
                        Toast.makeText(getContext(), "Error loading manager contracts", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot != null) {
                        contractList.clear();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            Contract contract = document.toObject(Contract.class);
                            contract.setId(document.getId());
                            contractList.add(contract);
                        }
                        updateUI();
                    }
                });
    }

    private boolean isContractExpired(Timestamp endDate) {
        Date currentDate = new Date();
        return endDate.toDate().before(currentDate);
    }

    private void updateContractStatusToCompleted(String contractId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", ContractState.COMPLETED.toString());
        updates.put("updateDate", FieldValue.serverTimestamp());

        db.collection("Contracts").document(contractId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ViewContractsFragment", "Contract " + contractId + " updated to COMPLETED");
                    // Re-fetch the contract to update the list with the new status
                    db.collection("Contracts").document(contractId).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                Contract updatedContract = documentSnapshot.toObject(Contract.class);
                                if (updatedContract != null) {
                                    updatedContract.setId(contractId);
                                    int index = contractList.indexOf(contractList.stream()
                                            .filter(c -> c.getId().equals(contractId))
                                            .findFirst().orElse(null));
                                    if (index >= 0) {
                                        contractList.set(index, updatedContract);
                                    } else {
                                        contractList.add(updatedContract);
                                    }
                                    updateUI();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewContractsFragment", "Failed to update contract: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to update contract status", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        if (contractList.isEmpty()) {
            recyclerViewContracts.setVisibility(View.GONE);
            noContractsTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerViewContracts.setVisibility(View.VISIBLE);
            noContractsTextView.setVisibility(View.GONE);
            contractAdapter.notifyDataSetChanged();
        }
    }
}