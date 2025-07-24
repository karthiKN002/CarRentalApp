package com.example.gearup.uiactivities.customer;

import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.adapters.ContractAdapter;
import com.example.gearup.models.Contract;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class ViewContractsFragment extends Fragment {

    private RecyclerView recyclerViewContracts;
    private TextView noContractsTextView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ContractAdapter contractAdapter;
    private List<Contract> contractList;
    private static final String PREFS_NAME = "CarRentalAppPrefs";
    private static final String ROLE_KEY = "user_role";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        contractList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_contract, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewContracts = view.findViewById(R.id.recyclerViewContracts);
        noContractsTextView = view.findViewById(R.id.noContractsTextView);

        recyclerViewContracts.setLayoutManager(new LinearLayoutManager(getContext()));
        contractAdapter = new ContractAdapter(getContext(), contractList);
        recyclerViewContracts.setAdapter(contractAdapter);

        loadContracts();
    }

    private boolean isAdminOrManager() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String role = sharedPreferences.getString(ROLE_KEY, "customer");
        return "admin".equals(role) || "manager".equals(role);
    }

    private void loadContracts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            noContractsTextView.setVisibility(View.VISIBLE);
            recyclerViewContracts.setVisibility(View.GONE);
            return;
        }

        String userId = currentUser.getUid();
        Query query;
        if (isAdminOrManager()) {
            query = db.collection("Contracts")
                    .whereEqualTo("managerId", userId)
                    .orderBy("status", Query.Direction.ASCENDING)
                    .orderBy("createdAt", Query.Direction.DESCENDING);
        } else {
            query = db.collection("Contracts")
                    .whereEqualTo("userId", userId)
                    .orderBy("status", Query.Direction.ASCENDING)
                    .orderBy("createdAt", Query.Direction.DESCENDING);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    contractList.clear();
                    for (DocumentSnapshot document : querySnapshot) {
                        Contract contract = document.toObject(Contract.class);
                        if (contract != null) {
                            contract.setId(document.getId());
                            contractList.add(contract);
                        }
                    }
                    contractAdapter.notifyDataSetChanged();
                    noContractsTextView.setVisibility(contractList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerViewContracts.setVisibility(contractList.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewContractsFragment", "Error fetching contracts: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to load contracts", Toast.LENGTH_SHORT).show();
                    noContractsTextView.setVisibility(View.VISIBLE);
                    recyclerViewContracts.setVisibility(View.GONE);
                });
    }
}