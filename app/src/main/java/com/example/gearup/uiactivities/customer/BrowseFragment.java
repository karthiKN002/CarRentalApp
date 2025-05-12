package com.example.gearup.uiactivities.customer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gearup.R;
import com.example.gearup.adapters.CarAdapter;
import com.example.gearup.models.Car;
import com.example.gearup.states.car.CarAvailabilityState;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BrowseFragment extends Fragment {

    private RecyclerView recyclerView;
    private CarAdapter carAdapter;
    private List<Car> carList;
    private List<Car> filteredCarList;
    private FirebaseFirestore db;
    private TextView noCarsTextView;
    private EditText searchEditText;
    private Spinner filterSpinner;
    private Spinner sortSpinner;

    public BrowseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewCars);
        noCarsTextView = view.findViewById(R.id.noCarsTextView);
        searchEditText = view.findViewById(R.id.searchEditText);
        filterSpinner = view.findViewById(R.id.filterSpinner);
        sortSpinner = view.findViewById(R.id.sortSpinner);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        carList = new ArrayList<>();
        filteredCarList = new ArrayList<>();
        carAdapter = new CarAdapter(getContext(), filteredCarList, false);
        recyclerView.setAdapter(carAdapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Setup filter spinner
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.car_filters,
                android.R.layout.simple_spinner_item
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(filterAdapter);

        // Setup sort spinner
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.car_sort_options,
                android.R.layout.simple_spinner_item
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        // Set up listeners
        setupListeners();

        // Load cars
        loadCars();
    }

    private void setupListeners() {
        // Search text listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCars();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter spinner listener
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterCars();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Sort spinner listener
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortCars();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadCars() {
        db.collection("Cars")
                .whereEqualTo("state", CarAvailabilityState.AVAILABLE.toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        carList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Car car = document.toObject(Car.class);
                            car.setId(document.getId());
                            carList.add(car);
                        }
                        filterCars();
                    } else {
                        noCarsTextView.setText("Error loading cars");
                        noCarsTextView.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void filterCars() {
        filteredCarList.clear();

        String searchText = searchEditText.getText().toString().toLowerCase();
        String filter = filterSpinner.getSelectedItem().toString();

        for (Car car : carList) {
            // Apply search filter
            boolean matchesSearch = car.getBrand().toLowerCase().contains(searchText) ||
                    car.getModel().toLowerCase().contains(searchText) ||
                    String.valueOf(car.getPrice()).contains(searchText);

            // Apply category filter
            boolean matchesFilter = filter.equals("All") ||
                    car.getType().equalsIgnoreCase(filter);

            if (matchesSearch && matchesFilter) {
                filteredCarList.add(car);
            }
        }

        sortCars();
        updateUI();
    }

    private void sortCars() {
        String sortOption = sortSpinner.getSelectedItem().toString();

        try {
            switch (sortOption) {
                case "Price: Low to High":
                    Collections.sort(filteredCarList, new Comparator<Car>() {
                        @Override
                        public int compare(Car c1, Car c2) {
                            return Double.compare(c1.getPrice(), c2.getPrice());
                        }
                    });
                    break;

                case "Price: High to Low":
                    Collections.sort(filteredCarList, new Comparator<Car>() {
                        @Override
                        public int compare(Car c1, Car c2) {
                            return Double.compare(c2.getPrice(), c1.getPrice());
                        }
                    });
                    break;

                case "Rating: High to Low":
                    Collections.sort(filteredCarList, new Comparator<Car>() {
                        @Override
                        public int compare(Car c1, Car c2) {
                            float r1 = c1.getRating();
                            float r2 = c2.getRating();
                            return Float.compare(r2, r1); // Descending order
                        }
                    });
                    break;

                case "Newest First":
                    Collections.sort(filteredCarList, new Comparator<Car>() {
                        @Override
                        public int compare(Car c1, Car c2) {
                            Timestamp t1 = c1.getCreatedAt();
                            Timestamp t2 = c2.getCreatedAt();

                            // Handle null cases
                            if (t1 == null && t2 == null) return 0;
                            if (t1 == null) return 1;  // Put nulls last
                            if (t2 == null) return -1; // Put nulls last

                            return t2.compareTo(t1); // Newest first
                        }
                    });
                    break;
            }
            carAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e("BrowseFragment", "Sorting error", e);
            // Fallback to default sorting if error occurs
            Collections.sort(filteredCarList, (c1, c2) -> c1.getBrand().compareTo(c2.getBrand()));
        }
    }

    private void updateUI() {
        if (filteredCarList.isEmpty()) {
            noCarsTextView.setText("No cars available");
            noCarsTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noCarsTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            carAdapter.notifyDataSetChanged();
        }
    }
}