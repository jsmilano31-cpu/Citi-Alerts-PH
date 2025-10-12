package com.example.citialertsph;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FeedbackFragment extends Fragment {

    private static final String TAG = "FeedbackFragment";
    private static final String API_URL = "https://jsmkj.space/citialerts/app/api/submit-feedback.php";

    // UI Components
    private TextInputEditText etUserName, etUserEmail, etFeedback;
    private RatingBar ratingBar, ratingEmergencyAlerts, ratingEvacuationCenters,
                     ratingNewsUpdates, ratingAppPerformance;
    private RadioGroup rgFeedbackCategory, rgRecommend;
    private MaterialButton btnSubmitFeedback;
    private TextView tvRatingText;
    private View loadingFrame;

    // Networking
    private OkHttpClient httpClient;
    private Gson gson;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);

        initializeViews(view);
        setupRatingListeners();
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        // Text inputs
        etUserName = view.findViewById(R.id.etUserName);
        etUserEmail = view.findViewById(R.id.etUserEmail);
        etFeedback = view.findViewById(R.id.etFeedback);

        // Rating bars
        ratingBar = view.findViewById(R.id.ratingBar);
        ratingEmergencyAlerts = view.findViewById(R.id.ratingEmergencyAlerts);
        ratingEvacuationCenters = view.findViewById(R.id.ratingEvacuationCenters);
        ratingNewsUpdates = view.findViewById(R.id.ratingNewsUpdates);
        ratingAppPerformance = view.findViewById(R.id.ratingAppPerformance);

        // Radio groups
        rgFeedbackCategory = view.findViewById(R.id.rgFeedbackCategory);
        rgRecommend = view.findViewById(R.id.rgRecommend);

        // Other components
        btnSubmitFeedback = view.findViewById(R.id.btnSubmitFeedback);
        tvRatingText = view.findViewById(R.id.tvRatingText);
        loadingFrame = view.findViewById(R.id.loadingFrame);

        // Initialize networking
        httpClient = new OkHttpClient();
        gson = new Gson();
    }

    private void setupRatingListeners() {
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                updateRatingText(rating);
            }
        });
    }

    private void updateRatingText(float rating) {
        String[] ratingTexts = {
            "Not rated", "Poor", "Fair", "Good", "Very Good", "Excellent"
        };
        int index = Math.min((int) rating, ratingTexts.length - 1);
        tvRatingText.setText(ratingTexts[index]);
    }

    private void setupClickListeners() {
        btnSubmitFeedback.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        if (!validateForm()) {
            return;
        }

        showLoading(true);

        // Collect all form data
        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("user_name", etUserName.getText().toString().trim());
        feedbackData.put("user_email", etUserEmail.getText().toString().trim());
        feedbackData.put("overall_rating", ratingBar.getRating());
        feedbackData.put("emergency_alerts_rating", ratingEmergencyAlerts.getRating());
        feedbackData.put("evacuation_centers_rating", ratingEvacuationCenters.getRating());
        feedbackData.put("news_updates_rating", ratingNewsUpdates.getRating());
        feedbackData.put("app_performance_rating", ratingAppPerformance.getRating());
        feedbackData.put("feedback_category", getSelectedFeedbackCategory());
        feedbackData.put("would_recommend", getSelectedRecommendation());
        feedbackData.put("comments", etFeedback.getText().toString().trim());

        // Convert to JSON
        String jsonData = gson.toJson(feedbackData);

        // Create request
        RequestBody requestBody = RequestBody.create(jsonData, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build();

        // Submit feedback
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Feedback submission failed", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Failed to submit feedback. Please check your internet connection.");
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Feedback response: " + responseBody);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            if (response.isSuccessful()) {
                                handleSuccessfulSubmission();
                            } else {
                                showError("Server error occurred. Please try again later.");
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing feedback response", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            showError("Error processing server response.");
                        });
                    }
                }
            }
        });
    }

    private boolean validateForm() {
        // Check required fields
        if (etUserName.getText().toString().trim().isEmpty()) {
            etUserName.setError("Name is required");
            etUserName.requestFocus();
            return false;
        }

        String email = etUserEmail.getText().toString().trim();
        if (email.isEmpty()) {
            etUserEmail.setError("Email is required");
            etUserEmail.requestFocus();
            return false;
        }

        if (!isValidEmail(email)) {
            etUserEmail.setError("Please enter a valid email address");
            etUserEmail.requestFocus();
            return false;
        }

        if (ratingBar.getRating() == 0) {
            showError("Please provide an overall rating");
            return false;
        }

        if (rgFeedbackCategory.getCheckedRadioButtonId() == -1) {
            showError("Please select a feedback category");
            return false;
        }

        if (rgRecommend.getCheckedRadioButtonId() == -1) {
            showError("Please indicate if you would recommend the app");
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private String getSelectedFeedbackCategory() {
        int selectedId = rgFeedbackCategory.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadio = getView().findViewById(selectedId);
            return selectedRadio.getText().toString();
        }
        return "";
    }

    private String getSelectedRecommendation() {
        int selectedId = rgRecommend.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadio = getView().findViewById(selectedId);
            return selectedRadio.getText().toString();
        }
        return "";
    }

    private void handleSuccessfulSubmission() {
        // Show success message
        if (getView() != null) {
            Snackbar.make(getView(), "Thank you! Your feedback has been submitted successfully.",
                    Snackbar.LENGTH_LONG).show();
        }

        // Clear form
        clearForm();
    }

    private void clearForm() {
        etUserName.setText("");
        etUserEmail.setText("");
        etFeedback.setText("");
        ratingBar.setRating(0);
        ratingEmergencyAlerts.setRating(0);
        ratingEvacuationCenters.setRating(0);
        ratingNewsUpdates.setRating(0);
        ratingAppPerformance.setRating(0);
        rgFeedbackCategory.clearCheck();
        rgRecommend.clearCheck();
        tvRatingText.setText("Not rated");
    }

    private void showLoading(boolean show) {
        if (loadingFrame != null) {
            loadingFrame.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnSubmitFeedback != null) {
            btnSubmitFeedback.setEnabled(!show);
        }
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
