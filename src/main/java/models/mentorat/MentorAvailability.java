package models.mentorat;

import java.sql.Time;
import java.sql.Timestamp;

public class MentorAvailability {
    private int id;
    private int mentorId;
    private java.sql.Date date;
    private Time startTime;
    private Time endTime;
    private Timestamp createdAt;

    // Helper for display
    private String mentorName;

    public MentorAvailability() {
    }

    public MentorAvailability(int id, int mentorId, java.sql.Date date, Time startTime, Time endTime,
            Timestamp createdAt) {
        this.id = id;
        this.mentorId = mentorId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMentorId() {
        return mentorId;
    }

    public void setMentorId(int mentorId) {
        this.mentorId = mentorId;
    }

    public java.sql.Date getDate() {
        return date;
    }

    public void setDate(java.sql.Date date) {
        this.date = date;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getMentorName() {
        return mentorName;
    }

    public void setMentorName(String mentorName) {
        this.mentorName = mentorName;
    }
}

