package com.example.citialertsph.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.citialertsph.R;
import com.example.citialertsph.models.EmergencyRequest;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmergencyListAdapter extends RecyclerView.Adapter<EmergencyListAdapter.ViewHolder> {
    private static final String TAG = "EmergencyListAdapter";
    private List<EmergencyRequest> emergencyRequests = new ArrayList<>();
    private OnEmergencyActionListener listener;
    private SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private boolean isUserMode = false; // To distinguish between user and responder views

    public interface OnEmergencyActionListener {
        void onRespond(EmergencyRequest request);
        void onComplete(EmergencyRequest request);
        void onItemClick(EmergencyRequest request);
    }

    public EmergencyListAdapter(OnEmergencyActionListener listener, boolean isUserMode) {
        this.listener = listener;
        this.isUserMode = isUserMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyRequest request = emergencyRequests.get(position);
        holder.bind(request);
    }

    @Override
    public int getItemCount() {
        return emergencyRequests.size();
    }

    public void updateData(List<EmergencyRequest> newRequests) {
        this.emergencyRequests = newRequests != null ? newRequests : new ArrayList<>();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView emergencyTypeText;
        TextView locationText;
        TextView timeText;
        TextView statusText;
        MaterialButton respondButton;
        MaterialButton completeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            emergencyTypeText = itemView.findViewById(R.id.emergencyTypeText);
            locationText = itemView.findViewById(R.id.locationText);
            timeText = itemView.findViewById(R.id.timeText);
            statusText = itemView.findViewById(R.id.statusText);
            respondButton = itemView.findViewById(R.id.respondButton);
            completeButton = itemView.findViewById(R.id.completeButton);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(emergencyRequests.get(position));
                }
            });

            respondButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRespond(emergencyRequests.get(position));
                }
            });

            completeButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onComplete(emergencyRequests.get(position));
                }
            });
        }

        void bind(EmergencyRequest request) {
            try {
                emergencyTypeText.setText(request.getEmergencyType());
                locationText.setText(request.getLocationName());

                // Handle date formatting with null checks
                Date createdAt = request.getCreatedAt();
                timeText.setText(createdAt != null ? displayFormat.format(createdAt) : "Just now");

                // Set status text and color
                statusText.setVisibility(View.VISIBLE);
                switch (request.getStatus()) {
                    case "pending":
                        statusText.setText("PENDING");
                        statusText.setBackgroundResource(R.color.status_pending);
                        break;
                    case "help_coming":
                        statusText.setText("HELP COMING");
                        statusText.setBackgroundResource(R.color.status_help_coming);
                        break;
                    case "completed":
                        statusText.setText("COMPLETED");
                        statusText.setBackgroundResource(R.color.status_completed);
                        break;
                    default:
                        statusText.setVisibility(View.GONE);
                        break;
                }

                // Handle button visibility and styling based on mode and status
                if (isUserMode) {
                    respondButton.setVisibility(View.GONE);
                    // Show complete button only if this is user's request and help is coming
                    if (request.isOwnedByCurrentUser() && "help_coming".equals(request.getStatus())) {
                        completeButton.setVisibility(View.VISIBLE);
                        completeButton.setEnabled(true);
                        completeButton.setAlpha(1.0f);
                    } else if (request.isOwnedByCurrentUser() && "completed".equals(request.getStatus())) {
                        // Show completed button but disabled for completed requests
                        completeButton.setVisibility(View.VISIBLE);
                        completeButton.setText("COMPLETED ✓");
                        completeButton.setEnabled(false);
                        completeButton.setAlpha(0.7f);
                        completeButton.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_blue_light));
                    } else {
                        completeButton.setVisibility(View.GONE);
                    }
                } else {
                    completeButton.setVisibility(View.GONE);
                    respondButton.setVisibility(
                        "pending".equals(request.getStatus()) ? View.VISIBLE : View.GONE
                    );
                }

                // Apply visual styling based on status
                if ("completed".equals(request.getStatus())) {
                    // Completed requests - slightly grayed out with checkmark
                    itemView.setAlpha(0.8f);
                    emergencyTypeText.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
                    locationText.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
                    timeText.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));

                    // Add checkmark to emergency type for completed requests
                    emergencyTypeText.setText(request.getEmergencyType() + " ✓");
                } else if (!isUserMode && !"pending".equals(request.getStatus())) {
                    // Gray out non-pending items for responder view
                    itemView.setAlpha(0.6f);
                    emergencyTypeText.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
                } else {
                    // Reset to normal styling for active requests
                    itemView.setAlpha(1.0f);
                    emergencyTypeText.setTextColor(itemView.getContext().getColor(android.R.color.black));
                    locationText.setTextColor(itemView.getContext().getColor(android.R.color.black));
                    timeText.setTextColor(itemView.getContext().getColor(android.R.color.black));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error binding emergency request", e);
            }
        }
    }
}
