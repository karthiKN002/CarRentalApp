package com.example.gearup.uiactivities.admin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gearup.R;

public class AdminProfileFragment extends Fragment {

    private TextView adminNameTextView, adminEmailTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminNameTextView = view.findViewById(R.id.adminNameTextView);
        adminEmailTextView = view.findViewById(R.id.adminEmailTextView);

        // Get current user info
        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences("CarRentalAppPrefs", Context.MODE_PRIVATE);

        String name = sharedPreferences.getString("fullName", "Admin User");
        String email = sharedPreferences.getString("email", "admin@example.com");

        adminNameTextView.setText(name);
        adminEmailTextView.setText(email);
    }
}