package com.example.gearup.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gearup.R;
import com.example.gearup.models.Mechanic;
import java.util.List;

public class MechanicsAdapter extends RecyclerView.Adapter<MechanicsAdapter.MechanicViewHolder> {

    private Context context;
    private List<Mechanic> mechanics;
    private OnMechanicClickListener clickListener;

    public MechanicsAdapter(Context context, List<Mechanic> mechanics, OnMechanicClickListener clickListener) {
        this.context = context;
        this.mechanics = mechanics;
        this.clickListener = clickListener;
    }

    public interface OnMechanicClickListener {
        void onMechanicClick(Mechanic mechanic);
    }

    @NonNull
    @Override
    public MechanicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mechanic, parent, false);
        return new MechanicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MechanicViewHolder holder, int position) {
        Mechanic mechanic = mechanics.get(position);
        holder.nameTextView.setText(mechanic.getFullName());
        holder.locationTextView.setText(String.format("%s (%.2f km)", mechanic.getLocationString(), mechanic.getDistance()));
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMechanicClick(mechanic);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mechanics.size();
    }

    static class MechanicViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, locationTextView;

        MechanicViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.mechanicName);
            locationTextView = itemView.findViewById(R.id.mechanicLocation);
        }
    }
}