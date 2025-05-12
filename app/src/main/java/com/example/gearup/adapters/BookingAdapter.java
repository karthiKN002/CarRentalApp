package com.example.gearup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.models.Booking;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private final List<Booking> bookingList;
    private final boolean isManagerMode;

    public BookingAdapter(List<Booking> bookingList, boolean isManagerMode) {
        this.bookingList = bookingList;
        this.isManagerMode = isManagerMode;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view, isManagerMode);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        holder.bind(booking);
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public void updateData(List<Booking> newBookings) {
        bookingList.clear();
        bookingList.addAll(newBookings);
        notifyDataSetChanged();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        private final TextView bookingId;
        private final TextView carModel;
        private final TextView customerName;
        private final TextView bookingDates;
        private final TextView bookingStatus;
        private final View actionButtons;

        public BookingViewHolder(@NonNull View itemView, boolean isManagerMode) {
            super(itemView);
            bookingId = itemView.findViewById(R.id.bookingId);
            carModel = itemView.findViewById(R.id.carModel);
            customerName = itemView.findViewById(R.id.customerName);
            bookingDates = itemView.findViewById(R.id.bookingDates);
            bookingStatus = itemView.findViewById(R.id.bookingStatus);
            actionButtons = itemView.findViewById(R.id.actionButtons);

            actionButtons.setVisibility(isManagerMode ? View.VISIBLE : View.GONE);
        }

        public void bind(Booking booking) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

            bookingId.setText(String.format("Booking #%s", booking.getBookingId()));
            carModel.setText(booking.getCarModel());
            customerName.setText(booking.getCustomerName());
            bookingDates.setText(String.format("%s to %s",
                    dateFormat.format(booking.getStartDate()),
                    dateFormat.format(booking.getEndDate())));
            bookingStatus.setText(booking.getStatus());
        }
    }
}