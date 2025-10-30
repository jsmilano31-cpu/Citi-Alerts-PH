package com.example.citialertsph;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.OnBackPressedCallback;

import com.example.citialertsph.models.EmergencyRequest;
import com.example.citialertsph.utils.ApiClient;
import com.example.citialertsph.utils.SessionManager;
import com.example.citialertsph.SeverityAssessmentDialog;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EmergencyActivity extends AppCompatActivity implements EmergencyListFragment.OnEmergencySelectedListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI Components
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private MapView mapView;
    private TextView locationText;
    private TextView statusIndicator;
    private TextView distanceIndicator;
    private MaterialButton emergencyButton;
    private MaterialButton cancelButton;
    private FloatingActionButton listViewButton;
    private FloatingActionButton fabMyLocation;
    private MaterialCardView statusCard;
    private MaterialCardView locationCard;
    private MaterialCardView mapCard;
    private MaterialCardView quickActionsCard;
    private MaterialCardView listCard; // added
    private View bottomActionsArea;     // added
    private ProgressBar loadingIndicator;
    private ImageView statusIcon;
    private GridLayout quickActionsGrid;

    // Location and Emergency Management
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private double currentLatitude;
    private double currentLongitude;
    private String currentAddress;
    private SessionManager sessionManager;
    private Handler statusCheckHandler;
    private static final long STATUS_CHECK_INTERVAL = 10000; // 10 seconds
    private final ApiClient apiClient = new ApiClient();
    private String currentEmergencyId;

    private static final String TAG = "EmergencyActivity";

    private static final String PREF_EMERGENCY_STATE = "emergency_state";
    private static final String KEY_EMERGENCY_ID = "current_emergency_id";
    private static final String KEY_EMERGENCY_STATUS = "emergency_status";
    private static final String KEY_HAS_RESPONDER = "has_responder";
    private static final String KEY_LAST_MESSAGE = "last_message";

    private EmergencyListFragment listFragment;
    private List<EmergencyRequest> activeRequests = new ArrayList<>();
    private boolean isModerator;
    private boolean listViewVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check session persistence
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Initialize OpenStreetMap configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_emergency);

        // Use consistent role checking
        boolean isResponderView = sessionManager.isModerator();

        initializeViews();
        setupToolbar();
        setupLocationServices();
        setupStatusChecker();
        setupQuickActions();

        // Register back press handler using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (listViewVisible) {
                    showMapView();
                } else {
                    finish();
                }
            }
        });

        if (!isResponderView) {
            restoreEmergencyState();
        }
        setupResponderView();

        // Add entrance animations
        animateViewsOnStart();
    }

    private void initializeViews() {
        // Initialize all UI components
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBarLayout);
        mapView = findViewById(R.id.mapView);
        locationText = findViewById(R.id.locationText);
        statusIndicator = findViewById(R.id.statusIndicator);
        distanceIndicator = findViewById(R.id.distanceIndicator);
        emergencyButton = findViewById(R.id.emergencyButton);
        cancelButton = findViewById(R.id.cancelButton);
        listViewButton = findViewById(R.id.listViewButton);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        statusCard = findViewById(R.id.statusCard);
        locationCard = findViewById(R.id.locationCard);
        mapCard = findViewById(R.id.mapCard);
        quickActionsCard = findViewById(R.id.quickActionsCard);
        listCard = findViewById(R.id.listCard); // added
        bottomActionsArea = findViewById(R.id.bottomActionsArea); // added
        loadingIndicator = findViewById(R.id.loadingIndicator);
        statusIcon = findViewById(R.id.statusIcon);

        // Setup map
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getController().setZoom(18.0);
        mapView.setMultiTouchControls(true);

        // Setup click listeners
        emergencyButton.setOnClickListener(v -> showEmergencyTypeDialog());
        cancelButton.setOnClickListener(v -> showCancelConfirmationDialog());

        fabMyLocation.setOnClickListener(v -> {
            if (currentLatitude != 0 && currentLongitude != 0) {
                GeoPoint point = new GeoPoint(currentLatitude, currentLongitude);
                mapView.getController().animateTo(point);
                // Add pulse animation to show location
                fabMyLocation.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            }
        });

        listViewButton.setOnClickListener(v -> {
            if (listViewVisible) {
                showMapView();
            } else {
                showEmergencyListFragment();
            }
        });

        // Back to map FAB inside list header
        View fabBackToMap = findViewById(R.id.fabBackToMap);
        if (fabBackToMap != null) {
            fabBackToMap.setOnClickListener(v -> showMapView());
        }

        // Hide emergency button for moderators
        if (sessionManager.isModerator()) {
            emergencyButton.setVisibility(View.GONE);
            findViewById(R.id.actionsLayout).setVisibility(View.GONE);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Emergency Response");
        }

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupQuickActions() {
        if (sessionManager.isModerator()) {
            quickActionsCard.setVisibility(View.GONE);
            return;
        }

        // Setup quick action cards with click listeners
        String[] emergencyTypes = {"Fire", "Medical Emergency (Stroke, etc.)", "Criminal Activity", "Flood"};

        // Find all quick action cards and set up click listeners
        View fireCard = quickActionsCard.findViewById(R.id.quickActionsCard).getRootView()
                .findViewById(R.id.quickActionsCard);

        // Since we're using a GridLayout, we'll need to set up click listeners programmatically
        // This is a simplified approach - you might want to implement this more robustly
        quickActionsCard.setOnClickListener(v -> {
            // Handle quick action clicks by showing the dialog
            showEmergencyTypeDialog();
        });
    }

    private void animateViewsOnStart() {
        // Animate cards with staggered entrance
        locationCard.setAlpha(0f);
        mapCard.setAlpha(0f);
        quickActionsCard.setAlpha(0f);

        locationCard.animate().alpha(1f).setDuration(500).setStartDelay(100).start();
        mapCard.animate().alpha(1f).setDuration(500).setStartDelay(200).start();
        quickActionsCard.animate().alpha(1f).setDuration(500).setStartDelay(300).start();
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                updateLocation(location);
            }
        };

        if (checkLocationPermission()) {
            startLocationUpdates();
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(5000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback, Looper.getMainLooper());
    }

    private void updateLocation(Location location) {
        if (location != null) {
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();

            GeoPoint point = new GeoPoint(currentLatitude, currentLongitude);
            mapView.getController().animateTo(point);

            updateMarker(point);
            updateAddressText(currentLatitude, currentLongitude);
        }
    }

    private void updateMarker(GeoPoint point) {
        mapView.getOverlays().clear();
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Your Location");

        // Style the marker
        Drawable markerIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation);
        if (markerIcon != null) {
            markerIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary_color), PorterDuff.Mode.SRC_IN);
            marker.setIcon(markerIcon);
        }

        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    private void updateAddressText(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                currentAddress = address.getAddressLine(0);
                locationText.setText(currentAddress);

                // Animate the location text update
                locationText.setAlpha(0.5f);
                locationText.animate().alpha(1f).setDuration(300).start();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting address", e);
            locationText.setText(getString(R.string.location_format, latitude, longitude));
        }
    }

    private void showEmergencyTypeDialog() {
        String[] emergencyTypes = {"Fire", "Criminal Activity", "Flood", "Medical Emergency (Stroke, etc.)", "Other"};
        int[] colors = {
            android.R.color.holo_red_light,
            android.R.color.holo_orange_dark,
            android.R.color.holo_blue_dark,
            android.R.color.holo_blue_light,
            android.R.color.holo_purple
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.emergency_type_title)
                .setItems(emergencyTypes, (dialog, which) -> {
                    String selectedType = emergencyTypes[which];
                    showSeverityAssessmentDialog(selectedType);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showSeverityAssessmentDialog(String emergencyType) {
        SeverityAssessmentDialog severityDialog = new SeverityAssessmentDialog(this, emergencyType,
                new SeverityAssessmentDialog.SeverityAssessmentListener() {
                    @Override
                    public void onAssessmentComplete(SeverityAssessmentDialog.SeverityData severityData) {
                        sendEmergencyRequestWithSeverity(severityData);
                    }

                    @Override
                    public void onAssessmentCancelled() {
                        // User cancelled the assessment, nothing to do
                    }
                });

        severityDialog.show();
    }

    private void sendEmergencyRequestWithSeverity(SeverityAssessmentDialog.SeverityData severityData) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("user_id", sessionManager.getUserId());
            json.addProperty("emergency_type", severityData.getEmergencyType());
            json.addProperty("latitude", currentLatitude);
            json.addProperty("longitude", currentLongitude);
            json.addProperty("location_name", currentAddress);

            // Add severity assessment data
            json.addProperty("severity_level", severityData.getSeverityLevel());
            json.addProperty("severity_description", severityData.getSeverityDescription());
            json.addProperty("has_injuries", severityData.hasInjuries());
            json.addProperty("area_accessible", severityData.getAreaAccessible());

            if (!severityData.getAdditionalNotes().isEmpty()) {
                json.addProperty("additional_notes", severityData.getAdditionalNotes());
            }

            Log.d("EmergencyActivity", "Sending emergency request with severity: " + json.toString());

            // Show loading state immediately with animation
            String loadingMessage = String.format("Sending %s emergency request (Level %d)...",
                    severityData.getSeverityDescription().toLowerCase(), severityData.getSeverityLevel());
            showLoadingStateWithAnimation(loadingMessage);

            apiClient.postRequest("create_emergency.php", json, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("EmergencyActivity", "Request failed", e);
                    runOnUiThread(() -> {
                        hideLoadingStateWithAnimation();
                        Toast.makeText(EmergencyActivity.this,
                                "Failed to send emergency request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("EmergencyActivity", "Response received: " + responseData);

                    if (response.isSuccessful()) {
                        try {
                            JsonObject jsonResponse = ApiClient.parseResponse(responseData);
                            if (jsonResponse.has("request_id")) {
                                currentEmergencyId = jsonResponse.get("request_id").getAsString();
                                Log.d("EmergencyActivity", "Emergency ID received: " + currentEmergencyId);

                                runOnUiThread(() -> {
                                    String priorityMessage = getSeverityPriorityMessage(severityData.getSeverityLevel());
                                    updateLoadingStateWithAnimation("Request sent! " + priorityMessage);
                                    startStatusChecking();
                                });
                            } else {
                                Log.e("EmergencyActivity", "No request_id in response: " + responseData);
                                runOnUiThread(() -> {
                                    hideLoadingStateWithAnimation();
                                    Toast.makeText(EmergencyActivity.this,
                                            "Invalid server response", Toast.LENGTH_LONG).show();
                                });
                            }
                        } catch (Exception e) {
                            Log.e("EmergencyActivity", "Error parsing response", e);
                            runOnUiThread(() -> {
                                hideLoadingStateWithAnimation();
                                Toast.makeText(EmergencyActivity.this,
                                        "Error parsing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    } else {
                        Log.e("EmergencyActivity", "Server error: " + response.code() + " - " + responseData);
                        runOnUiThread(() -> {
                            hideLoadingStateWithAnimation();
                            Toast.makeText(EmergencyActivity.this,
                                    "Server error: " + response.code(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e("EmergencyActivity", "Error creating request", e);
            hideLoadingStateWithAnimation();
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getSeverityPriorityMessage(int severityLevel) {
        switch (severityLevel) {
            case 1:
            case 2:
                return "Looking for available responders...";
            case 3:
                return "Priority request - searching for responders...";
            case 4:
                return "HIGH PRIORITY - Urgent response required!";
            case 5:
                return "CRITICAL EMERGENCY - Immediate response dispatched!";
            default:
                return "Looking for available responders...";
        }
    }


    private void showLoadingStateWithAnimation(String message) {
        statusCard.setVisibility(View.VISIBLE);
        statusCard.setAlpha(0f);
        statusCard.animate().alpha(1f).setDuration(300).start();

        loadingIndicator.setVisibility(View.VISIBLE);
        statusIcon.setVisibility(View.GONE);
        statusIndicator.setText(message);
        emergencyButton.setEnabled(false);

        // Animate button state
        emergencyButton.animate().alpha(0.5f).setDuration(200).start();
    }

    private void updateLoadingStateWithAnimation(String message) {
        statusCard.setVisibility(View.VISIBLE);
        statusIndicator.setText(message);

        // Pulse animation for status text
        statusIndicator.setAlpha(0.7f);
        statusIndicator.animate().alpha(1f).setDuration(500).start();
    }

    private void hideLoadingStateWithAnimation() {
        statusCard.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            statusCard.setVisibility(View.GONE);
            statusCard.setAlpha(1f);
        }).start();

        loadingIndicator.setVisibility(View.GONE);
        emergencyButton.setEnabled(true);
        emergencyButton.animate().alpha(1f).setDuration(200).start();
    }

    // Add the missing showLoadingState method (non-animated version for state restoration)
    private void showLoadingState(String message) {
        statusCard.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.VISIBLE);
        statusIcon.setVisibility(View.GONE);
        statusIndicator.setText(message);
        emergencyButton.setEnabled(false);
    }

    // Add the missing hideLoadingState method (non-animated version)
    private void hideLoadingState() {
        statusCard.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
        emergencyButton.setEnabled(true);
    }

    private void setupStatusChecker() {
        statusCheckHandler = new Handler(Looper.getMainLooper());
    }

    private void startStatusChecking() {
        // Stop any existing polling
        if (statusCheckHandler != null) {
            statusCheckHandler.removeCallbacksAndMessages(null);
        }

        // Create a new runnable for polling
        Runnable pollingRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("EmergencyActivity", "Polling cycle triggered at: " + System.currentTimeMillis());
                checkEmergencyStatus();
                // Schedule next run
                statusCheckHandler.postDelayed(this, STATUS_CHECK_INTERVAL);
            }
        };

        // Check immediately first
        Log.d("EmergencyActivity", "Starting initial status check");
        checkEmergencyStatus();

        // Then start periodic checks
        statusCheckHandler.postDelayed(pollingRunnable, STATUS_CHECK_INTERVAL);
    }

    private void checkEmergencyStatus() {
        if (currentEmergencyId == null) {
            Log.w("EmergencyActivity", "No emergency ID to check status - stopping status checks");
            stopStatusChecking();
            return;
        }

        Log.d("EmergencyActivity", "Active polling: Checking status for emergency: " + currentEmergencyId);

        apiClient.getRequest("check_emergency_status.php?request_id=" + currentEmergencyId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("EmergencyActivity", "Status check failed", e);
                // Don't stop checking on network failures, just log the error
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body().string();
                Log.d("EmergencyActivity", "Status response received at: " + System.currentTimeMillis() + " - " + responseData);

                if (response.isSuccessful()) {
                    try {
                        JsonObject jsonResponse = ApiClient.parseResponse(responseData);
                        runOnUiThread(() -> updateStatusIndicator(jsonResponse));
                    } catch (Exception e) {
                        Log.e("EmergencyActivity", "Error parsing status response", e);
                    }
                } else if (response.code() == 404) {
                    // Emergency not found - likely completed or deleted
                    Log.w("EmergencyActivity", "Emergency not found - stopping status checks");
                    runOnUiThread(() -> {
                        stopStatusChecking();
                        clearEmergencyState();
                        currentEmergencyId = null;
                        hideLoadingStateWithAnimation();
                        Toast.makeText(EmergencyActivity.this, "Emergency request no longer active", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void restoreEmergencyState() {
        SharedPreferences prefs = getSharedPreferences(PREF_EMERGENCY_STATE, MODE_PRIVATE);
        currentEmergencyId = prefs.getString(KEY_EMERGENCY_ID, null);

        if (currentEmergencyId != null) {
            String status = prefs.getString(KEY_EMERGENCY_STATUS, "pending");
            boolean hasResponder = prefs.getBoolean(KEY_HAS_RESPONDER, false);
            String message = prefs.getString(KEY_LAST_MESSAGE, "Checking status...");

            // Restore UI state
            showLoadingState(message);

            // Create a temporary status object to update the UI
            JsonObject savedStatus = new JsonObject();
            savedStatus.addProperty("status", status);
            savedStatus.addProperty("has_responder", hasResponder);
            updateStatusIndicator(savedStatus);

            // Start immediate polling
            startStatusChecking();
        }
    }

    private void saveEmergencyState(String status, boolean hasResponder, String message) {
        getSharedPreferences(PREF_EMERGENCY_STATE, MODE_PRIVATE)
                .edit()
                .putString(KEY_EMERGENCY_ID, currentEmergencyId)
                .putString(KEY_EMERGENCY_STATUS, status)
                .putBoolean(KEY_HAS_RESPONDER, hasResponder)
                .putString(KEY_LAST_MESSAGE, message)
                .apply();
    }

    private void clearEmergencyState() {
        getSharedPreferences(PREF_EMERGENCY_STATE, MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.cancel_dialog_title)
                .setMessage(R.string.cancel_dialog_message)
                .setPositiveButton(R.string.yes_cancel, (dialog, which) -> cancelEmergencyRequest())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void cancelEmergencyRequest() {
        if (currentEmergencyId == null) return;

        JsonObject json = new JsonObject();
        json.addProperty("request_id", currentEmergencyId);

        apiClient.postRequest("cancel_emergency.php", json, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(EmergencyActivity.this,
                        "Failed to cancel request: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body().string();
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        // Stop status checking and clear state immediately
                        stopStatusChecking();
                        clearEmergencyState();
                        currentEmergencyId = null;
                        hideLoadingStateWithAnimation();
                        Toast.makeText(EmergencyActivity.this,
                                "Emergency request cancelled", Toast.LENGTH_LONG).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(EmergencyActivity.this,
                            "Failed to cancel request", Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    // Update updateStatusIndicator method to use string resources
    private void updateStatusIndicator(JsonObject status) {
        try {
            String emergencyStatus = status.get("status").getAsString();
            boolean hasResponder = status.get("has_responder").getAsBoolean();

            statusCard.setVisibility(View.VISIBLE);
            String message;
            int backgroundColor;
            boolean showLoading = true;
            boolean showCancelButton = true;

            // Reset distance indicator
            distanceIndicator.setVisibility(View.GONE);

            if (hasResponder) {
                message = getString(R.string.help_on_way);
                backgroundColor = ContextCompat.getColor(this, android.R.color.holo_green_light);
                showLoading = false;

                // When help is coming, show the user's emergency list so they can complete it
                showUserEmergencyList();

                // Show distance message if available
                if (status.has("distance_message")) {
                    String distanceMessage = status.get("distance_message").getAsString();
                    distanceIndicator.setText(distanceMessage);
                    distanceIndicator.setVisibility(View.VISIBLE);

                    if (distanceMessage.contains("arrived") || distanceMessage.contains("very near")) {
                        backgroundColor = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                    }
                }
            } else {
                switch (emergencyStatus) {
                    case "pending":
                        message = getString(R.string.looking_for_responders);
                        backgroundColor = ContextCompat.getColor(this, android.R.color.holo_orange_light);
                        break;
                    case "completed":
                        message = getString(R.string.request_completed);
                        backgroundColor = ContextCompat.getColor(this, android.R.color.holo_blue_light);
                        showLoading = false;
                        showCancelButton = false;

                        // Stop status checking immediately
                        stopStatusChecking();
                        clearEmergencyState();
                        currentEmergencyId = null;

                        // Hide status card after a brief delay to show completion message
                        if (statusCheckHandler != null) {
                            statusCheckHandler.postDelayed(() -> {
                                hideLoadingStateWithAnimation();
                                Toast.makeText(this, "Emergency request completed successfully!", Toast.LENGTH_LONG).show();
                            }, 2000);
                        }

                        return; // Exit early to prevent further processing

                    case "cancelled":
                        message = getString(R.string.request_cancelled);
                        backgroundColor = ContextCompat.getColor(this, android.R.color.holo_orange_light);
                        showLoading = false;
                        showCancelButton = false;

                        // Stop status checking immediately
                        stopStatusChecking();
                        clearEmergencyState();
                        currentEmergencyId = null;

                        // Hide status card after a brief delay to show cancellation message
                        if (statusCheckHandler != null) {
                            statusCheckHandler.postDelayed(() -> {
                                hideLoadingStateWithAnimation();
                            }, 2000);
                        }

                        return; // Exit early to prevent further processing

                    default:
                        message = getString(R.string.waiting_response);
                        backgroundColor = ContextCompat.getColor(this, android.R.color.holo_orange_light);
                }
            }

            // Save current state if not completed or cancelled
            if (showCancelButton) {
                saveEmergencyState(emergencyStatus, hasResponder, message);
            }

            statusCard.setCardBackgroundColor(backgroundColor);
            statusIndicator.setText(message);
            loadingIndicator.setVisibility(showLoading ? View.VISIBLE : View.GONE);
            cancelButton.setVisibility(showCancelButton ? View.VISIBLE : View.GONE);
            emergencyButton.setEnabled(!showLoading);

        } catch (Exception e) {
            Log.e(TAG, "Error updating status indicator", e);
        }
    }

    private void stopStatusChecking() {
        if (statusCheckHandler != null) {
            statusCheckHandler.removeCallbacksAndMessages(null);
            Log.d("EmergencyActivity", "Status checking stopped");
        }
    }

    private void showUserEmergencyList() {
        if (listFragment == null) {
            // Create fragment specifically for user view (true = user mode)
            listFragment = EmergencyListFragment.newInstance(true);
            listFragment.setOnEmergencySelectedListener(this);
        }
        // Reuse common toggling logic
        showEmergencyListFragment();
    }

    private void showEmergencyListFragment() {
        if (listFragment == null) {
            // Create fragment with appropriate mode
            listFragment = EmergencyListFragment.newInstance(!sessionManager.isModerator());
            listFragment.setOnEmergencySelectedListener(this);
        }

        // Ensure fragment is attached
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, listFragment)
            .commit();

        // Toggle full-screen containers
        if (mapCard != null) mapCard.setVisibility(View.GONE);
        if (listCard != null) listCard.setVisibility(View.VISIBLE);
        if (bottomActionsArea != null) bottomActionsArea.setVisibility(View.GONE);
        listViewVisible = true;
        updateViewButtons();
    }

    private void showMapView() {
        // Toggle full-screen containers
        if (mapCard != null) mapCard.setVisibility(View.VISIBLE);
        if (listCard != null) listCard.setVisibility(View.GONE);
        if (bottomActionsArea != null) bottomActionsArea.setVisibility(View.VISIBLE);
        listViewVisible = false;
        updateViewButtons();
    }

    private void updateViewButtons() {
        if (listViewButton != null) {
            // FloatingActionButton only supports icons, not text
            listViewButton.setImageDrawable(ContextCompat.getDrawable(this,
                listViewVisible ? android.R.drawable.ic_dialog_map : android.R.drawable.ic_menu_view));
        }
    }

    // EmergencyListFragment.OnEmergencySelectedListener implementation
    @Override
    public void onEmergencySelected(EmergencyRequest request) {
        getSupportFragmentManager().popBackStack();
        // Center map on selected emergency
        mapView.getController().animateTo(new GeoPoint(request.getLatitude(), request.getLongitude()));
        showEmergencyDetails(request);
    }

    @Override
    public void onEmergencyResponse(EmergencyRequest request) {
        acceptEmergencyRequest(request);
    }

    private void showEmergencyDetails(EmergencyRequest request) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_emergency_details, null);
        TextView typeText = dialogView.findViewById(R.id.emergencyTypeText);
        TextView locText = dialogView.findViewById(R.id.locationText);
        TextView timeText = dialogView.findViewById(R.id.timeText);
        MaterialButton respondButton = dialogView.findViewById(R.id.respondButton);
        MaterialButton dismissButton = dialogView.findViewById(R.id.dismissButton);

        // Severity assessment UI components
        TextView severityDescriptionText = dialogView.findViewById(R.id.severityDescriptionText);
        TextView severityLevelBadge = dialogView.findViewById(R.id.severityLevelBadge);
        TextView injuryStatusText = dialogView.findViewById(R.id.injuryStatusText);
        TextView accessibilityStatusText = dialogView.findViewById(R.id.accessibilityStatusText);
        android.widget.LinearLayout additionalNotesLayout = dialogView.findViewById(R.id.additionalNotesLayout);
        TextView additionalNotesText = dialogView.findViewById(R.id.additionalNotesText);
        ImageView severityIcon = dialogView.findViewById(R.id.severityIcon);

        typeText.setText(request.getEmergencyType());
        locText.setText(request.getLocationName());
        try {
            String formatted = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(request.getCreatedAt());
            timeText.setText(formatted);
        } catch (Exception e) {
            timeText.setText(String.valueOf(request.getCreatedAt()));
        }

        // Display severity assessment information
        displaySeverityInformation(request, severityDescriptionText, severityLevelBadge,
                injuryStatusText, accessibilityStatusText,
                additionalNotesLayout, additionalNotesText, severityIcon);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Close button always available
        if (dismissButton != null) {
            dismissButton.setOnClickListener(v -> dialog.dismiss());
        }

        if ("pending".equalsIgnoreCase(request.getStatus()) && sessionManager.isModerator()) {
            respondButton.setVisibility(View.VISIBLE);
            respondButton.setOnClickListener(v -> {
                acceptEmergencyRequest(request);
                dialog.dismiss();
            });
        } else {
            respondButton.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private void acceptEmergencyRequest(EmergencyRequest request) {
        JsonObject json = new JsonObject();
        json.addProperty("request_id", request.getId());
        json.addProperty("responder_id", sessionManager.getUserId());

        apiClient.postRequest("accept_emergency.php", json, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(EmergencyActivity.this,
                        getString(R.string.failed_accept), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(EmergencyActivity.this,
                                getString(R.string.request_accepted), Toast.LENGTH_SHORT).show();
                        // Switch to list so responder can manage the request
                        showEmergencyListFragment();
                    } else {
                        Toast.makeText(EmergencyActivity.this,
                                getString(R.string.failed_accept), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // Display severity assessment information in the dialog for the selected emergency request
    private void displaySeverityInformation(EmergencyRequest request, TextView severityDescriptionText,
                                            TextView severityLevelBadge, TextView injuryStatusText,
                                            TextView accessibilityStatusText, android.widget.LinearLayout additionalNotesLayout,
                                            TextView additionalNotesText, ImageView severityIcon) {

        int severityLevel = request.getSeverityLevel();
        String severityDescription = request.getSeverityDescription();

        // Set severity description
        if (severityDescription != null && !severityDescription.isEmpty()) {
            severityDescriptionText.setText(severityDescription);
        } else {
            severityDescriptionText.setText("Not assessed");
        }

        // Set severity level badge
        if (severityLevel > 0) {
            severityLevelBadge.setText("LEVEL " + severityLevel);

            // Set badge color based on severity level
            int badgeColor;
            int iconTint;
            switch (severityLevel) {
                case 1:
                case 2:
                    badgeColor = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                    iconTint = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                    break;
                case 3:
                    badgeColor = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
                    iconTint = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
                    break;
                case 4:
                case 5:
                    badgeColor = ContextCompat.getColor(this, android.R.color.holo_red_dark);
                    iconTint = ContextCompat.getColor(this, android.R.color.holo_red_dark);
                    break;
                default:
                    badgeColor = ContextCompat.getColor(this, android.R.color.darker_gray);
                    iconTint = ContextCompat.getColor(this, android.R.color.darker_gray);
            }

            severityLevelBadge.setBackgroundColor(badgeColor);
            severityIcon.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN);
        } else {
            severityLevelBadge.setText("N/A");
            severityLevelBadge.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        // Set injury status
        if (request.hasInjuries()) {
            injuryStatusText.setText("Injuries reported");
            injuryStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        } else {
            injuryStatusText.setText("No injuries reported");
            injuryStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }

        // Set accessibility status
        String accessibility = request.getAreaAccessible();
        if (accessibility != null) {
            switch (accessibility.toLowerCase()) {
                case "yes":
                    accessibilityStatusText.setText("Area is accessible");
                    accessibilityStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                    break;
                case "no":
                    accessibilityStatusText.setText("Area is not accessible");
                    accessibilityStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
                    break;
                case "blocked":
                    accessibilityStatusText.setText("Area is blocked");
                    accessibilityStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                    break;
                default:
                    accessibilityStatusText.setText("Accessibility unknown");
                    accessibilityStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            }
        } else {
            accessibilityStatusText.setText("Accessibility not assessed");
            accessibilityStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        // Show additional notes if available
        String additionalNotes = request.getAdditionalNotes();
        if (additionalNotes != null && !additionalNotes.trim().isEmpty()) {
            additionalNotesLayout.setVisibility(View.VISIBLE);
            additionalNotesText.setText(additionalNotes);
        } else {
            additionalNotesLayout.setVisibility(View.GONE);
        }
    }

    // Add: allow the owner to mark an emergency request as completed
    public void completeEmergencyRequest(EmergencyRequest request) {
        if (request == null) return;

        JsonObject json = new JsonObject();
        json.addProperty("request_id", request.getId());
        json.addProperty("user_id", sessionManager.getUserId());

        apiClient.postRequest("complete_emergency.php", json, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(EmergencyActivity.this,
                        "Failed to complete request: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                JsonObject jsonResponse = ApiClient.parseResponse(responseData);

                runOnUiThread(() -> {
                    boolean ok = response.isSuccessful()
                            && jsonResponse.has("success")
                            && jsonResponse.get("success").getAsBoolean();

                    if (ok) {
                        Toast.makeText(EmergencyActivity.this,
                                "Emergency marked as completed", Toast.LENGTH_SHORT).show();

                        // Update status card to completed and clear state immediately
                        JsonObject status = new JsonObject();
                        status.addProperty("status", "completed");
                        status.addProperty("has_responder", false);
                        updateStatusIndicator(status);

                        // Refresh the list UI if visible
                        if (listFragment != null) {
                            listFragment.refreshList();
                        }

                        // Return to map view after completion for cleaner UX
                        showMapView();
                    } else {
                        String msg = jsonResponse.has("error")
                                ? jsonResponse.get("error").getAsString()
                                : "Failed to complete request";
                        Toast.makeText(EmergencyActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusCheckHandler != null) {
            statusCheckHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (checkLocationPermission()) {
            startLocationUpdates();
        }
        // Resume status checking if we have an active emergency
        if (currentEmergencyId != null) {
            Log.d("EmergencyActivity", "Resuming status checking for emergency: " + currentEmergencyId);
            startStatusChecking();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        // Stop status checking when activity is paused
        if (statusCheckHandler != null) {
            Log.d("EmergencyActivity", "Pausing status checking");
            statusCheckHandler.removeCallbacksAndMessages(null);
        }
    }

    private void setupResponderView() {
        // If not a moderator/responder, nothing special to set up
        if (!sessionManager.isModerator()) return;

        // Ensure default view is map with actions hidden for moderators
        if (mapCard != null) mapCard.setVisibility(View.VISIBLE);
        if (listCard != null) listCard.setVisibility(View.GONE);
        if (bottomActionsArea != null) bottomActionsArea.setVisibility(View.GONE);
        listViewVisible = false;
        updateViewButtons();

        // Prepare the list fragment for later switching (responder mode = false)
        if (listFragment == null) {
            listFragment = EmergencyListFragment.newInstance(false);
            listFragment.setOnEmergencySelectedListener(this);
        }
        // Note: fetching/markers are handled by fragment when list is shown
    }
}
