package com.example.gearup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.google.android.libraries.places.api.model.Place;
import java.util.List;

public class MechanicsAdapter extends RecyclerView.Adapter<MechanicsAdapter.MechanicViewHolder> {
    private List<Place> mechanics;

    public MechanicsAdapter(List<Place> mechanics) {
        this.mechanics = mechanics;
    }

    @Override
    public MechanicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mechanic, parent, false);
        return new MechanicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MechanicViewHolder holder, int position) {
        Place place = mechanics.get(position);
        holder.nameTextView.setText(place.getName());
        holder.addressTextView.setText(place.getAddress());
    }

    @Override
    public int getItemCount() {
        return mechanics.size();
    }

    static class MechanicViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, addressTextView;

        MechanicViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.mechanicName);
            addressTextView = itemView.findViewById(R.id.mechanicAddress);
        }
    }
}