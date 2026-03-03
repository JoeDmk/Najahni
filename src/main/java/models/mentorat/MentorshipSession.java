package models.mentorat;

import java.sql.Timestamp;

public class MentorshipSession {
    private int id;
    private int requestId;
    private Timestamp scheduledAt;
    private int durationMinutes;
    private SessionStatus status;
    private String mentorFeedback;
    private String entrepreneurFeedback;
    private int mentorRating;
    private int entrepreneurRating;
    private String meetingLink;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public enum SessionStatus {
        scheduled, completed, cancelled, no_show
    }

    public MentorshipSession() {
    }

    public MentorshipSession(int id, int requestId, Timestamp scheduledAt, int durationMinutes, SessionStatus status,
            String mentorFeedback, String entrepreneurFeedback, int mentorRating, int entrepreneurRating,
            String meetingLink, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.requestId = requestId;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.mentorFeedback = mentorFeedback;
        this.entrepreneurFeedback = entrepreneurFeedback;
        this.mentorRating = mentorRating;
        this.entrepreneurRating = entrepreneurRating;
        this.meetingLink = meetingLink;
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

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public Timestamp getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Timestamp scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getMentorFeedback() {
        return mentorFeedback;
    }

    public void setMentorFeedback(String mentorFeedback) {
        this.mentorFeedback = mentorFeedback;
    }

    public String getEntrepreneurFeedback() {
        return entrepreneurFeedback;
    }

    public void setEntrepreneurFeedback(String entrepreneurFeedback) {
        this.entrepreneurFeedback = entrepreneurFeedback;
    }

    public int getMentorRating() {
        return mentorRating;
    }

    public void setMentorRating(int mentorRating) {
        this.mentorRating = mentorRating;
    }

    public int getEntrepreneurRating() {
        return entrepreneurRating;
    }

    public void setEntrepreneurRating(int entrepreneurRating) {
        this.entrepreneurRating = entrepreneurRating;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
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
}

