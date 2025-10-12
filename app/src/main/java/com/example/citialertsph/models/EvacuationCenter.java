package com.example.citialertsph.models;

public class EvacuationCenter {
    private int id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String description;
    private Integer capacity;
    private String contactNumber;
    private String status;
    private String createdAt;
    private String updatedAt;

    // Constructor
    public EvacuationCenter() {}

    public EvacuationCenter(int id, String name, String address, double latitude, double longitude,
                           String description, Integer capacity, String contactNumber, String status,
                           String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.capacity = capacity;
        this.contactNumber = contactNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public String getCapacityText() {
        return capacity != null ? capacity + " people" : "Capacity not specified";
    }

    public String getContactText() {
        return contactNumber != null && !contactNumber.isEmpty() ? contactNumber : "No contact available";
    }

    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
}
