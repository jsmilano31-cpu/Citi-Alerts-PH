package com.example.citialertsph;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.citialertsph.adapters.LocationsAdapter;
import com.example.citialertsph.models.EvacuationCenter;
import com.example.citialertsph.models.LocationsResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LocationsFragment extends Fragment implements LocationsAdapter.OnLocationClickListener, LocationListener {

    private static final String TAG = "LocationsFragment";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String API_URL = "https://jsmkj.space/citialerts/app/api/mobile-locations.php"; // Update this URL

    // Views
    private MapView mapView;
    private RecyclerView recyclerViewLocations;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton fabMyLocation, fabToggleView;
    private View emptyState, mapLoadingFrame;
    private android.widget.TextView tvLocationCount, tvLastUpdated;

    // Data and Adapters
    private LocationsAdapter locationsAdapter;
    private List<EvacuationCenter> evacuationCenters;
    private OkHttpClient httpClient;
    private Gson gson;

    // Map and Location
    private MyLocationNewOverlay myLocationOverlay;
    private LocationManager locationManager;
    private boolean isMapView = true;

    // Default location (Manila, Philippines)
    private static final double DEFAULT_LAT = 14.5995;
    private static final double DEFAULT_LNG = 120.9842;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_locations, container, false);

        initializeViews(view);
        initializeMap();
        initializeRecyclerView();
        setupClickListeners();
        loadEvacuationCenters();

        return view;
    }

    private void initializeViews(View view) {
        mapView = view.findViewById(R.id.mapView);
        recyclerViewLocations = view.findViewById(R.id.recyclerViewLocations);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        fabMyLocation = view.findViewById(R.id.fabMyLocation);
        fabToggleView = view.findViewById(R.id.fabToggleView);
        emptyState = view.findViewById(R.id.emptyState);
        mapLoadingFrame = view.findViewById(R.id.mapLoadingFrame);
        tvLocationCount = view.findViewById(R.id.tvLocationCount);
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated);

        // Initialize data structures
        evacuationCenters = new ArrayList<>();
        httpClient = new OkHttpClient();
        gson = new Gson();

        // Setup swipe refresh
        swipeRefresh.setColorSchemeResources(R.color.primary_color);
        swipeRefresh.setOnRefreshListener(this::loadEvacuationCenters);
    }

    private void initializeMap() {
        try {
            // Configure osmdroid
            Context ctx = requireContext().getApplicationContext();
            Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
            Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

            // Improved map setup with better gesture handling
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.setBuiltInZoomControls(false); // Disable built-in zoom controls to prevent conflicts
            mapView.setHorizontalMapRepetitionEnabled(false);
            mapView.setVerticalMapRepetitionEnabled(false);

            // Set zoom limits for better performance
            mapView.setMinZoomLevel(5.0);
            mapView.setMaxZoomLevel(19.0);

            // Initial map position
            mapView.getController().setZoom(13.0);
            mapView.getController().setCenter(new GeoPoint(DEFAULT_LAT, DEFAULT_LNG));

            // Configure gesture detector for smoother interactions
            mapView.getOverlayManager().getTilesOverlay().setLoadingBackgroundColor(Color.TRANSPARENT);

            // Add my location overlay with improved settings
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
            myLocationOverlay.setDrawAccuracyEnabled(true);

            // Customize my location overlay appearance
            myLocationOverlay.setPersonHotspot(12.0f, 12.0f);

            mapView.getOverlays().add(myLocationOverlay);

            // Initialize location manager
            locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

            // Improved map ready callback
            mapView.addMapListener(new DelayedMapListener(new MapListener() {
                @Override
                public boolean onScroll(ScrollEvent event) {
                    return false;
                }

                @Override
                public boolean onZoom(ZoomEvent event) {
                    // Handle zoom events if needed
                    return false;
                }
            }));

            // Hide loading after map is ready with better timing
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (mapLoadingFrame != null) {
                    mapLoadingFrame.setVisibility(View.GONE);
                }
                // Force map invalidation for proper rendering
                mapView.invalidate();
            }, 1000);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing map", e);
            showError("Failed to initialize map");
        }
    }

    private void initializeRecyclerView() {
        locationsAdapter = new LocationsAdapter(requireContext(), evacuationCenters);
        locationsAdapter.setOnLocationClickListener(this);

        recyclerViewLocations.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewLocations.setAdapter(locationsAdapter);
        recyclerViewLocations.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        fabMyLocation.setOnClickListener(v -> getCurrentLocation());
        fabToggleView.setOnClickListener(v -> toggleMapView());
    }

    private void loadEvacuationCenters() {
        if (!swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(true);
        }

        Request request = new Request.Builder()
                .url(API_URL)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API call failed", e);
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    showError("Failed to load evacuation centers. Please check your internet connection.");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "HTTP error: " + response.code() + " " + response.message());
                        requireActivity().runOnUiThread(() -> {
                            swipeRefresh.setRefreshing(false);
                            showError("Server error: " + response.code() + ". Please try again later.");
                        });
                        return;
                    }

                    String responseBody = response.body().string();
                    Log.d(TAG, "API Response: " + responseBody);

                    if (responseBody == null || responseBody.trim().isEmpty()) {
                        Log.e(TAG, "Empty response body");
                        requireActivity().runOnUiThread(() -> {
                            swipeRefresh.setRefreshing(false);
                            showError("Empty response from server. Please try again later.");
                        });
                        return;
                    }

                    LocationsResponse locationsResponse = null;
                    try {
                        locationsResponse = gson.fromJson(responseBody, LocationsResponse.class);
                    } catch (Exception jsonException) {
                        Log.e(TAG, "JSON parsing error", jsonException);
                        requireActivity().runOnUiThread(() -> {
                            swipeRefresh.setRefreshing(false);
                            showError("Invalid response format from server.");
                        });
                        return;
                    }

                    if (locationsResponse == null) {
                        Log.e(TAG, "Parsed response is null");
                        requireActivity().runOnUiThread(() -> {
                            swipeRefresh.setRefreshing(false);
                            showError("Failed to parse server response.");
                        });
                        return;
                    }

                    // Make final copy for lambda
                    final LocationsResponse finalLocationsResponse = locationsResponse;

                    requireActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);

                        if (finalLocationsResponse.isSuccess() && finalLocationsResponse.getData() != null) {
                            updateLocationsList(finalLocationsResponse.getData());
                            updateMapMarkers(finalLocationsResponse.getData());
                        } else {
                            String errorMessage = finalLocationsResponse.getError() != null ?
                                    finalLocationsResponse.getError() : "Failed to load locations";
                            showError(errorMessage);
                            // Show empty state even if API call technically succeeded but returned no data
                            updateLocationsList(new ArrayList<>());
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error processing response", e);
                    requireActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);
                        showError("Unexpected error occurred. Please try again.");
                    });
                }
            }
        });
    }

    private void updateLocationsList(List<EvacuationCenter> locations) {
        evacuationCenters.clear();
        evacuationCenters.addAll(locations);
        locationsAdapter.updateLocations(evacuationCenters);

        // Update UI elements
        if (locations.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerViewLocations.setVisibility(View.GONE);
            tvLocationCount.setText("No evacuation centers found");
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerViewLocations.setVisibility(View.VISIBLE);
            tvLocationCount.setText(locations.size() + " evacuation centers found");
        }

        // Update last updated time
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        tvLastUpdated.setText("Updated " + sdf.format(new Date()));
    }

    private void updateMapMarkers(List<EvacuationCenter> locations) {
        // Clear existing markers (except my location)
        mapView.getOverlays().clear();
        mapView.getOverlays().add(myLocationOverlay);

        // Add markers for each evacuation center
        for (EvacuationCenter center : locations) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(center.getLatitude(), center.getLongitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(center.getName());
            marker.setSubDescription(center.getAddress());

            // Set marker icon (you can customize this)
            marker.setIcon(getResources().getDrawable(R.drawable.ic_location));

            // Add click listener
            marker.setOnMarkerClickListener((m, mapView) -> {
                onLocationClick(center);
                return true;
            });

            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();

        // Zoom to show all markers if there are any
        if (!locations.isEmpty()) {
            zoomToShowAllLocations(locations);
        }
    }

    private void zoomToShowAllLocations(List<EvacuationCenter> locations) {
        if (locations.isEmpty()) return;

        double minLat = locations.get(0).getLatitude();
        double maxLat = locations.get(0).getLatitude();
        double minLng = locations.get(0).getLongitude();
        double maxLng = locations.get(0).getLongitude();

        for (EvacuationCenter center : locations) {
            minLat = Math.min(minLat, center.getLatitude());
            maxLat = Math.max(maxLat, center.getLatitude());
            minLng = Math.min(minLng, center.getLongitude());
            maxLng = Math.max(maxLng, center.getLongitude());
        }

        // Add some padding
        double latPadding = (maxLat - minLat) * 0.1;
        double lngPadding = (maxLng - minLng) * 0.1;

        GeoPoint center = new GeoPoint((minLat + maxLat) / 2, (minLng + maxLng) / 2);
        mapView.getController().setCenter(center);

        // Calculate appropriate zoom level
        double latSpan = maxLat - minLat + latPadding;
        double lngSpan = maxLng - minLng + lngPadding;
        double maxSpan = Math.max(latSpan, lngSpan);

        int zoomLevel = (int) Math.max(10, 15 - Math.log(maxSpan * 100) / Math.log(2));
        mapView.getController().setZoom((double) Math.min(zoomLevel, 16));
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        try {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, Looper.getMainLooper());
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, Looper.getMainLooper());
            Toast.makeText(requireContext(), "Getting your location...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error getting location", e);
            showError("Unable to get your current location");
        }
    }

    private void toggleMapView() {
        isMapView = !isMapView;
        if (isMapView) {
            fabToggleView.setImageResource(R.drawable.ic_view_list);
            // Show map, hide list (implementation depends on your UI structure)
        } else {
            fabToggleView.setImageResource(R.drawable.ic_map);
            // Hide map, show list
        }
    }

    @Override
    public void onLocationClick(EvacuationCenter location) {
        // Center map on selected location
        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapView.getController().animateTo(point);
        mapView.getController().setZoom(16.0);

        // Show location details (you can implement a bottom sheet or dialog here)
        showLocationDetails(location);
    }

    @Override
    public void onDirectionsClick(EvacuationCenter location) {
        // Open directions in external maps app
        String uri = String.format(Locale.ENGLISH,
                "geo:%f,%f?q=%f,%f(%s)",
                location.getLatitude(), location.getLongitude(),
                location.getLatitude(), location.getLongitude(),
                location.getName());

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Fallback to browser
            String browserUri = String.format(Locale.ENGLISH,
                    "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                    location.getLatitude(), location.getLongitude());
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserUri));
            startActivity(browserIntent);
        }
    }

    private void showLocationDetails(EvacuationCenter location) {
        String message = String.format(Locale.getDefault(),
                "%s\n\n%s\n\nCapacity: %s\nContact: %s",
                location.getName(),
                location.getAddress(),
                location.getCapacityText(),
                location.getContactText());

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> loadEvacuationCenters())
                    .show();
        }
    }

    // LocationListener implementation
    @Override
    public void onLocationChanged(@NonNull Location location) {
        GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapView.getController().animateTo(userLocation);
        mapView.getController().setZoom(15.0);
        Toast.makeText(requireContext(), "Location found!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                showError("Location permission is required to show your current location");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
