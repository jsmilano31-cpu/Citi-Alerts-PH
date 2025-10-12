package com.example.citialertsph.models;

import com.google.gson.annotations.SerializedName;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EmergencyRequest {
    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("emergency_type")
    private String emergencyType;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("location_name")
    private String locationName;

    @SerializedName("status")
    private String status;

    @SerializedName("responder_id")
    private Integer responderId;

    @SerializedName("created_at")
    private String createdAtStr;

    private transient Date createdAt;
    private transient int currentUserId; // Add this field
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getDefault());
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEmergencyType() { return emergencyType; }
    public void setEmergencyType(String emergencyType) { this.emergencyType = emergencyType; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getResponderId() { return responderId; }
    public void setResponderId(Integer responderId) { this.responderId = responderId; }

    public synchronized Date getCreatedAt() {
        if (createdAt == null && createdAtStr != null) {
            try {
                createdAt = DATE_FORMAT.parse(createdAtStr);
            } catch (ParseException e) {
                createdAt = new Date(); // Fallback to current time if parsing fails
            }
        }
        return createdAt != null ? createdAt : new Date();
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedAtStr() {
        return createdAtStr;
    }

    public void setCreatedAtStr(String createdAtStr) {
        this.createdAtStr = createdAtStr;
        if (createdAtStr != null) {
            try {
                this.createdAt = DATE_FORMAT.parse(createdAtStr);
            } catch (ParseException e) {
                this.createdAt = new Date(); // Fallback to current time if parsing fails
            }
        }
    }

    public boolean isOwnedByCurrentUser() {
        return userId == currentUserId;
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }
}
