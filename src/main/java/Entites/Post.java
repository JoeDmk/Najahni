    package Entites;

    import java.sql.Timestamp;

    public class Post {

        private int id;
        private int userId;
        private String content;
        private String imageUrl;      // ✅ from posts.image_url
        private Timestamp createdAt;  // ✅ from posts.created_at

        private String firstname;     // ✅ from users.firstname (join)
        private String lastname;      // ✅ from users.lastname (optional but recommended)

        private int reactionsCount;
        private String myReaction; // "LIKE","LOVE","HAHA","WOW","SAD","ANGRY" or null

        public int getReactionsCount() { return reactionsCount; }
        public void setReactionsCount(int reactionsCount) { this.reactionsCount = reactionsCount; }

        public String getMyReaction() { return myReaction; }
        public void setMyReaction(String myReaction) { this.myReaction = myReaction; }

        public Post() {}

        public Post(int userId, String content) {
            this.userId = userId;
            this.content = content;
        }

        public Post(int id, int userId, String content) {
            this.id = id;
            this.userId = userId;
            this.content = content;
        }

        // Getters/Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }

        public String getLastname() { return lastname; }
        public void setLastname(String lastname) { this.lastname = lastname; }


        @Override
        public String toString() {
            return "Post{" +
                    "id=" + id +
                    ", userId=" + userId +
                    ", content='" + content + '\'' +
                    ", imageUrl='" + imageUrl + '\'' +
                    ", createdAt=" + createdAt +
                    ", firstname='" + firstname + '\'' +
                    ", lastname='" + lastname + '\'' +
                    '}';
        }
    }
