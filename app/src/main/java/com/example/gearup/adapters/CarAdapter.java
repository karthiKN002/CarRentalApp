package com.example.gearup.adapters;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gearup.R;
import com.example.gearup.states.car.CarAvailabilityState;
import com.example.gearup.uiactivities.manager.EditCarFragment;
import com.example.gearup.models.Car;
import com.example.gearup.uiactivities.customer.RentCarFragment;

import java.util.ArrayList;
import java.util.List;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private List<Car> carList;
    private Context context;
    private boolean isAdminOrManager; // Renamed for clarity

    public CarAdapter(Context context, List<Car> carList, boolean isAdminOrManager) {
        this.context = context;
        this.carList = carList;
        this.isAdminOrManager = isAdminOrManager;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = carList.get(position);

        holder.carBrandModel.setText(car.getBrand() + " " + car.getModel());
        holder.carPrice.setText("" + car.getPrice());
        holder.carSeats.setText(String.valueOf(car.getSeats()));
        holder.carLocation.setText(car.getLocation());
        holder.carRating.setRating(car.getRating());
        holder.ratingCount.setText(car.getRatingCount() + " ratings");

        if (car.getImages() != null && !car.getImages().isEmpty()) {
            Glide.with(context)
                    .load(car.getImages().get(0))
                    .placeholder(R.drawable.car_placeholder)
                    .into(holder.carImage);
        } else {
            holder.carImage.setImageResource(R.drawable.car_placeholder);
        }

        if (isAdminOrManager) {
            holder.actionButton.setEnabled(true);
            holder.actionButton.setText("Edit");
        } else {
            if (car.getCurrentState() == CarAvailabilityState.AVAILABLE) {
                holder.actionButton.setEnabled(true);
                holder.actionButton.setText("Rent");
            } else {
                holder.actionButton.setEnabled(false);
                holder.actionButton.setText("Unavailable");
            }
        }

        holder.actionButton.setOnClickListener(v -> {
            try {
                FragmentActivity fragmentActivity = (FragmentActivity) context;
                Log.d("CarAdapter", "Action button clicked for car: " + car.getId() + ", isAdminOrManager: " + isAdminOrManager);
                if (isAdminOrManager) {
                    EditCarFragment editCarFragment = new EditCarFragment();
                    Bundle bundle = new Bundle(); // Renamed bundleEdit to bundle for consistency
                    bundle.putString("carId", car.getId());
                    bundle.putString("carBrand", car.getBrand());
                    bundle.putString("carModel", car.getModel());
                    bundle.putString("carLocation", car.getLocation());
                    bundle.putInt("carSeats", car.getSeats());
                    bundle.putDouble("carPrice", car.getPrice());
                    bundle.putStringArrayList("carImageUrls", new ArrayList<>(car.getImages()));
                    bundle.putString("state", car.getCurrentState().toString());
                    bundle.putString("carDescription", car.getDescription());
                    bundle.putFloat("rating", car.getRating());
                    bundle.putInt("ratingCount", car.getRatingCount());

                    editCarFragment.setArguments(bundle);

                    fragmentActivity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.managerFragmentContainer, editCarFragment)
                            .addToBackStack(null)
                            .commit();
                } else {
                    if (car.getCurrentState() == CarAvailabilityState.AVAILABLE) {
                        RentCarFragment rentCarFragment = new RentCarFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("carId", car.getId());
                        bundle.putString("carBrandModel", car.getBrand() + " " + car.getModel());
                        bundle.putString("carLocation", car.getLocation());
                        bundle.putInt("carSeats", car.getSeats());
                        bundle.putDouble("carPrice", car.getPrice());
                        bundle.putFloat("carRating", car.getRating());
                        bundle.putStringArrayList("carImageUrls", new ArrayList<>(car.getImages()));
                        bundle.putString("carDescription", car.getDescription());

                        rentCarFragment.setArguments(bundle);

                        fragmentActivity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.customerFragmentContainer, rentCarFragment)
                                .addToBackStack(null)
                                .commit();
                    } else {
                        Toast.makeText(context, "Car is currently unavailable", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (ClassCastException e) {
                Log.e("CarAdapter", "Context is not a FragmentActivity", e);
                Toast.makeText(context, "Error navigating to fragment", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("CarAdapter", "Error navigating to fragment", e);
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    public void updateData(List<Car> newCarList) {
        this.carList = newCarList;
        notifyDataSetChanged();
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder {
        TextView carBrandModel, carLocation, carPrice, carSeats, ratingCount;
        ImageView carImage;
        RatingBar carRating;
        Button actionButton;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            carBrandModel = itemView.findViewById(R.id.carBrandModel);
            carLocation = itemView.findViewById(R.id.carLocation);
            carPrice = itemView.findViewById(R.id.carPrice);
            carSeats = itemView.findViewById(R.id.carSeats);
            carImage = itemView.findViewById(R.id.carImage);
            carRating = itemView.findViewById(R.id.carRating);
            ratingCount = itemView.findViewById(R.id.ratingCount);
            actionButton = itemView.findViewById(R.id.actionButton);
        }
    }
}