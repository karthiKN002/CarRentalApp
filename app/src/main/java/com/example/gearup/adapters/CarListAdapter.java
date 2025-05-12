package com.example.gearup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.models.Car;
import com.squareup.picasso.Picasso;
import java.util.List;

public class CarListAdapter extends RecyclerView.Adapter<CarListAdapter.CarViewHolder> {

    private final List<Car> carList;
    private final boolean isManagerMode;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Car car);
        void onEditClick(Car car);
        void onDeleteClick(Car car);
    }

    public CarListAdapter(List<Car> carList, boolean isManagerMode) {
        this.carList = carList;
        this.isManagerMode = isManagerMode;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = carList.get(position);
        holder.bind(car, isManagerMode, listener);
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    public void updateData(List<Car> newCars) {
        carList.clear();
        carList.addAll(newCars);
        notifyDataSetChanged();
    }

    static class CarViewHolder extends RecyclerView.ViewHolder {
        private final ImageView carImage;
        private final TextView carBrandModel;
        private final TextView carLocation;
        private final TextView carSeats;
        private final TextView carPrice;
        private final TextView carStatus;
        private final RatingBar carRating;
        private final TextView ratingCount;
        private final ImageButton editButton;
        private final ImageButton deleteButton;
        private final Button actionButton;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            carImage = itemView.findViewById(R.id.carImage);
            carBrandModel = itemView.findViewById(R.id.carBrandModel);
            carLocation = itemView.findViewById(R.id.carLocation);
            carSeats = itemView.findViewById(R.id.carSeats);
            carPrice = itemView.findViewById(R.id.carPrice);
            carStatus = itemView.findViewById(R.id.carStatus);
            carRating = itemView.findViewById(R.id.carRating);
            ratingCount = itemView.findViewById(R.id.ratingCount);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            actionButton = itemView.findViewById(R.id.actionButton);
        }

        public void bind(Car car, boolean isManagerMode, OnItemClickListener listener) {
            // Load car image
            if (!car.getImages().isEmpty()) {
                Picasso.get().load(car.getImages().get(0)).into(carImage);
            }

            // Set car information
            carBrandModel.setText(car.getBrand() + " " + car.getModel());
            carLocation.setText(car.getLocation());
            carSeats.setText(String.format("%d Seats", car.getSeats()));
            carPrice.setText(String.format("₹%.2f/day", car.getPrice()));

            // Set status
            carStatus.setText(car.isAvailable() ? "AVAILABLE" : "BOOKED");
            carStatus.setBackgroundResource(car.isAvailable() ?
                    R.drawable.bg_status_available : R.drawable.bg_status_booked);

            // Set rating info
            carRating.setRating(car.getRating());
            ratingCount.setText(String.format("(%d)", car.getRatingCount()));

            // Handle manager/user mode
            if (isManagerMode) {
                actionButton.setVisibility(View.GONE);
                editButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);

                editButton.setOnClickListener(v -> {
                    if (listener != null) listener.onEditClick(car);
                });

                deleteButton.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(car);
                });
            } else {
                editButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText(car.isAvailable() ? "Book Now" : "Unavailable");
                actionButton.setEnabled(car.isAvailable());

                actionButton.setOnClickListener(v -> {
                    if (listener != null && car.isAvailable()) {
                        listener.onItemClick(car);
                    }
                });
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(car);
            });
        }
    }
}