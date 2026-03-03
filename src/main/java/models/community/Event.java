package models.community;

import java.sql.Timestamp;

public class Event {

    private int id;
    private String title;
    private String description;
    private Timestamp eventDate;
    private Timestamp createdAt;
    private int capacity;
    private int userId;
    public int getUserId(){
        return userId;
    }
    public void setUserId(int userId){
        this.userId=userId;
    }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public Event() {}
    public Event(int id, String title, String description,
                 Timestamp eventDate, Timestamp createdAt, int capacity,int userId) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.createdAt = createdAt;
        this.capacity = capacity;
        this.userId=userId;
    }



    public Event(int id, String title, String description, Timestamp eventDate, Timestamp createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.createdAt = createdAt;
    }

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

    public Timestamp getEventDate() {
        return eventDate;
    }

    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", eventDate=" + eventDate +
                ", createdAt=" + createdAt +
                ", capacity=" + capacity +
                '}';
    }

}
