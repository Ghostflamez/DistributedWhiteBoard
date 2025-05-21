package com.whiteboard.server;

import com.whiteboard.common.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class UserManager {
    private static final Logger logger = Logger.getLogger(UserManager.class.getName());

    private String managerId = null;
    private Map<String, User> connectedUsers;  // sessionId -> User
    private Map<String, User> pendingUsers;    // sessionId -> User
    private Map<String, String> usernameToSessionId; // username -> sessionId

    public UserManager() {
        connectedUsers = new ConcurrentHashMap<>();
        pendingUsers = new ConcurrentHashMap<>();
        usernameToSessionId = new ConcurrentHashMap<>();
    }

    /**
     * 连接用户，如果是第一个用户则成为管理员
     * @param username 用户名
     * @return 会话ID
     */
    public synchronized String connectUser(String username) {
        // 生成唯一会话ID
        String sessionId = UUID.randomUUID().toString();

        // 第一个连接的用户成为管理员
        boolean isManager = (managerId == null);

        User user = new User(username, isManager);
        user.setSessionId(sessionId);

        if (isManager) {
            // 管理员直接连接
            managerId = sessionId;
            connectedUsers.put(sessionId, user);
            usernameToSessionId.put(username, sessionId);
            logger.info("Manager connected: " + username);
        } else {
            // 其他用户需要管理员批准
            pendingUsers.put(sessionId, user);
            logger.info("User pending approval: " + username);
        }

        return sessionId;
    }

    /**
     * 管理员批准用户
     * @param username 要批准的用户名
     * @param managerId 管理员会话ID
     * @return 是否成功批准
     */
    public synchronized boolean approveUser(String username, String managerId) {
        if (!isManager(managerId)) {
            logger.warning("Non-manager attempted to approve user: " + username);
            return false;
        }

        // 查找匹配的待审核用户
        String pendingSessionId = null;
        for (Map.Entry<String, User> entry : pendingUsers.entrySet()) {
            if (entry.getValue().getUsername().equals(username)) {
                pendingSessionId = entry.getKey();
                break;
            }
        }

        if (pendingSessionId != null) {
            User user = pendingUsers.get(pendingSessionId);
            pendingUsers.remove(pendingSessionId);
            connectedUsers.put(pendingSessionId, user);
            usernameToSessionId.put(username, pendingSessionId);
            logger.info("User approved: " + username);
            return true;
        }

        logger.warning("User not found in pending list: " + username);
        return false;
    }

    /**
     * 移除用户
     * @param sessionId 会话ID
     */
    public synchronized void removeUser(String sessionId) {
        User user = connectedUsers.get(sessionId);
        if (user != null) {
            connectedUsers.remove(sessionId);
            usernameToSessionId.remove(user.getUsername());

            // 如果是管理员离开，应用将终止
            if (sessionId.equals(managerId)) {
                managerId = null;
                logger.warning("Manager has left");
            }

            logger.info("User removed: " + user.getUsername());
        } else {
            // 检查待审核用户
            user = pendingUsers.get(sessionId);
            if (user != null) {
                pendingUsers.remove(sessionId);
                logger.info("Pending user removed: " + user.getUsername());
            }
        }
    }

    /**
     * 检查用户是否为管理员
     * @param sessionId 会话ID
     * @return 是否为管理员
     */
    public boolean isManager(String sessionId) {
        return sessionId != null && sessionId.equals(managerId);
    }

    /**
     * 检查用户是否已连接
     * @param sessionId 会话ID
     * @return 是否已连接
     */
    public boolean isConnectedUser(String sessionId) {
        return connectedUsers.containsKey(sessionId);
    }

    /**
     * 获取所有已连接用户名
     * @return 用户名列表
     */
    public List<String> getConnectedUsernames() {
        List<String> usernames = new ArrayList<>();
        for (User user : connectedUsers.values()) {
            usernames.add(user.getUsername());
        }
        return usernames;
    }

    /**
     * 通过会话ID获取用户
     * @param sessionId 会话ID
     * @return 用户对象
     */
    public User getUserBySessionId(String sessionId) {
        return connectedUsers.get(sessionId);
    }

    /**
     * 通过用户名获取用户
     * @param username 用户名
     * @return 用户对象
     */
    public User getUserByUsername(String username) {
        String sessionId = usernameToSessionId.get(username);
        if (sessionId != null) {
            return connectedUsers.get(sessionId);
        }
        return null;
    }

    /**
     * 获取管理员ID
     * @return 管理员会话ID
     */
    public String getManagerId() {
        return managerId;
    }
}