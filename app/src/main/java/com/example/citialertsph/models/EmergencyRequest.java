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

    // Emergency Severity Assessment fields
    @SerializedName("severity_level")
    private int severityLevel; // 1-5 scale

    @SerializedName("severity_description")
    private String severityDescription; // "Minor", "Moderate", "Severe"

    @SerializedName("has_injuries")
    private boolean hasInjuries;

    @SerializedName("area_accessible")
    private String areaAccessible; // "Yes", "No", "Blocked"

    @SerializedName("additional_notes")
    private String additionalNotes;

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

    // Severity Assessment getters and setters
    public int getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(int severityLevel) {
        this.severityLevel = severityLevel;
    }

    public String getSeverityDescription() {
        return severityDescription;
    }

    public void setSeverityDescription(String severityDescription) {
        this.severityDescription = severityDescription;
    }

    public boolean hasInjuries() {
        return hasInjuries;
    }

    public void setHasInjuries(boolean hasInjuries) {
        this.hasInjuries = hasInjuries;
    }

    public String getAreaAccessible() {
        return areaAccessible;
    }

    public void setAreaAccessible(String areaAccessible) {
        this.areaAccessible = areaAccessible;
    }

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

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

    // Helper methods for severity assessment
    public String getSeverityColor() {
        switch (severityLevel) {
            case 1:
            case 2:
                return "#4CAF50"; // Green for minor
            case 3:
                return "#FF9800"; // Orange for moderate
            case 4:
            case 5:
                return "#F44336"; // Red for severe
            default:
                return "#9E9E9E"; // Gray for unknown
        }
    }

    public String getSeverityIcon() {
        switch (severityLevel) {
            case 1:
            case 2:
                return "ic_info"; // Info icon for minor
            case 3:
                return "ic_warning"; // Warning icon for moderate
            case 4:
            case 5:
                return "ic_emergency"; // Emergency icon for severe
            default:
                return "ic_help"; // Help icon for unknown
        }
    }

    public static int calculateSeverityLevel(String severity, boolean hasInjuries, String accessibility) {
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
}
