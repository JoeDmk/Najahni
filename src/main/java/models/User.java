package models;

import util.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private int id;
    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String password;
    private Type role;
    private String bio;
    private String profilePicture;
    private String companyName;
    private String linkedinUrl;
    private String address;
    private LocalDate dateOfBirth;
    private boolean verified;
    private boolean phoneVerified;
    private boolean isActive;
    private boolean isBanned;
    private String googleProviderId;
    private boolean faceRegistered;
    private String preferredTheme;
    private String preferredLanguage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== Constructors ====================

    public User() {
    }

    /** Login constructor */
    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /** Sign-up constructor (minimal) */
    public User(String firstname, String lastname, String email, String phone, String password, Type role) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.verified = false;
        this.isActive = true;
        this.isBanned = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /** Full constructor (from DB) */
    public User(int id, String firstname, String lastname, String email, String phone, String password,
                Type role, String bio, String profilePicture, String companyName, String linkedinUrl,
                String address, LocalDate dateOfBirth, boolean verified, boolean isActive, boolean isBanned,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.bio = bio;
        this.profilePicture = profilePicture;
        this.companyName = companyName;
        this.linkedinUrl = linkedinUrl;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.verified = verified;
        this.isActive = isActive;
        this.isBanned = isBanned;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Admin update constructor */
    public User(int id, String firstname, String lastname, String email, String phone, String password,
                Type role, boolean isBanned, boolean isActive) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.isBanned = isBanned;
        this.isActive = isActive;
    }

    // ==================== Getters & Setters ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Type getRole() { return role; }
    public void setRole(Type role) { this.role = role; }

    // Backward compatibility aliases
    public Type getRoles() { return role; }
    public void setRoles(Type role) { this.role = role; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }

    public boolean getIsBanned() { return isBanned; }
    public void setIsBanned(boolean isBanned) { this.isBanned = isBanned; }

    public String getGoogleProviderId() { return googleProviderId; }
    public void setGoogleProviderId(String googleProviderId) { this.googleProviderId = googleProviderId; }

    public boolean isFaceRegistered() { return faceRegistered; }
    public void setFaceRegistered(boolean faceRegistered) { this.faceRegistered = faceRegistered; }

    public String getPreferredTheme() { return preferredTheme != null ? preferredTheme : "light"; }
    public void setPreferredTheme(String preferredTheme) { this.preferredTheme = preferredTheme; }

    public String getPreferredLanguage() { return preferredLanguage != null ? preferredLanguage : "fr"; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ==================== Display helpers ====================

    public String getIsActiveDisplay() {
        return isActive ? "Actif" : "Inactif";
    }

    public String getIsBannedDisplay() {
        return isBanned ? "Banni" : "Non banni";
    }

    public String getVerifiedDisplay() {
        return verified ? "Vérifié" : "Non vérifié";
    }

    public String getFullName() {
        return firstname + " " + lastname;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role=" + role +
                ", bio='" + bio + '\'' +
                ", companyName='" + companyName + '\'' +
                ", linkedinUrl='" + linkedinUrl + '\'' +
                ", address='" + address + '\'' +
                ", verified=" + verified +
                ", isActive=" + isActive +
                ", isBanned=" + isBanned +
                ", createdAt=" + createdAt +
                '}';
    }
}
