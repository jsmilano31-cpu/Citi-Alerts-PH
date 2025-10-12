package com.example.citialertsph.models;

import java.util.List;

public class LocationsResponse {
    private boolean success;
    private List<EvacuationCenter> data;
    private int count;
    private String message;
    private String error;

    // Constructor
    public LocationsResponse() {}

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<EvacuationCenter> getData() { return data; }
    public void setData(List<EvacuationCenter> data) { this.data = data; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
