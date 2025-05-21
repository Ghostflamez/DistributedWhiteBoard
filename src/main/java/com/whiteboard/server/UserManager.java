package com.whiteboard.server;

import com.whiteboard.common.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class UserManager {
    private static final Logger logger = Logger.getLogger(UserManager.class.getName());

    private String managerId = null;
    private Map<String, User> connectedUsers;       // sessionId -> User
    private Map<String, User> pendingUsers;         // sessionId -> User
    private Map<String, String> sessionToUid;       // sessionId -> uid
    private Map<String, String> usernameToUid;      // username -> uid
    private Set<String> approvedUids;               // 已批准的UID集合

    private final long USER_TIMEOUT_MS = 10000;     // 10秒
    private final long MANAGER_TIMEOUT_MS = 15000;  // 15秒

    public UserManager() {
        connectedUsers = new ConcurrentHashMap<>();
        pendingUsers = new ConcurrentHashMap<>();
        sessionToUid = new ConcurrentHashMap<>();
        usernameToUid = new ConcurrentHashMap<>();
        approvedUids = new HashSet<>();

        // 启动连接监控线程
        startConnectionMonitor();
    }

    /**
     * 连接用户，如果是第一个用户则成为管理员
     * @param username 用户名
     * @return 会话ID，如果用户名冲突则返回null
     */
    public synchronized String connectUser(String username) {
        // 生成唯一UID
        String uid = UUID.randomUUID().toString();

        // 生成唯一会话ID
        String sessionId = UUID.randomUUID().toString();

        // 检查用户名是否已存在
        if (usernameToUid.containsKey(username)) {
            // 用户名冲突处理
            String originalUsername = username;
            int suffix = 1;
            while (usernameToUid.containsKey(username)) {
                username = originalUsername + "_" + suffix++;
            }
            logger.info("Username conflict resolved: " + originalUsername + " -> " + username);
        }

        // 第一个连接的用户成为管理员
        boolean isManager = (managerId == null);

        User user = new User(uid, username, isManager);
        user.setSessionId(sessionId);

        if (isManager) {
            // 管理员直接连接
            managerId = sessionId;
            connectedUsers.put(sessionId, user);
            sessionToUid.put(sessionId, uid);
            usernameToUid.put(username, uid);
            approvedUids.add(uid);  // 管理员自动批准
            logger.info("Manager connected: " + username + ", UID: " + uid);
        } else {
            // 其他用户需要管理员批准
            pendingUsers.put(sessionId, user);
            sessionToUid.put(sessionId, uid);
            logger.info("User pending approval: " + username + ", UID: " + uid);
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
        String targetUid = null;
        String pendingSessionId = null;

        for (Map.Entry<String, User> entry : pendingUsers.entrySet()) {
            if (entry.getValue().getUsername().equals(username)) {
                pendingSessionId = entry.getKey();
                targetUid = entry.getValue().getUid();
                break;
            }
        }

        if (pendingSessionId != null && targetUid != null) {
            User user = pendingUsers.get(pendingSessionId);
            pendingUsers.remove(pendingSessionId);
            connectedUsers.put(pendingSessionId, user);
            approvedUids.add(targetUid);  // 添加到已批准列表
            logger.info("User approved: " + username + ", UID: " + targetUid);
            return true;
        }

        logger.warning("User not found in pending list: " + username);
        return false;
    }

    /**
     * 检查用户是否已被批准
     * @param uid 用户UID
     * @return 是否已批准
     */
    public boolean isApproved(String uid) {
        return approvedUids.contains(uid);
    }

    /**
     * 从sessionId获取UID
     * @param sessionId 会话ID
     * @return UID
     */
    public String getUidBySessionId(String sessionId) {
        return sessionToUid.get(sessionId);
    }

    /**
     * 根据用户名获取UID
     * @param username 用户名
     * @return UID，如不存在返回null
     */
    public String getUidByUsername(String username) {
        return usernameToUid.get(username);
    }

    /**
     * 移除用户
     * @param sessionId 会话ID
     */
    public synchronized void removeUser(String sessionId) {
        User user = connectedUsers.get(sessionId);

        if (user != null) {
            connectedUsers.remove(sessionId);
            String uid = sessionToUid.get(sessionId);
            sessionToUid.remove(sessionId);
            usernameToUid.remove(user.getUsername());

            // 如果是管理员离开，应用将终止
            if (sessionId.equals(managerId)) {
                managerId = null;
                logger.warning("Manager has left");
            }

            logger.info("User removed: " + user.getUsername() + ", UID: " + uid);
        } else {
            // 检查待审核用户
            user = pendingUsers.get(sessionId);
            if (user != null) {
                pendingUsers.remove(sessionId);
                String uid = sessionToUid.get(sessionId);
                sessionToUid.remove(sessionId);
                usernameToUid.remove(user.getUsername());
                logger.info("Pending user removed: " + user.getUsername() + ", UID: " + uid);
            }
        }
    }

    /**
     * 更新用户活动时间
     * @param sessionId 会话ID
     */
    public void updateUserActivity(String sessionId) {
        User user = connectedUsers.get(sessionId);
        if (user != null) {
            user.updateActivity();
        } else {
            user = pendingUsers.get(sessionId);
            if (user != null) {
                user.updateActivity();
            }
        }
    }

    /**
     * 启动连接监控线程
     */
    private void startConnectionMonitor() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkConnectionStatus();
            }
        }, 5000, 5000); // 每5秒检查一次
    }

    /**
     * 检查所有用户的连接状态
     */
    private synchronized void checkConnectionStatus() {
        long currentTime = System.currentTimeMillis();
        List<String> sessionsToRemove = new ArrayList<>();

        // 检查管理员状态
        if (managerId != null) {
            User manager = connectedUsers.get(managerId);
            if (manager != null && (currentTime - manager.getLastActivity()) > MANAGER_TIMEOUT_MS) {
                logger.warning("Manager timeout detected");
                sessionsToRemove.add(managerId);
            }
        }

        // 检查已连接用户
        for (Map.Entry<String, User> entry : connectedUsers.entrySet()) {
            String sessionId = entry.getKey();
            if (sessionId.equals(managerId)) continue; // 跳过管理员

            User user = entry.getValue();
            if ((currentTime - user.getLastActivity()) > USER_TIMEOUT_MS) {
                logger.info("User timeout detected: " + user.getUsername());
                sessionsToRemove.add(sessionId);
            }
        }

        // 检查等待批准的用户
        for (Map.Entry<String, User> entry : pendingUsers.entrySet()) {
            String sessionId = entry.getKey();
            User user = entry.getValue();
            if ((currentTime - user.getLastActivity()) > USER_TIMEOUT_MS) {
                logger.info("Pending user timeout detected: " + user.getUsername());
                sessionsToRemove.add(sessionId);
            }
        }

        // 移除超时用户
        for (String sessionId : sessionsToRemove) {
            removeUser(sessionId);
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
     * 检查用户是否在等待批准
     * @param sessionId 会话ID
     * @return 是否在等待批准
     */
    public boolean isPendingUser(String sessionId) {
        return pendingUsers.containsKey(sessionId);
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
     * 获取所有等待批准的用户名
     * @return 用户名列表
     */
    public List<String> getPendingUsernames() {
        List<String> usernames = new ArrayList<>();
        for (User user : pendingUsers.values()) {
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
        User user = connectedUsers.get(sessionId);
        if (user == null) {
            user = pendingUsers.get(sessionId);
        }
        return user;
    }

    /**
     * 通过用户名获取用户
     * @param username 用户名
     * @return 用户对象
     */
    public User getUserByUsername(String username) {
        String uid = usernameToUid.get(username);
        if (uid == null) return null;

        // 检查已连接用户
        for (User user : connectedUsers.values()) {
            if (user.getUid().equals(uid)) {
                return user;
            }
        }

        // 检查等待用户
        for (User user : pendingUsers.values()) {
            if (user.getUid().equals(uid)) {
                return user;
            }
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

    /**
     * 检查用户名是否存在（包括已连接和等待批准的用户）
     * @param username 用户名
     * @return 是否存在
     */
    public boolean isUsernameExists(String username) {
        return usernameToUid.containsKey(username);
    }
}