package com.example.gearup.uiactivities.customer;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.gearup.R;
import com.example.gearup.states.contract.ContractState;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class RentCarDialogFragment extends DialogFragment {

    private static final String TAG = "RentCarDialogFragment";
    private CalendarView calendarView;
    private Button startTimeButton, endTimeButton, nextButton, cancelButton;
    private FirebaseFirestore db;
    private String carId;
    private Set<Long> bookedDates;
    private Date startDate, endDate;
    private String startTime, endTime;
    private OnDateSelectedListener listener;

    public interface OnDateSelectedListener {
        void onDatesSelected(Date startDate, Date endDate, String startTime, String endTime);
    }

    public static RentCarDialogFragment newInstance(String carId) {
        RentCarDialogFragment fragment = new RentCarDialogFragment();
        Bundle args = new Bundle();
        args.putString("carId", carId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        bookedDates = new HashSet<>();
        if (getArguments() != null) {
            carId = getArguments().getString("carId");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.rent_car_dialog, null);

        calendarView = view.findViewById(R.id.calendarView);
        startTimeButton = view.findViewById(R.id.startTimeButton);
        endTimeButton = view.findViewById(R.id.endTimeButton);
        nextButton = view.findViewById(R.id.nextButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setView(view)
                .setTitle("Select Rental Dates");

        loadBookedDates();
        setupCalendar();
        setupButtons();

        return builder.create();
    }

    private void loadBookedDates() {
        db.collection("Contracts")
                .whereEqualTo("carId", carId)
                .whereEqualTo("status", ContractState.ACTIVE.toString())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    bookedDates.clear();
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
                    updateCalendarView();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading booked dates: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to load booked dates", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupCalendar() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long minDate = today.getTimeInMillis();
        calendarView.setMinDate(minDate);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0);
            selectedCalendar.set(Calendar.MILLISECOND, 0);
            long selectedDate = selectedCalendar.getTimeInMillis();

            if (selectedDate < minDate) {
                Toast.makeText(requireContext(), "Cannot select past dates", Toast.LENGTH_SHORT).show();
                return;
            }

            if (startDate == null) {
                startDate = selectedCalendar.getTime();
                startTimeButton.setEnabled(true);
                endTimeButton.setEnabled(false);
                endDate = null;
                endTime = null;
                endTimeButton.setText("Select End Time");
            } else if (endDate == null) {
                if (selectedDate <= startDate.getTime()) {
                    Toast.makeText(requireContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
                    return;
                }
                endDate = selectedCalendar.getTime();
                endTimeButton.setEnabled(true);
            } else {
                startDate = selectedCalendar.getTime();
                endDate = null;
                startTime = null;
                endTime = null;
                startTimeButton.setText("Select Start Time");
                endTimeButton.setText("Select End Time");
                startTimeButton.setEnabled(true);
                endTimeButton.setEnabled(false);
            }
        });
    }

    private void setupButtons() {
        startTimeButton.setOnClickListener(v -> showTimePicker(startTimeButton));
        endTimeButton.setOnClickListener(v -> showTimePicker(endTimeButton));

        nextButton.setOnClickListener(v -> {
            if (startDate == null || endDate == null || startTime == null || endTime == null) {
                Toast.makeText(requireContext(), "Please select valid dates and times", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                Date fullStartDate = sdf.parse(
                        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate) + " " + startTime);
                Date fullEndDate = sdf.parse(
                        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate) + " " + endTime);

                if (fullEndDate.before(fullStartDate) || fullEndDate.equals(fullStartDate)) {
                    Toast.makeText(requireContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Normalize selected dates to midnight for comparison
                Calendar tempCalendar = Calendar.getInstance();
                tempCalendar.setTime(fullStartDate);
                tempCalendar.set(Calendar.HOUR_OF_DAY, 0);
                tempCalendar.set(Calendar.MINUTE, 0);
                tempCalendar.set(Calendar.SECOND, 0);
                tempCalendar.set(Calendar.MILLISECOND, 0);
                long selectedStartMillis = tempCalendar.getTimeInMillis();
                tempCalendar.setTime(fullEndDate);
                tempCalendar.set(Calendar.HOUR_OF_DAY, 0);
                tempCalendar.set(Calendar.MINUTE, 0);
                tempCalendar.set(Calendar.SECOND, 0);
                tempCalendar.set(Calendar.MILLISECOND, 0);
                long selectedEndMillis = tempCalendar.getTimeInMillis();

                // Check for overlapping dates
                tempCalendar.setTimeInMillis(selectedStartMillis);
                while (tempCalendar.getTimeInMillis() <= selectedEndMillis) {
                    if (bookedDates.contains(tempCalendar.getTimeInMillis())) {
                        Toast.makeText(requireContext(), "Sorry, this date is already booked", Toast.LENGTH_LONG).show();
                        endDate = null;
                        endTime = null;
                        endTimeButton.setText("Select End Time");
                        endTimeButton.setEnabled(false);
                        return;
                    }
                    tempCalendar.add(Calendar.DAY_OF_MONTH, 1);
                }

                // Valid dates, proceed
                if (listener != null) {
                    listener.onDatesSelected(fullStartDate, fullEndDate, startTime, endTime);
                    dismiss();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing dates: " + e.getMessage());
                Toast.makeText(requireContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void updateCalendarView() {
        calendarView.setDate(Calendar.getInstance().getTimeInMillis());
        // Note: Native CalendarView doesn't support highlighting booked dates.
    }

    private void showTimePicker(Button button) {
        if ((button == startTimeButton && startDate == null) || (button == endTimeButton && endDate == null)) {
            Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.getTime());
                    if (button == startTimeButton) {
                        startTime = time;
                        startTimeButton.setText("Start At: " + time);
                    } else {
                        endTime = time;
                        endTimeButton.setText("End At: " + time);
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        timePickerDialog.show();
    }
}