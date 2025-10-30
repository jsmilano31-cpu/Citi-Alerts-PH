package com.example.citialertsph.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        TextView severityBadge;
        TextView severityDescriptionText;
        LinearLayout severityInfoLayout;
        ImageView injuryIndicator;
        ImageView accessibilityIndicator;
        MaterialButton respondButton;
        MaterialButton completeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            emergencyTypeText = itemView.findViewById(R.id.emergencyTypeText);
            locationText = itemView.findViewById(R.id.locationText);
            timeText = itemView.findViewById(R.id.timeText);
            statusText = itemView.findViewById(R.id.statusText);
            severityBadge = itemView.findViewById(R.id.severityBadge);
            severityDescriptionText = itemView.findViewById(R.id.severityDescriptionText);
            severityInfoLayout = itemView.findViewById(R.id.severityInfoLayout);
            injuryIndicator = itemView.findViewById(R.id.injuryIndicator);
            accessibilityIndicator = itemView.findViewById(R.id.accessibilityIndicator);
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

                // Display severity information
                displaySeverityInfo(request);

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

        private void displaySeverityInfo(EmergencyRequest request) {
            int severityLevel = request.getSeverityLevel();
            String severityDescription = request.getSeverityDescription();

            // Show severity badge if level is available
            if (severityLevel > 0) {
                severityBadge.setVisibility(View.VISIBLE);
                severityBadge.setText("L" + severityLevel);

                // Set badge color based on severity level
                int badgeColor;
                switch (severityLevel) {
                    case 1:
                    case 2:
                        badgeColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark);
                        break;
                    case 3:
                        badgeColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_orange_dark);
                        break;
                    case 4:
                    case 5:
                        badgeColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark);
                        break;
                    default:
                        badgeColor = ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray);
                }
                severityBadge.setBackgroundColor(badgeColor);
            } else {
                severityBadge.setVisibility(View.GONE);
            }

            // Show severity description if available
            if (severityDescription != null && !severityDescription.isEmpty()) {
                severityInfoLayout.setVisibility(View.VISIBLE);
                severityDescriptionText.setText(severityDescription);

                // Set description color based on severity
                int textColor;
                switch (severityDescription.toLowerCase()) {
                    case "minor":
                        textColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark);
                        break;
                    case "moderate":
                        textColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_orange_dark);
                        break;
                    case "severe":
                        textColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark);
                        break;
                    default:
                        textColor = ContextCompat.getColor(itemView.getContext(), android.R.color.black);
                }
                severityDescriptionText.setTextColor(textColor);
            } else {
                severityInfoLayout.setVisibility(View.GONE);
            }

            // Show injury indicator if injuries are reported
            if (request.hasInjuries()) {
                injuryIndicator.setVisibility(View.VISIBLE);
                injuryIndicator.setColorFilter(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.holo_red_dark));
            } else {
                injuryIndicator.setVisibility(View.GONE);
            }

            // Show accessibility indicator if area is not accessible or blocked
            String accessibility = request.getAreaAccessible();
            if (accessibility != null && !"yes".equalsIgnoreCase(accessibility)) {
                accessibilityIndicator.setVisibility(View.VISIBLE);
                int iconColor = "blocked".equalsIgnoreCase(accessibility) ?
                        android.R.color.holo_red_dark : android.R.color.holo_orange_dark;
                accessibilityIndicator.setColorFilter(ContextCompat.getColor(itemView.getContext(), iconColor));
            } else {
                accessibilityIndicator.setVisibility(View.GONE);
            }
        }
    }
}
