package com.example.gearup.uiactivities.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.gearup.R;
import com.example.gearup.adapters.CarAdapter;
import com.example.gearup.models.Car;
import com.example.gearup.states.car.CarAvailabilityState;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;

public class ViewCarsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextInputEditText searchBar;
    private CarAdapter carAdapter;
    private ArrayList<Car> carList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final String PREFS_NAME = "CarRentalAppPrefs";
    private static final String ROLE_KEY = "user_role";
    private ListenerRegistration carListener;
    private TextView noCarsTextView;
    private View progressBar;

    public ViewCarsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_cars, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCars);
        searchBar = view.findViewById(R.id.searchBar);
        noCarsTextView = view.findViewById(R.id.noCarsTextView);
        progressBar = view.findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        carList = new ArrayList<>();
        carAdapter = new CarAdapter(getActivity(), carList, isAdminOrManager());
        recyclerView.setAdapter(carAdapter);
        searchBar = view.findViewById(R.id.searchBar);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCars(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });

        loadCars();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (carListener != null) {
            carListener.remove();
            carListener = null;
        }
    }

    private void filterCars(String searchText) {
        ArrayList<Car> filteredCars = new ArrayList<>();
        for (Car car : carList) {
            if (car.getBrand().toLowerCase().contains(searchText) ||
                    car.getModel().toLowerCase().contains(searchText) ||
                    String.valueOf(car.getSeats()).contains(searchText) ||
                    String.valueOf(car.getPrice()).contains(searchText) ||
                    String.valueOf(car.getRating()).contains(searchText)) {
                filteredCars.add(car);
            }
        }
        carAdapter.updateData(filteredCars);
        noCarsTextView.setVisibility(filteredCars.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean isAdminOrManager() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String role = sharedPreferences.getString(ROLE_KEY, "customer");
        return "admin".equals(role) || "manager".equals(role);
    }

    private boolean isAdmin() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String role = sharedPreferences.getString(ROLE_KEY, "customer");
        return "admin".equals(role);
    }

    private void loadCars() {
        progressBar.setVisibility(View.VISIBLE);
        noCarsTextView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        Query query;
        if (isAdminOrManager()) {
            if (isAdmin()) {
                // Admins see all cars
                query = db.collection("Cars")
                        .orderBy("createdAt", Query.Direction.DESCENDING);
                Log.d("ViewCarsFragment", "Loading all cars for admin");
            } else {
                // Managers see only their own cars
                String managerId = mAuth.getCurrentUser().getUid();
                query = db.collection("Cars")
                        .whereEqualTo("managerId", managerId)
                        .orderBy("createdAt", Query.Direction.DESCENDING);
                Log.d("ViewCarsFragment", "Loading cars for managerId: " + managerId);
            }
        } else {
            // Customers see only AVAILABLE or RENTED cars
            query = db.collection("Cars")
                    .whereIn("state", Arrays.asList(
                            CarAvailabilityState.AVAILABLE.toString(),
                            CarAvailabilityState.RENTED.toString()
                    ))
                    .orderBy("createdAt", Query.Direction.DESCENDING);
            Log.d("ViewCarsFragment", "Loading available/rented cars for customer");
        }

        query.get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        carList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Car car = document.toObject(Car.class);
                            car.setId(document.getId());
                            carList.add(car);
                            Log.d("ViewCarsFragment", "Loaded car: id=" + car.getId() + ", managerId=" + car.getManagerId() + ", name=" + car.getBrand() + " " + car.getModel());
                        }

                        if (carList.isEmpty()) {
                            showNoCarsMessage();
                        } else {
                            carAdapter.notifyDataSetChanged();
                        }
                    } else {
                        showErrorAndRetry(task.getException());
                    }
                });
    }

    private void showNoCarsMessage() {
        if (noCarsTextView != null && recyclerView != null) {
            noCarsTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void showErrorAndRetry(Exception e) {
        Toast.makeText(getContext(), "Failed to load cars: " + e.getMessage(), Toast.LENGTH_LONG).show();
        Log.e("ViewCarsFragment", "Error loading cars: " + e.getMessage());
    }
}