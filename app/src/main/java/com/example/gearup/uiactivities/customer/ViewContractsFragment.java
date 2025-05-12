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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;

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

    public ViewContractsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
            loadAllContracts(); // Admins can see all contracts
        } else if ("manager".equals(role) || "customer".equals(role)) {
            loadUserContracts(userId); // Managers and customers see only their own contracts
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

    private void loadUserContracts(String userId) {
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
                            contractList.add(contract);
                        }
                        updateUI();
                    }
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