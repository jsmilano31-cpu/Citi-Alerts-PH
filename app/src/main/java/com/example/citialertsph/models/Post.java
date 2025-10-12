package com.example.citialertsph.models;

public class Post {
    private int id;
    private int moderatorId;
    private String title;
    private String description;
    private String imagePath;
    private String status;
    private int views;
    private String createdAt;
    private String updatedAt;
    private String moderatorName;
    private String moderatorOrganization;
    private String moderatorImage;
    private boolean moderatorVerified;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getModeratorId() { return moderatorId; }
    public void setModeratorId(int moderatorId) { this.moderatorId = moderatorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getModeratorName() { return moderatorName; }
    public void setModeratorName(String moderatorName) { this.moderatorName = moderatorName; }

    public String getModeratorOrganization() { return moderatorOrganization; }
    public void setModeratorOrganization(String moderatorOrganization) { this.moderatorOrganization = moderatorOrganization; }

    public String getModeratorImage() { return moderatorImage; }
    public void setModeratorImage(String moderatorImage) { this.moderatorImage = moderatorImage; }

    public boolean isModeratorVerified() { return moderatorVerified; }
    public void setModeratorVerified(boolean moderatorVerified) { this.moderatorVerified = moderatorVerified; }
}
