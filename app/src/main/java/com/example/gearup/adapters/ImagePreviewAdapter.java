package com.example.gearup.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.gearup.R;
import java.util.ArrayList;
import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {

    private final Context context;
    private final ArrayList<String> images;
    private final OnImageRemoveListener removeListener;

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    public ImagePreviewAdapter(Context context, ArrayList<String> images, OnImageRemoveListener listener) {
        this.context = context;
        this.images = new ArrayList<>(images); // Defensive copy
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = images.get(position);
        Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .into(holder.imageView);
        holder.removeButton.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void addImages(List<String> newImages) {
        int startPosition = images.size();
        images.addAll(newImages);
        notifyItemRangeInserted(startPosition, newImages.size());
    }

    public void removeImage(int position) {
        if (position >= 0 && position < images.size()) {
            images.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, images.size());
        }
    }

    public void updateImages(List<String> newImages) {
        images.clear();
        images.addAll(newImages);
        notifyDataSetChanged();
    }

    public String getImageAt(int position) {
        if (position >= 0 && position < images.size()) {
            return images.get(position);
        }
        return null;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        Button removeButton;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            removeButton = itemView.findViewById(R.id.buttonRemoveImage);
        }
    }
}