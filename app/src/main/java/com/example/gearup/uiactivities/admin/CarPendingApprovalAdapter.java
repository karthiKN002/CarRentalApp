package com.example.gearup.uiactivities.admin;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gearup.R;

import java.util.ArrayList;
import java.util.List;

public class CarPendingApprovalAdapter extends RecyclerView.Adapter<CarPendingApprovalAdapter.ViewHolder> {
    private static final String TAG = "CarPendingApprovalAdapter";
    private List<CarPendingApproval> carList;
    private final OnCarApproveClickListener listener;

    public CarPendingApprovalAdapter(List<CarPendingApproval> carList, OnCarApproveClickListener listener) {
        this.carList = new ArrayList<>(carList);
        this.listener = listener;
        Log.d(TAG, "Adapter initialized with " + carList.size() + " cars");
    }

    public void updateList(List<CarPendingApproval> newCarList) {
        Log.d(TAG, "Updating list with " + newCarList.size() + " cars");
        this.carList = new ArrayList<>(newCarList);
        notifyDataSetChanged();
        Log.d(TAG, "Adapter item count after update: " + getItemCount());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_car_approval, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CarPendingApproval car = carList.get(position);
        Log.d(TAG, "Binding car at position " + position + ": " + car.getBrand() + " " + car.getModel());
        holder.textViewCar.setText(car.getBrand() + " " + car.getModel());
        holder.textViewLocation.setText(car.getLocation());
        holder.textViewPrice.setText("₹" + car.getPrice() + "/day");
        holder.buttonViewDetails.setOnClickListener(v -> {
            Log.d(TAG, "View details clicked for car ID: " + car.getId());
            listener.onViewDetailsClick(car);
        });
    }

    @Override
    public int getItemCount() {
        int size = carList.size();
        Log.d(TAG, "getItemCount: " + size);
        return size;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCar;
        TextView textViewLocation;
        TextView textViewPrice;
        Button buttonViewDetails;

        ViewHolder(View itemView) {
            super(itemView);
            textViewCar = itemView.findViewById(R.id.textViewCar);
            textViewLocation = itemView.findViewById(R.id.textViewLocation);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            buttonViewDetails = itemView.findViewById(R.id.buttonViewDetails);
        }
    }
}