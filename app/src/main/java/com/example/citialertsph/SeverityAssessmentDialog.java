package com.example.citialertsph;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Dialog for emergency severity assessment
 * Collects severity level, injury status, accessibility, and additional notes
 */
public class SeverityAssessmentDialog {

    public interface SeverityAssessmentListener {
        void onAssessmentComplete(SeverityData severityData);

        void onAssessmentCancelled();
    }

    public static class SeverityData {
        private String emergencyType;
        private String severityDescription;
        private int severityLevel;
        private boolean hasInjuries;
        private String areaAccessible;
        private String additionalNotes;

        public SeverityData(String emergencyType, String severityDescription, int severityLevel,
                            boolean hasInjuries, String areaAccessible, String additionalNotes) {
            this.emergencyType = emergencyType;
            this.severityDescription = severityDescription;
            this.severityLevel = severityLevel;
            this.hasInjuries = hasInjuries;
            this.areaAccessible = areaAccessible;
            this.additionalNotes = additionalNotes;
        }

        // Getters
        public String getEmergencyType() {
            return emergencyType;
        }

        public String getSeverityDescription() {
            return severityDescription;
        }

        public int getSeverityLevel() {
            return severityLevel;
        }

        public boolean hasInjuries() {
            return hasInjuries;
        }

        public String getAreaAccessible() {
            return areaAccessible;
        }

        public String getAdditionalNotes() {
            return additionalNotes;
        }
    }

    private Context context;
    private String emergencyType;
    private SeverityAssessmentListener listener;
    private AlertDialog dialog;

    // UI Components
    private TextView emergencyTypeText;
    private RadioGroup severityRadioGroup;
    private RadioGroup injuriesRadioGroup;
    private RadioGroup accessibilityRadioGroup;
    private TextInputEditText additionalNotesInput;
    private MaterialButton sendRequestButton;
    private MaterialButton cancelButton;

    public SeverityAssessmentDialog(Context context, String emergencyType, SeverityAssessmentListener listener) {
        this.context = context;
        this.emergencyType = emergencyType;
        this.listener = listener;
    }

    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_severity_assessment, null);
        initializeViews(dialogView);
        setupDialog(dialogView);
        setupListeners();

        dialog.show();
    }

    private void initializeViews(View dialogView) {
        emergencyTypeText = dialogView.findViewById(R.id.emergencyTypeText);
        severityRadioGroup = dialogView.findViewById(R.id.severityRadioGroup);
        injuriesRadioGroup = dialogView.findViewById(R.id.injuriesRadioGroup);
        accessibilityRadioGroup = dialogView.findViewById(R.id.accessibilityRadioGroup);
        additionalNotesInput = dialogView.findViewById(R.id.additionalNotesInput);
        sendRequestButton = dialogView.findViewById(R.id.sendRequestButton);
        cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Set emergency type
        emergencyTypeText.setText(emergencyType);

        // Set default selections
        ((RadioButton) dialogView.findViewById(R.id.radioModerate)).setChecked(true);
        ((RadioButton) dialogView.findViewById(R.id.radioInjuriesNo)).setChecked(true);
        ((RadioButton) dialogView.findViewById(R.id.radioAccessibleYes)).setChecked(true);
    }

    private void setupDialog(View dialogView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        builder.setCancelable(true);

        dialog = builder.create();

        // Make dialog background transparent for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void setupListeners() {
        sendRequestButton.setOnClickListener(v -> {
            if (validateInputs()) {
                SeverityData data = collectData();
                if (listener != null) {
                    listener.onAssessmentComplete(data);
                }
                dialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAssessmentCancelled();
            }
            dialog.dismiss();
        });

        // Update button color based on severity selection
        severityRadioGroup.setOnCheckedChangeListener((group, checkedId) -> updateButtonAppearance());
    }

    private boolean validateInputs() {
        if (severityRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please select the severity level", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (injuriesRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please indicate if there are any injuries", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (accessibilityRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(context, "Please indicate if the area is accessible", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private SeverityData collectData() {
        // Get severity level
        String severityDescription = "Moderate";
        int severityLevel = 3;

        int selectedSeverityId = severityRadioGroup.getCheckedRadioButtonId();
        if (selectedSeverityId == R.id.radioMinor) {
            severityDescription = "Minor";
            severityLevel = 1;
        } else if (selectedSeverityId == R.id.radioModerate) {
            severityDescription = "Moderate";
            severityLevel = 3;
        } else if (selectedSeverityId == R.id.radioSevere) {
            severityDescription = "Severe";
            severityLevel = 4;
        }

        // Get injury status
        boolean hasInjuries = injuriesRadioGroup.getCheckedRadioButtonId() == R.id.radioInjuriesYes;

        // Get accessibility status
        String areaAccessible = "Yes";
        int selectedAccessibilityId = accessibilityRadioGroup.getCheckedRadioButtonId();
        if (selectedAccessibilityId == R.id.radioAccessibleNo) {
            areaAccessible = "No";
        } else if (selectedAccessibilityId == R.id.radioAccessibleBlocked) {
            areaAccessible = "Blocked";
        }

        // Get additional notes
        String additionalNotes = "";
        if (additionalNotesInput.getText() != null) {
            additionalNotes = additionalNotesInput.getText().toString().trim();
        }

        // Calculate final severity level based on all factors
        int finalSeverityLevel = calculateSeverityLevel(severityDescription, hasInjuries, areaAccessible);

        return new SeverityData(emergencyType, severityDescription, finalSeverityLevel,
                hasInjuries, areaAccessible, additionalNotes);
    }

    private int calculateSeverityLevel(String severity, boolean hasInjuries, String accessibility) {
        int baseLevel = 1;

        // Base severity level
        switch (severity.toLowerCase()) {
            case "minor":
                baseLevel = 1;
                break;
            case "moderate":
                baseLevel = 3;
                break;
            case "severe":
                baseLevel = 4;
                break;
        }

        // Increase level if there are injuries
        if (hasInjuries) {
            baseLevel = Math.min(5, baseLevel + 1);
        }

        // Increase level if area is blocked or inaccessible
        if ("blocked".equalsIgnoreCase(accessibility) || "no".equalsIgnoreCase(accessibility)) {
            baseLevel = Math.min(5, baseLevel + 1);
        }

        return baseLevel;
    }

    private void updateButtonAppearance() {
        int selectedId = severityRadioGroup.getCheckedRadioButtonId();
        int color = Color.parseColor("#D32F2F"); // Default red

        if (selectedId == R.id.radioMinor) {
            color = Color.parseColor("#388E3C"); // Green
        } else if (selectedId == R.id.radioModerate) {
            color = Color.parseColor("#F57C00"); // Orange
        } else if (selectedId == R.id.radioSevere) {
            color = Color.parseColor("#D32F2F"); // Red
        }

        // Update button background color
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(32f);
        sendRequestButton.setBackgroundDrawable(background);
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}