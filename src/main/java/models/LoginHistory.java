package models;

import java.time.LocalDateTime;

public class LoginHistory {
    private int id;
    private int userId;
    private String ipAddress;
    private String deviceInfo;
    private String loginMethod; // PASSWORD, GOOGLE, FACE_ID
    private boolean success;
    private String location;
    private LocalDateTime loginTime;

    // Transient fields for display
    private String userEmail;
    private String userFullName;

    public LoginHistory() {}

    public LoginHistory(int userId, String ipAddress, String deviceInfo, String loginMethod, boolean success, String location) {
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.loginMethod = loginMethod;
        this.success = success;
        this.location = location;
        this.loginTime = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getLoginMethod() { return loginMethod; }
    public void setLoginMethod(String loginMethod) { this.loginMethod = loginMethod; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getSuccessDisplay() { return success ? "Succès" : "Échec"; }

    @Override
    public String toString() {
        return "LoginHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", ipAddress='" + ipAddress + '\'' +
                ", loginMethod='" + loginMethod + '\'' +
                ", success=" + success +
                ", loginTime=" + loginTime +
                '}';
    }
}
