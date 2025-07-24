package com.example.gearup.uiactivities.customer;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.gearup.BuildConfig;
import com.example.gearup.R;
import com.example.gearup.adapters.CarImageAdapter;
import com.example.gearup.states.contract.ContractState;
import com.example.gearup.uiactivities.manager.ChatFragment;
import com.example.gearup.utilities.MapPickerActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CarDetailsFragment extends Fragment {

    private static final String TAG = "CarDetailsFragment";
    private static final int MAP_PICKER_REQUEST_CODE = 101;
    private ViewPager2 carImageSlider;
    private TextView carBrandModelTextView, carDescriptionTextView, carLocationTextView, carSeatsTextView, carPriceTextView;
    private TextView managerNameTextView, rentedCarsTextView, managerContactTextView;
    private RatingBar carRatingBar;
    private Button rentButton;
    private String carId, carName;
    private ArrayList<String> carImageUrls;
    private double pricePerDay;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Date startDate;
    private Date endDate;
    private String pickupLocation;
    private double totalPayment;
    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;
    private OkHttpClient client = new OkHttpClient();
    private PaymentSheet.CustomerConfiguration customerConfig;
    private static final String BACKEND_URL = "http://192.168.0.107:4242";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        PaymentConfiguration.init(requireContext(), BuildConfig.STRIPE_PUBLISHABLE_KEY);
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_car_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        carImageSlider = view.findViewById(R.id.carImageSlider);
        carBrandModelTextView = view.findViewById(R.id.carBrandModel);
        carDescriptionTextView = view.findViewById(R.id.carDescription);
        carLocationTextView = view.findViewById(R.id.carLocation);
        carSeatsTextView = view.findViewById(R.id.carSeats);
        carPriceTextView = view.findViewById(R.id.carPrice);
        carRatingBar = view.findViewById(R.id.carRating);
        managerNameTextView = view.findViewById(R.id.managerNameTextView);
        rentedCarsTextView = view.findViewById(R.id.rentedCarsTextView);
        managerContactTextView = view.findViewById(R.id.managerContactTextView);
        rentButton = view.findViewById(R.id.rentButton);

        Button chatButton = view.findViewById(R.id.chatButton);

        Bundle bundle = getArguments();
        if (bundle != null) {
            carId = bundle.getString("carId");
            carName = bundle.getString("carBrandModel", "N/A");
            String carDescription = bundle.getString("carDescription");
            String carLocation = bundle.getString("carLocation");
            int carSeats = bundle.getInt("carSeats");
            pricePerDay = bundle.getDouble("carPrice");
            float carRating = bundle.getFloat("carRating");
            carImageUrls = bundle.getStringArrayList("carImageUrls");
            String managerId = bundle.getString("managerId");

            // Log bundle data for debugging
            Log.d(TAG, "Bundle data: carId=" + carId + ", managerId=" + managerId + ", carName=" + carName);

            carBrandModelTextView.setText(carName);
            carDescriptionTextView.setText(carDescription);
            carLocationTextView.setText("Location: " + carLocation);
            carSeatsTextView.setText("Seats: " + carSeats);
            carPriceTextView.setText(String.format(Locale.US, "Price per day: ₹%.2f", pricePerDay));
            carRatingBar.setRating(carRating);

            if (carImageUrls != null && !carImageUrls.isEmpty()) {
                CarImageAdapter adapter = new CarImageAdapter(getContext(), carImageUrls);
                carImageSlider.setAdapter(adapter);
            } else {
                ImageView carImage = view.findViewById(R.id.carImage);
                carImage.setImageResource(R.drawable.car_placeholder);
            }

            // Fetch manager details
            if (managerId != null && !managerId.isEmpty() && !"N/A".equals(managerId)) {
                Log.d(TAG, "Querying users collection for managerId: " + managerId);
                FirebaseFirestore.getInstance().collection("users").document(managerId)
                        .get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                Log.d(TAG, "Manager document data: " + document.getData());
                                String managerName = document.getString("fullName");
                                String contactNumber = document.getString("phone");
                                String userType = document.getString("userType");
                                if (managerName != null && "manager".equals(userType)) {
                                    managerNameTextView.setText("Manager: " + managerName);
                                    managerContactTextView.setText("Contact: " + (contactNumber != null ? contactNumber : "N/A"));
                                    Log.d(TAG, "Fetched manager fullName: " + managerName + ", phone: " + contactNumber + ", userType: " + userType);
                                } else {
                                    managerNameTextView.setText("Manager: N/A");
                                    managerContactTextView.setText("Contact: N/A");
                                    Log.e(TAG, "Manager document invalid: fullName=" + managerName + ", userType=" + userType);
                                }
                            } else {
                                managerNameTextView.setText("Manager: N/A");
                                managerContactTextView.setText("Contact: N/A");
                                Log.e(TAG, "Manager document does not exist for managerId: " + managerId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            managerNameTextView.setText("Manager: N/A");
                            managerContactTextView.setText("Contact: N/A");
                            Log.e(TAG, "Error fetching manager data: " + e.getMessage());
                        });
            } else {
                managerNameTextView.setText("Manager: N/A");
                managerContactTextView.setText("Contact: N/A");
                Log.e(TAG, "Invalid managerId: " + managerId);
            }

            rentButton.setOnClickListener(v -> showDatePickerDialog(bundle));

            if (chatButton != null) {
                chatButton.setOnClickListener(v -> {
                    if (managerId != null && !managerId.isEmpty() && !"N/A".equals(managerId)) {
                        ChatFragment chatFragment = new ChatFragment();
                        Bundle chatBundle = new Bundle();
                        chatBundle.putString("receiverId", managerId);
                        chatBundle.putString("carId", carId);
                        chatFragment.setArguments(chatBundle);
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.customerFragmentContainer, chatFragment)
                                .addToBackStack(null)
                                .commit();
                    } else {
                        Toast.makeText(requireContext(), "Cannot start chat: No manager assigned to this car", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Chat with Manager button not found in layout");
                Toast.makeText(requireContext(), "Chat button not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDatePickerDialog(Bundle bundle) {
        RentCarDialogFragment dialogFragment = RentCarDialogFragment.newInstance(carId);
        dialogFragment.setOnDateSelectedListener((startDate, endDate, startTime, endTime) -> {
            this.startDate = startDate;
            this.endDate = endDate;
            try {
                String startDateTimeStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate) + " " + startTime;
                String endDateTimeStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate) + " " + endTime;
                this.startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(startDateTimeStr);
                this.endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(endDateTimeStr);

                if (endDate.before(startDate) || endDate.equals(startDate)) {
                    Toast.makeText(requireContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
                    return;
                }

                long days = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24) + 1; // Include end date
                if (days <= 0) {
                    Toast.makeText(requireContext(), "Rental period must be at least one day", Toast.LENGTH_SHORT).show();
                    return;
                }
                totalPayment = pricePerDay * days;
                showMapPicker(bundle);
            } catch (ParseException e) {
                Toast.makeText(requireContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
            }
        });
        dialogFragment.show(getParentFragmentManager(), "RentCarDialog");
    }

    private void showMapPicker(Bundle bundle) {
        Intent intent = new Intent(requireContext(), MapPickerActivity.class);
        startActivityForResult(intent, MAP_PICKER_REQUEST_CODE);
        this.bundle = bundle;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            double latitude = data.getDoubleExtra("latitude", 0.0);
            double longitude = data.getDoubleExtra("longitude", 0.0);
            String address = data.getStringExtra("address");
            if (latitude != 0.0 && longitude != 0.0 && address != null) {
                pickupLocation = address + ", " + latitude + "," + longitude;
                showConfirmationDialog(bundle);
            } else {
                Toast.makeText(requireContext(), "Invalid location selected", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Location selection failed", Toast.LENGTH_SHORT).show();
        }
    }

    private Bundle bundle;

    private void showConfirmationDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.confirmation_dialog, null);
        builder.setView(dialogView);

        TextView carNameTextView = dialogView.findViewById(R.id.confirmCarName);
        TextView datesTextView = dialogView.findViewById(R.id.confirmDates);
        TextView totalTextView = dialogView.findViewById(R.id.confirmTotal);
        TextView locationTextView = dialogView.findViewById(R.id.confirmLocation);
        Button payButton = dialogView.findViewById(R.id.payButton);

        carNameTextView.setText("Car: " + carName);
        datesTextView.setText("From: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(startDate) + "\nTo: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(endDate));
        totalTextView.setText(String.format(Locale.US, "Total: ₹%.2f", totalPayment));
        locationTextView.setText("Pickup Location: " + pickupLocation);

        AlertDialog dialog = builder.create();

        payButton.setOnClickListener(v -> {
            dialog.dismiss();
            fetchPaymentSheetConfig();
        });

        dialog.show();
    }

    private void fetchPaymentSheetConfig() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String email = currentUser.getEmail();
        String name = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Customer";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("name", name);
            jsonBody.put("totalAmount", (long) (totalPayment * 100)); // Convert to paisa
            jsonBody.put("currency", "inr");
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            Toast.makeText(requireContext(), "Error preparing payment", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(BACKEND_URL + "/payment-sheet")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network failure: " + e.getMessage());
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Network error: Unable to fetch payment config", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Server error: " + response.code());
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Server error: Unable to fetch payment config", Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONObject responseJson = new JSONObject(responseBody);
                    paymentIntentClientSecret = responseJson.getString("paymentIntent");
                    String customerId = responseJson.getString("customer");
                    String ephemeralKey = responseJson.getString("ephemeralKey");

                    customerConfig = new PaymentSheet.CustomerConfiguration(customerId, ephemeralKey);
                    requireActivity().runOnUiThread(() -> presentPaymentSheet());
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parse error: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error parsing payment config", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void presentPaymentSheet() {
        if (customerConfig != null && paymentIntentClientSecret != null) {
            paymentSheet.presentWithPaymentIntent(
                    paymentIntentClientSecret,
                    new PaymentSheet.Configuration(
                            "CarRentalApp",
                            customerConfig,
                            new PaymentSheet.GooglePayConfiguration(
                                    PaymentSheet.GooglePayConfiguration.Environment.Test,
                                    "IN",
                                    "INR"
                            )
                    )
            );
        } else {
            Toast.makeText(requireContext(), "Payment setup incomplete", Toast.LENGTH_SHORT).show();
        }
    }

    private void onPaymentSheetResult(PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Toast.makeText(requireContext(), "Payment Successful!", Toast.LENGTH_SHORT).show();
            saveContractToFirestore();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(requireContext(), "Payment Canceled", Toast.LENGTH_SHORT).show();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            PaymentSheetResult.Failed failedResult = (PaymentSheetResult.Failed) paymentSheetResult;
            Toast.makeText(requireContext(), "Payment Failed: " + failedResult.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveContractToFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show());
            return;
        }

        String userId = currentUser.getUid();
        db.collection("Contracts")
                .whereEqualTo("carId", carId)
                .whereEqualTo("status", ContractState.ACTIVE.toString())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<Long> bookedDates = new HashSet<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        Timestamp start = document.getTimestamp("startDate");
                        Timestamp end = document.getTimestamp("endDate");
                        if (start != null && end != null) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(start.toDate());
                            calendar.set(Calendar.HOUR_OF_DAY, 0);
                            calendar.set(Calendar.MINUTE, 0);
                            calendar.set(Calendar.SECOND, 0);
                            calendar.set(Calendar.MILLISECOND, 0);
                            long startMillis = calendar.getTimeInMillis();
                            calendar.setTime(end.toDate());
                            calendar.set(Calendar.HOUR_OF_DAY, 0);
                            calendar.set(Calendar.MINUTE, 0);
                            calendar.set(Calendar.SECOND, 0);
                            calendar.set(Calendar.MILLISECOND, 0);
                            long endMillis = calendar.getTimeInMillis();
                            calendar.setTimeInMillis(startMillis);
                            while (calendar.getTimeInMillis() <= endMillis) {
                                bookedDates.add(calendar.getTimeInMillis());
                                calendar.add(Calendar.DAY_OF_MONTH, 1);
                            }
                        }
                    }

                    // Normalize selected dates to midnight
                    Calendar tempCalendar = Calendar.getInstance();
                    tempCalendar.setTime(startDate);
                    tempCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    tempCalendar.set(Calendar.MINUTE, 0);
                    tempCalendar.set(Calendar.SECOND, 0);
                    tempCalendar.set(Calendar.MILLISECOND, 0);
                    long selectedStartMillis = tempCalendar.getTimeInMillis();
                    tempCalendar.setTime(endDate);
                    tempCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    tempCalendar.set(Calendar.MINUTE, 0);
                    tempCalendar.set(Calendar.SECOND, 0);
                    tempCalendar.set(Calendar.MILLISECOND, 0);
                    long selectedEndMillis = tempCalendar.getTimeInMillis();

                    // Check for overlapping dates
                    tempCalendar.setTimeInMillis(selectedStartMillis);
                    while (tempCalendar.getTimeInMillis() <= selectedEndMillis) {
                        if (bookedDates.contains(tempCalendar.getTimeInMillis())) {
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Sorry, choose another date, this date is already booked", Toast.LENGTH_LONG).show());
                            return;
                        }
                        tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    db.collection("Cars").document(carId).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String managerId = documentSnapshot.getString("managerId");
                                    if (managerId == null || managerId.isEmpty()) {
                                        managerId = "N/A";
                                    }

                                    Map<String, Object> contractData = new HashMap<>();
                                    contractData.put("userId", userId);
                                    contractData.put("carId", carId);
                                    contractData.put("carName", carName);
                                    contractData.put("managerId", managerId);
                                    contractData.put("createdAt", Timestamp.now());
                                    contractData.put("startDate", new Timestamp(startDate));
                                    contractData.put("endDate", new Timestamp(endDate));
                                    contractData.put("updateDate", null);
                                    contractData.put("totalPayment", totalPayment);
                                    contractData.put("status", ContractState.ACTIVE.toString());
                                    contractData.put("pickupLocation", pickupLocation);
                                    contractData.put("rated", false);

                                    String finalManagerId = managerId;
                                    db.collection("Contracts").add(contractData)
                                            .addOnSuccessListener(documentReference -> {
                                                requireActivity().runOnUiThread(() -> {
                                                    Toast.makeText(requireContext(), "Contract saved successfully.", Toast.LENGTH_SHORT).show();
                                                    sendNotificationToManager(finalManagerId, carId);
                                                    navigateToViewContractsFragment();
                                                });
                                            })
                                            .addOnFailureListener(e -> requireActivity().runOnUiThread(() ->
                                                    Toast.makeText(requireContext(), "Failed to save contract: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
                                } else {
                                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Car not found", Toast.LENGTH_SHORT).show());
                                }
                            })
                            .addOnFailureListener(e -> requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Failed to fetch car: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
                })
                .addOnFailureListener(e -> requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to check booked dates: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
    }

    private void sendNotificationToManager(String managerId, String carId) {
        if (managerId == null || "N/A".equals(managerId)) {
            Log.w(TAG, "Cannot send notification: Invalid managerId");
            return;
        }
        String messageText = String.format(
                "Your car %s is booked from %s to %s at location",
                carName,
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(startDate),
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(endDate)
        );
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", userId);
        messageData.put("receiverId", managerId);
        messageData.put("message", messageText);
        messageData.put("timestamp", Timestamp.now());
        messageData.put("carId", carId);
        messageData.put("messageType", "BOOKING_NOTIFICATION");
        messageData.put("isRead", false);
        messageData.put("confirmed", false);
        messageData.put("pickupLocation", pickupLocation);

        db.collection("Messages").add(messageData)
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "Booking notification sent to manager: " + managerId);
                    db.collection("users").document(managerId)
                            .get()
                            .addOnSuccessListener(document -> {
                                String token = document.getString("fcmToken");
                                if (token != null && !token.isEmpty()) {
                                    sendFcmNotification(token, "Car Booked", messageText);
                                }
                            });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send booking notification: " + e.getMessage()));
    }

    private void sendFcmNotification(String token, String title, String body) {
        JSONObject json = new JSONObject();
        try {
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);
            json.put("notification", notification);
            json.put("to", token);
            RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .post(requestBody)
                    .addHeader("Authorization", "key=YOUR_FCM_SERVER_KEY")
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to send notification: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Notification send failed: " + response.code());
                    } else {
                        Log.d(TAG, "Notification sent successfully");
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error in notification: " + e.getMessage());
        }
    }

    private void navigateToViewContractsFragment() {
        ViewContractsFragment viewContractsFragment = new ViewContractsFragment();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.customerFragmentContainer, viewContractsFragment)
                .addToBackStack(null)
                .commit();
    }

    private int getContainerId() {
        return R.id.customerFragmentContainer;
    }
}