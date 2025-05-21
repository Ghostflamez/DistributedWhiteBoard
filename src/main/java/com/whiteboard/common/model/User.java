package com.whiteboard.common.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uid;         // 系统生成的唯一标识符
    private String username;    // 用户提供的显示名称
    private String sessionId;   // 会话ID
    private boolean isManager;  // 是否为管理员
    private long lastActivity;  // 最后活动时间

    public User(String uid, String username, boolean isManager) {
        this.uid = uid;
        this.username = username;
        this.isManager = isManager;
        this.lastActivity = System.currentTimeMillis();
    }

    // 添加 getter/setter 方法
    public String getUid() {
        return uid;
    }

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

    public void updateActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        User other = (User) obj;
        return uid != null ? uid.equals(other.uid) : other.uid == null;
    }

    @Override
    public int hashCode() {
        return uid != null ? uid.hashCode() : 0;
    }
}