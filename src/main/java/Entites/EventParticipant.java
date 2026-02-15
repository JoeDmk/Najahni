package Entites;

public class EventParticipant {

    private int id;
    private int eventId;
    private int userId;
    private String firstname;  // 🔥 add this
    public String getFirstname() {
        return firstname;
    }
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }
    public EventParticipant() {}

    public EventParticipant(int eventId, int userId) {
        this.eventId = eventId;
        this.userId = userId;
    }

    public EventParticipant(int id, int eventId, int userId) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public int getEventId() {
        return eventId;
    }

    public int getUserId() {
        return userId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "EventParticipant{" +
                "id=" + id +
                ", eventId=" + eventId +
                ", userId=" + userId +
                '}';
    }
}
