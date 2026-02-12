package com.najahni.models;

import java.time.LocalDateTime;

/**
 * Model class representing a Project entity.
 * Projects are created by entrepreneurs and can receive investments.
 */
public class Project {

    private int id;
    private String title;
    private String description;
    private String sector;
    private ProjectStatus status;
    private int entrepreneurId;
    private String entrepreneurName; // For display purposes
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public Project() {
        this.status = ProjectStatus.DRAFT;
    }

    // Constructor without id (for creating new projects)
    public Project(String title, String description, String sector, ProjectStatus status, int entrepreneurId) {
        this.title = title;
        this.description = description;
        this.sector = sector;
        this.status = status;
        this.entrepreneurId = entrepreneurId;
    }

    // Full constructor
    public Project(int id, String title, String description, String sector, ProjectStatus status, int entrepreneurId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.sector = sector;
        this.status = status;
        this.entrepreneurId = entrepreneurId;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public int getEntrepreneurId() {
        return entrepreneurId;
    }

    public void setEntrepreneurId(int entrepreneurId) {
        this.entrepreneurId = entrepreneurId;
    }

    public String getEntrepreneurName() {
        return entrepreneurName;
    }

    public void setEntrepreneurName(String entrepreneurName) {
        this.entrepreneurName = entrepreneurName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return title + " [" + sector + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Project project = (Project) obj;
        return id == project.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
