package com.whiteboard.server;

import com.whiteboard.client.shapes.Shape;
import com.whiteboard.common.model.User;
import com.whiteboard.common.model.WhiteboardState;
import com.whiteboard.common.remote.IWhiteboardClient;
import com.whiteboard.common.remote.IWhiteboardServer;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * WhiteboardServer类实现了IWhiteboardServer接口，提供了白板的远程服务。
 * 它管理用户连接、绘图操作、文件保存和加载等功能。
 */

public class WhiteboardServer implements IWhiteboardServer {
    private static final Logger logger = Logger.getLogger(WhiteboardServer.class.getName());

    // 状态和用户管理
    private WhiteboardState whiteboardState;
    private UserManager userManager;
    private Map<String, IWhiteboardClient> clientCallbacks;

    // 锁定管理
    private Map<String, String> regionLocks; // 区域ID -> 会话ID
    private static final long LOCK_TIMEOUT_MS = 1000;
    private static final int REGION_SIZE = 50; // 像素

    public WhiteboardServer() {
        whiteboardState = new WhiteboardState();
        userManager = new UserManager();
        clientCallbacks = new ConcurrentHashMap<>();
        regionLocks = new ConcurrentHashMap<>();

        // 添加关闭钩子
        addShutdownHook();
    }

    // 用户管理方法实现
    @Override
    public String connectUser(String username, boolean requestAsManager) throws RemoteException {
        logger.info("User connecting: " + username + ", request as manager: " + requestAsManager);

        // 如果请求作为管理员但已有管理员
        if (requestAsManager && userManager.getManagerId() != null) {
            logger.warning("Second manager attempted to connect: " + username);
            return null; // 拒绝连接
        }

        return userManager.connectUser(username, requestAsManager);
    }

    @Override
    public boolean approveUser(String username, String managerId) throws RemoteException {
        logger.info("Manager " + managerId + " approving user: " + username);
        if (userManager.isManager(managerId)) {
            boolean approved = userManager.approveUser(username, managerId);
            if (approved) {
                // 获取用户会话ID
                User user = userManager.getUserByUsername(username);
                if (user != null) {
                    String userSessionId = user.getSessionId();

                    // 通知用户已批准
                    IWhiteboardClient client = clientCallbacks.get(userSessionId);
                    if (client != null) {
                        try {
                            client.notifyManagerDecision(true);
                            logger.info("Notified user " + username + " of approval");
                        } catch (RemoteException e) {
                            logger.warning("Error notifying user of approval: " + e.getMessage());
                        }
                    }

                    // 广播更新的用户列表
                    broadcastUserList();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void disconnectUser(String sessionId) throws RemoteException {
        logger.info("User disconnecting, session: " + sessionId);

        User user = userManager.getUserBySessionId(sessionId);
        if (user != null) {
            if (user.isManager()) {
                // 如果管理员断开连接，通知所有客户端
                notifyManagerLeft();
                logger.info("Manager left, notifying all clients");
            } else {
                userManager.removeUser(sessionId);
                clientCallbacks.remove(sessionId);
                broadcastUserList();
                logger.info("User removed: " + user.getUsername());
            }
        }
    }

    @Override
    public List<String> getConnectedUsers() throws RemoteException {
        return userManager.getConnectedUsernames();
    }

    @Override
    public boolean kickUser(String username, String managerId) throws RemoteException {
        logger.info("Manager " + managerId + " kicking user: " + username);
        if (userManager.isManager(managerId)) {
            User userToKick = userManager.getUserByUsername(username);
            if (userToKick != null && !userToKick.isManager()) {
                String sessionToKick = userToKick.getSessionId();

                // 通知被踢出的用户
                IWhiteboardClient clientToKick = clientCallbacks.get(sessionToKick);
                if (clientToKick != null) {
                    try {
                        clientToKick.notifyKicked();
                    } catch (RemoteException e) {
                        logger.warning("Error notifying kicked user: " + e.getMessage());
                    }
                }

                // 移除用户
                userManager.removeUser(sessionToKick);
                clientCallbacks.remove(sessionToKick);
                broadcastUserList();
                return true;
            }
        }
        return false;
    }

    // 绘图操作方法实现
    @Override
    public void addShape(Shape shape, String sessionId) throws RemoteException {
        logger.info("Adding shape from session: " + sessionId);

        // 检查用户权限
        if (userManager.isConnectedUser(sessionId)) {
            // 添加形状到白板状态
            whiteboardState.addShape(shape);

            // 广播形状给所有客户端
            for (Map.Entry<String, IWhiteboardClient> entry : clientCallbacks.entrySet()) {
                try {
                    // 这里打印日志以确认广播确实在发生
                    logger.info("Broadcasting shape to client: " + entry.getKey());
                    entry.getValue().updateShape(shape);
                } catch (RemoteException e) {
                    logger.warning("Error sending shape update to client: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void removeShape(String shapeId, String sessionId) throws RemoteException {
        logger.info("Removing shape: " + shapeId + " from session: " + sessionId);

        // 检查用户权限
        if (userManager.isConnectedUser(sessionId)) {
            // 从白板状态移除形状
            whiteboardState.removeShape(shapeId);

            // 广播移除操作给所有客户端
            for (Map.Entry<String, IWhiteboardClient> entry : clientCallbacks.entrySet()) {
                if (!entry.getKey().equals(sessionId)) { // 不需要发回给发送者
                    try {
                        entry.getValue().removeShape(shapeId);
                    } catch (RemoteException e) {
                        logger.warning("Error sending shape removal to client: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void clearCanvas(String sessionId) throws RemoteException {
        logger.info("Clearing canvas, requested by session: " + sessionId);

        // 检查用户权限（只有管理员可以清除画布）
        if (userManager.isManager(sessionId)) {
            // 清除白板状态
            whiteboardState.clear();

            // 广播清除操作给所有客户端
            for (Map.Entry<String, IWhiteboardClient> entry : clientCallbacks.entrySet()) {
                if (!entry.getKey().equals(sessionId)) { // 不需要发回给发送者
                    try {
                        entry.getValue().receiveClearCanvas();
                    } catch (RemoteException e) {
                        logger.warning("Error sending canvas clear to client: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public List<Shape> getAllShapes() throws RemoteException {
        return whiteboardState.getShapes();
    }

    // 客户端注册方法实现
    @Override
    public void registerClient(String sessionId, IWhiteboardClient client) throws RemoteException {
        logger.info("Registering client callback for session: " + sessionId);
        try {
            // 验证会话ID
            if (sessionId == null) {
                logger.warning("Attempt to register client with null session ID");
                return;
            }

            // 检查用户权限
            if (!userManager.isConnectedUser(sessionId) && !userManager.isManager(sessionId)) {
                logger.warning("Attempt to register unauthorized client: " + sessionId);
                return;
            }

            // 注册回调
            clientCallbacks.put(sessionId, client);

            // 发送初始状态
            sendInitialState(sessionId);

            // 广播用户列表
            broadcastUserList();

            logger.info("Client registered successfully: " + sessionId);
        } catch (Exception e) {
            logger.severe("Error registering client: " + e.getMessage());
            throw new RemoteException("Failed to register client", e);
        }
    }

    @Override
    public void unregisterClient(String sessionId) throws RemoteException {
        logger.info("Unregistering client callback for session: " + sessionId);
        clientCallbacks.remove(sessionId);
    }

    // 文件操作方法实现
    @Override
    public boolean saveWhiteboard(String filename, String sessionId) throws RemoteException {
        logger.info("Saving whiteboard to: " + filename);

        // 检查用户权限（只有管理员可以保存）
        if (userManager.isManager(sessionId)) {
            try {
                FileOutputStream fileOut = new FileOutputStream(filename);
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                out.writeObject(whiteboardState);
                out.close();
                fileOut.close();
                logger.info("Whiteboard saved successfully");
                return true;
            } catch (IOException e) {
                logger.severe("Error saving whiteboard: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean loadWhiteboard(String filename, String sessionId) throws RemoteException {
        logger.info("Loading whiteboard from: " + filename);

        // 检查用户权限（只有管理员可以加载）
        if (userManager.isManager(sessionId)) {
            try {
                FileInputStream fileIn = new FileInputStream(filename);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                whiteboardState = (WhiteboardState) in.readObject();
                in.close();
                fileIn.close();
                logger.info("Whiteboard loaded successfully");

                // 广播新状态给所有客户端
                for (Map.Entry<String, IWhiteboardClient> entry : clientCallbacks.entrySet()) {
                    try {
                        // 清除当前画布
                        entry.getValue().receiveClearCanvas();

                        // 发送所有形状
                        for (Shape shape : whiteboardState.getShapes()) {
                            entry.getValue().updateShape(shape);
                        }
                    } catch (RemoteException e) {
                        logger.warning("Error sending loaded whiteboard to client: " + e.getMessage());
                    }
                }

                return true;
            } catch (IOException | ClassNotFoundException e) {
                logger.severe("Error loading whiteboard: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    // 聊天功能实现
    @Override
    public void sendChatMessage(String message, String senderSessionId) throws RemoteException {
        logger.info("Chat message from session: " + senderSessionId);

        // 检查用户权限
        if (userManager.isConnectedUser(senderSessionId)) {
            User sender = userManager.getUserBySessionId(senderSessionId);
            String senderName = sender.getUsername();

            // 广播消息给所有客户端
            for (IWhiteboardClient client : clientCallbacks.values()) {
                try {
                    client.receiveMessage(senderName, message);
                } catch (RemoteException e) {
                    logger.warning("Error sending chat message to client: " + e.getMessage());
                }
            }
        }
    }

    // 辅助方法
    private void broadcastUserList() {
        List<String> usernames = userManager.getConnectedUsernames();
        for (IWhiteboardClient client : clientCallbacks.values()) {
            try {
                client.updateUserList(usernames);
            } catch (RemoteException e) {
                logger.warning("Error broadcasting user list: " + e.getMessage());
            }
        }
    }

    private void notifyManagerLeft() {
        for (IWhiteboardClient client : clientCallbacks.values()) {
            try {
                client.notifyManagerLeft();
            } catch (RemoteException e) {
                logger.warning("Error notifying clients that manager left: " + e.getMessage());
            }
        }
    }

    /**
     * 通知用户已被管理员批准
     * @param username 被批准的用户名
     */
    private void notifyUserApproved(String username) {
        // 找到用户的会话ID
        String userSessionId = null;
        for (Map.Entry<String, IWhiteboardClient> entry : clientCallbacks.entrySet()) {
            User user = userManager.getUserBySessionId(entry.getKey());
            if (user != null && user.getUsername().equals(username)) {
                userSessionId = entry.getKey();
                break;
            }
        }

        if (userSessionId != null) {
            IWhiteboardClient client = clientCallbacks.get(userSessionId);
            if (client != null) {
                try {
                    client.notifyManagerDecision(true); // true表示被批准
                    logger.info("Notified user " + username + " that they were approved");
                } catch (RemoteException e) {
                    logger.warning("Error notifying user of approval: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 发送初始白板状态给新连接的客户端
     * @param sessionId 客户端会话ID
     */
    private void sendInitialState(String sessionId) {
        IWhiteboardClient client = clientCallbacks.get(sessionId);
        if (client != null) {
            try {
                // 发送所有现有形状
                List<Shape> shapes = whiteboardState.getShapes();
                for (Shape shape : shapes) {
                    client.updateShape(shape);
                }
                logger.info("Sent initial state to client: " + sessionId + " (" + shapes.size() + " shapes)");
            } catch (RemoteException e) {
                logger.warning("Error sending initial state to client: " + e.getMessage());
            }
        }
    }

    // 并发控制方法
    private String calculateRegionId(int x, int y) {
        int regionX = x / REGION_SIZE;
        int regionY = y / REGION_SIZE;
        return regionX + ":" + regionY;
    }

    /**
     * 处理用户加入请求
     */
    @Override
    public void requestJoin(String username, String sessionId) throws RemoteException {
        logger.info("Join request from user: " + username + ", session: " + sessionId);

        // 更新用户活动时间
        userManager.updateUserActivity(sessionId);

        // 获取用户UID
        String uid = userManager.getUidBySessionId(sessionId);
        if (uid == null) {
            logger.warning("Session not found: " + sessionId);
            return;
        }

        // 检查是否已批准
        if (userManager.isApproved(uid)) {
            // 已批准则直接通知用户
            IWhiteboardClient client = clientCallbacks.get(sessionId);
            if (client != null) {
                try {
                    client.notifyManagerDecision(true);
                } catch (RemoteException e) {
                    logger.warning("Error notifying approved user: " + e.getMessage());
                }
            }
            return;
        }

        // 如果未批准且不在等待列表，则通知管理员
        if (!userManager.isPendingUser(sessionId)) {
            logger.warning("User not in pending list: " + sessionId);
            return;
        }

        // 通知管理员
        notifyManagerAboutPendingUser(username);
    }

    /**
     * 通知管理员有新用户请求加入
     */
    private void notifyManagerAboutPendingUser(String username) {
        String managerId = userManager.getManagerId();
        if (managerId != null) {
            IWhiteboardClient managerClient = clientCallbacks.get(managerId);
            if (managerClient != null) {
                try {
                    // 检查用户是否在线
                    User user = userManager.getUserByUsername(username);
                    boolean isOnline = (user != null &&
                            (System.currentTimeMillis() - user.getLastActivity()) <= 10000);

                    managerClient.notifyPendingJoinRequest(username, isOnline);
                    logger.info("Notified manager about pending user: " + username);
                } catch (RemoteException e) {
                    logger.warning("Failed to notify manager about pending user: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 拒绝用户加入请求
     */
    @Override
    public void rejectUser(String username, String managerId) throws RemoteException {
        logger.info("Manager " + managerId + " rejecting user: " + username);

        if (!userManager.isManager(managerId)) {
            logger.warning("Non-manager attempted to reject user: " + username);
            return;
        }

        // 查找用户会话
        User user = userManager.getUserByUsername(username);
        if (user != null && userManager.isPendingUser(user.getSessionId())) {
            // 通知用户被拒绝
            IWhiteboardClient client = clientCallbacks.get(user.getSessionId());
            if (client != null) {
                try {
                    client.notifyManagerDecision(false);
                    logger.info("Notified user of rejection: " + username);
                } catch (RemoteException e) {
                    logger.warning("Error notifying rejected user: " + e.getMessage());
                }
            }

            // 从等待列表移除用户
            userManager.removeUser(user.getSessionId());
        }
    }

    /**
     * 更新用户活动时间
     */
    @Override
    public void updateUserActivity(String sessionId) throws RemoteException {
        userManager.updateUserActivity(sessionId);
    }

    // 启动服务器
    public static void main(String[] args) {
        try {
            // 创建服务器实例
            WhiteboardServer server = new WhiteboardServer();

            // 导出远程对象
            IWhiteboardServer stub = (IWhiteboardServer) UnicastRemoteObject.exportObject(server, 0);

            // 创建注册表
            int port = 1099; // 默认RMI端口
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
            Registry registry = LocateRegistry.createRegistry(port);

            // 绑定远程对象
            registry.rebind("WhiteboardServer", stub);

            logger.info("WhiteboardServer running on port " + port);
        } catch (Exception e) {
            logger.severe("WhiteboardServer exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    public boolean isManager(String sessionId) throws RemoteException {
        return userManager.isManager(sessionId);
    }

    /**
     * 在服务器关闭前通知所有客户端
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            notifyServerShutdown();
        }));
    }

    /**
     * 通知所有客户端服务器将要关闭
     */
    private void notifyServerShutdown() {
        logger.info("Server shutting down, notifying all clients");

        for (IWhiteboardClient client : clientCallbacks.values()) {
            try {
                client.notifyServerDisconnected();
            } catch (RemoteException e) {
                // 忽略关闭时的异常
            }
        }
    }
}