package models.mentorat;

import java.sql.Timestamp;

public class MentorshipRequest {
    private int id;
    private int entrepreneurId;
    private int mentorId;
    private int projectId;
    private java.sql.Date date;
    private String time;
    private String motivation;
    private String goals;
    private float matchScore;
    private boolean autoApproved;
    private RequestStatus status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Helper fields for display
    private String entrepreneurName;
    private String mentorName;
    private String projectName;

    public enum RequestStatus {
        auto_accepted, pending_review, rejected, cancelled, completed
    }

    public MentorshipRequest() {
    }

    public MentorshipRequest(int id, int entrepreneurId, int mentorId, int projectId, java.sql.Date date, String time,
            String motivation, String goals,
            float matchScore, boolean autoApproved, RequestStatus status, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.entrepreneurId = entrepreneurId;
        this.mentorId = mentorId;
        this.projectId = projectId;
        this.date = date;
        this.time = time;
        this.motivation = motivation;
        this.goals = goals;
        this.matchScore = matchScore;
        this.autoApproved = autoApproved;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEntrepreneurId() {
        return entrepreneurId;
    }

    public void setEntrepreneurId(int entrepreneurId) {
        this.entrepreneurId = entrepreneurId;
    }

    public int getMentorId() {
        return mentorId;
    }

    public void setMentorId(int mentorId) {
        this.mentorId = mentorId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public java.sql.Date getDate() {
        return date;
    }

    public void setDate(java.sql.Date date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public String getGoals() {
        return goals;
    }

    public void setGoals(String goals) {
        this.goals = goals;
    }

    public float getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(float matchScore) {
        this.matchScore = matchScore;
    }

    public boolean isAutoApproved() {
        return autoApproved;
    }

    public void setAutoApproved(boolean autoApproved) {
        this.autoApproved = autoApproved;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEntrepreneurName() {
        return entrepreneurName;
    }

    public void setEntrepreneurName(String entrepreneurName) {
        this.entrepreneurName = entrepreneurName;
    }

    public String getMentorName() {
        return mentorName;
    }

    public void setMentorName(String mentorName) {
        this.mentorName = mentorName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String toString() {
        return "Request #" + id + " (" + status + ") - " + date + " " + time;
    }
}

