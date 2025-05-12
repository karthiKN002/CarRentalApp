package com.example.gearup.uiactivities.customer;

import android.os.Bundle;
import androidx.annotation.NonNull;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;

public class ViewAvailableCarFragment extends Fragment {
    private RecyclerView recyclerView;
    private CarAdapter carAdapter;
    private ArrayList<Car> carList;
    private FirebaseFirestore db;
    private TextInputEditText searchBar;
    private TextView noCarsTextView;

    public ViewAvailableCarFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        carList = new ArrayList<>();
    }

    public void filterCars(String searchText) {
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_available_car, container, false);

        searchBar = view.findViewById(R.id.searchBar);
        recyclerView = view.findViewById(R.id.recyclerViewCars);
        noCarsTextView = view.findViewById(R.id.noCarsTextView); // Add this ID to layout
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        carAdapter = new CarAdapter(requireContext(), carList, false);
        recyclerView.setAdapter(carAdapter);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().toLowerCase();
                filterCars(searchText);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAvailableCars();
    }

    private void loadAvailableCars() {
        db.collection("Cars")
                .whereEqualTo("state", CarAvailabilityState.AVAILABLE.name())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        carList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            Car car = document.toObject(Car.class);
                            car.setId(document.getId());
                            carList.add(car);
                        }
                        carAdapter.notifyDataSetChanged();
                        noCarsTextView.setVisibility(carList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(getContext(), "Failed to load cars", Toast.LENGTH_SHORT).show();
                        Log.e("ViewAvailableCarFragment", "Error loading cars", task.getException());
                        noCarsTextView.setVisibility(View.VISIBLE);
                    }
                });
    }
}