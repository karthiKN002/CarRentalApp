package com.example.gearup.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gearup.R;

import java.util.ArrayList;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {
    private ArrayList<String> imageUrls;
    private Context context;
    private OnImageRemoveListener onImageRemoveListener;

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    public ImagePreviewAdapter(Context context, ArrayList<String> imageUrls, OnImageRemoveListener listener) {
        this.context = context;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        this.onImageRemoveListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view, onImageRemoveListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        if (imageUrls.isEmpty()) {
            Glide.with(context)
                    .load(R.drawable.car_placeholder)
                    .into(holder.imageView);
            holder.removeButton.setVisibility(View.GONE);
        } else {
            String imageUrl = imageUrls.get(position);
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.car_placeholder)
                    .into(holder.imageView);
            holder.removeButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.isEmpty() ? 1 : imageUrls.size();
    }

    public void updateImages(ArrayList<String> newImageUrls) {
        this.imageUrls = newImageUrls != null ? newImageUrls : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addImage(String imageUrl) {
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }
        imageUrls.add(imageUrl);
        notifyItemInserted(imageUrls.size() - 1);
    }

    public void removeImage(int position) {
        if (position >= 0 && position < imageUrls.size()) {
            imageUrls.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, imageUrls.size());
        }
    }

    public String getImageAt(int position) {
        if (position >= 0 && position < imageUrls.size()) {
            return imageUrls.get(position);
        }
        return null;
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton removeButton;

        public ImageViewHolder(@NonNull View itemView, OnImageRemoveListener listener) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imagePreview);
            removeButton = itemView.findViewById(R.id.buttonRemoveImage);

            removeButton.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onRemove(position);
                    }
                }
            });
        }
    }
}