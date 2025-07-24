package com.example.gearup.uiactivities.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gearup.R;

import com.example.gearup.uiactivities.admin.CarPendingApproval;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CarDetailsActivity extends AppCompatActivity {
    private TextView textViewCar, textViewLocation, textViewPrice, textViewDescription;
    private RecyclerView recyclerViewImages;
    private Button buttonViewFc, buttonViewRc, buttonViewInsurance, buttonViewPuc, buttonApprove, buttonReject;
    private ImageAdapter imageAdapter;
    private FirebaseFirestore db;
    private String carId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        db = FirebaseFirestore.getInstance();
        textViewCar = findViewById(R.id.textViewCar);
        textViewLocation = findViewById(R.id.textViewLocation);
        textViewPrice = findViewById(R.id.textViewPrice);
        textViewDescription = findViewById(R.id.textViewDescription);
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        buttonViewFc = findViewById(R.id.buttonViewFc);
        buttonViewRc = findViewById(R.id.buttonViewRc);
        buttonViewInsurance = findViewById(R.id.buttonViewInsurance);
        buttonViewPuc = findViewById(R.id.buttonViewPuc);
        buttonApprove = findViewById(R.id.buttonApprove);
        buttonReject = findViewById(R.id.buttonReject);

        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter();
        recyclerViewImages.setAdapter(imageAdapter);

        carId = getIntent().getStringExtra("carId");
        if (carId != null) {
            loadCarDetails(carId);
        }

        buttonApprove.setOnClickListener(v -> approveCar());
        buttonReject.setOnClickListener(v -> rejectCar());
    }

    private void loadCarDetails(String carId) {
        db.collection("pending_cars").document(carId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        CarPendingApproval car = document.toObject(CarPendingApproval.class);
                        if (car != null) {
                            textViewCar.setText(car.getBrand() + " " + car.getModel());
                            textViewLocation.setText(car.getLocation());
                            textViewPrice.setText("₹" + car.getPrice() + "/day");
                            textViewDescription.setText(car.getDescription());
                            imageAdapter.setImages(car.getImages());

                            setupDocumentButton(buttonViewFc, car.getFcDocument());
                            setupDocumentButton(buttonViewRc, car.getRcDocument());
                            setupDocumentButton(buttonViewInsurance, car.getInsuranceDocument());
                            setupDocumentButton(buttonViewPuc, car.getPucDocument());
                        }
                    } else {
                        Toast.makeText(this, "Car not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load car details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupDocumentButton(Button button, String url) {
        if (url != null && !url.isEmpty()) {
            button.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            button.setEnabled(false);
            button.setText("No Document");
        }
    }

    private void approveCar() {
        db.collection("pending_cars").document(carId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Map<String, Object> carData = new HashMap<>(document.getData());
                        carData.put("state", "AVAILABLE"); // Update state
                        carData.put("approvedAt", FieldValue.serverTimestamp()); // Add approval timestamp
                        db.collection("Cars").document(carId).set(carData)
                                .addOnSuccessListener(aVoid -> {
                                    db.collection("pending_cars").document(carId).delete()
                                            .addOnSuccessListener(aVoid2 -> {
                                                Toast.makeText(this, "Car approved", Toast.LENGTH_SHORT).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Failed to delete pending car: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to approve car: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Car not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to retrieve car: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectCar() {
        db.collection("pending_cars").document(carId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Car rejected", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to reject car: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private ArrayList<String> images = new ArrayList<>();

        void setImages(ArrayList<String> images) {
            this.images = images != null ? images : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Picasso.get()
                    .load(images.get(position))
                    .placeholder(R.drawable.car_placeholder) // Add placeholder
                    .error(R.drawable.car_placeholder) // Add error image
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }
}