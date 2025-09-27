package com.example.citialertsph;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.citialertsph.utils.SessionManager;

public class ProfileFragment extends Fragment {
    private TextView tvUsername;
    private TextView tvEmail;
    private Button btnLogout;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(requireActivity());

        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Get user data from SessionManager instead of direct SharedPreferences
        tvUsername.setText(sessionManager.getUsername());
        tvEmail.setText(sessionManager.getUserEmail());

        btnLogout.setOnClickListener(v -> {
            // Use SessionManager for logout which includes Volley call
            sessionManager.logoutUser();

            // Redirect to LoginActivity
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}
