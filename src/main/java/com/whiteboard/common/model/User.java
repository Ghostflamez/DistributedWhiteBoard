package com.whiteboard.common.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String sessionId;
    private boolean isManager;
    private long lastActivity;

    public User(String username, boolean isManager) {
        this.username = username;
        this.isManager = isManager;
        this.lastActivity = System.currentTimeMillis();
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isManager() {
        return isManager;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        User other = (User) obj;
        return sessionId != null ? sessionId.equals(other.sessionId) : other.sessionId == null;
    }

    @Override
    public int hashCode() {
        return sessionId != null ? sessionId.hashCode() : 0;
    }
}