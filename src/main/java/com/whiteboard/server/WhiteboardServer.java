package com.whiteboard.server;

import com.whiteboard.client.shapes.Shape;
import com.whiteboard.common.model.User;
import com.whiteboard.common.model.WhiteboardState;
import com.whiteboard.common.remote.IWhiteboardClient;
import com.whiteboard.common.remote.IWhiteboardServer;
import com.whiteboard.common.model.WhiteboardSaveData;

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
import java.util.Timer;
import java.util.TimerTask;

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

        // 启动主动心跳检测
        startActiveHeartbeatCheck();

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
        if (!userManager.isManager(managerId)) {
            logger.warning("Non-manager " + managerId + " attempted to approve user: " + username);
            return false;
        }

        // 查找用户并批准
        boolean approved = userManager.approveUser(username, managerId);
        if (!approved) {
            logger.warning("Failed to approve user: " + username);
            return false;
        }

        // 获取用户会话ID
        User user = userManager.getUserByUsername(username);
        if (user == null) {
            logger.warning("Approved user not found: " + username);
            return false;
        }

        String userSessionId = user.getSessionId();
        logger.info("User approved: " + username + ", sessionId: " + userSessionId);

        // 通知用户已批准
        IWhiteboardClient client = clientCallbacks.get(userSessionId);
        if (client == null) {
            // 用户回调尚未注册，将其放入待处理的批准通知队列
            logger.info("Client callback not found for " + username + ", deferring notification");
            // 注: 可以添加队列来处理延迟通知，但这里我们假设客户端会再次发送请求
        } else {
            try {
                client.notifyManagerDecision(true);
                logger.info("Notified user " + username + " of approval");
            } catch (RemoteException e) {
                logger.warning("Error notifying user of approval: " + e.getMessage());
                // 移除可能中断的客户端
                clientCallbacks.remove(userSessionId);
            }
        }

        // 广播更新的用户列表
        broadcastUserList();
        logger.info("User list broadcasted after approval");

        return true;
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

            // 广播形状给所有客户端，带断连检测
            broadcastShapeUpdate(shape);
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

    // Check if user is manager
    if (!userManager.isManager(sessionId)) {
        logger.warning("Non-manager attempted to clear canvas: " + sessionId);
        throw new RemoteException("Only manager can clear canvas");
        // Return early - important!
    }

    // User is manager, proceed with clear operation
    logger.info("Manager authorized to clear canvas, proceeding...");

    // Clear whiteboard state
    whiteboardState.clear();

    // 广播清除命令，带断连检测
    broadcastClearCanvas();
}

    @Override
    public List<Shape> getAllShapes() throws RemoteException {
        return whiteboardState.getShapes();
    }

    // 客户端注册方法实现
    @Override
    public void registerClient(String sessionId, IWhiteboardClient client) throws RemoteException {
        logger.info("Registering client callback for session: " + sessionId);

        // 验证会话ID
        if (sessionId == null) {
            logger.warning("Attempt to register client with null session ID");
            throw new RemoteException("Invalid session ID");
        }

        // 检查用户是否存在
        User user = userManager.getUserBySessionId(sessionId);
        if (user == null) {
            logger.warning("Unknown user trying to register client: " + sessionId);
            throw new RemoteException("Unknown user");
        }

        boolean isUserManager = userManager.isManager(sessionId);
        boolean isApproved = userManager.isApproved(userManager.getUidBySessionId(sessionId));

        // Register callback for all valid users (including unapproved users)
        clientCallbacks.put(sessionId, client);
        logger.info("Client callback registered for: " + user.getUsername() +
                " (Manager: " + isUserManager + ", Approved: " + isApproved + ")");

        // Only send initial state to managers and approved users
        if (isUserManager || isApproved) {
            sendInitialState(sessionId);
        }

        // Only managers and approved users see the user list
        if (isUserManager || isApproved) {
            // 广播用户列表
            broadcastUserList();
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

        if (!userManager.isManager(sessionId)) {
            logger.warning("Non-manager attempted to save: " + sessionId);
            return false;
        }

        try {
            // 确保文件扩展名
            if (!filename.endsWith(".wbd")) {
                filename += ".wbd";
            }

            // 创建保存目录
            File saveDir = new File("whiteboards");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            File saveFile = new File(saveDir, filename);

            // 保存白板状态
            try (FileOutputStream fileOut = new FileOutputStream(saveFile);
                 ObjectOutputStream out = new ObjectOutputStream(fileOut)) {

                // 创建保存数据对象
                WhiteboardSaveData saveData = new WhiteboardSaveData();
                saveData.shapes = whiteboardState.getShapes();
                saveData.version = whiteboardState.getVersion();
                saveData.timestamp = System.currentTimeMillis();
                saveData.createdBy = userManager.getUserBySessionId(sessionId).getUsername();

                out.writeObject(saveData);
                out.flush();

                logger.info("Whiteboard saved successfully to: " + saveFile.getAbsolutePath());
                return true;
            }
        } catch (IOException e) {
            logger.severe("Error saving whiteboard: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean loadWhiteboard(String filename, String sessionId) throws RemoteException {
        logger.info("Loading whiteboard from: " + filename);

        if (!userManager.isManager(sessionId)) {
            logger.warning("Non-manager attempted to load: " + sessionId);
            return false;
        }

        try {
            // 确保文件扩展名
            if (!filename.endsWith(".wbd")) {
                filename += ".wbd";
            }

            File loadFile = new File("whiteboards", filename);
            if (!loadFile.exists()) {
                logger.warning("File not found: " + loadFile.getAbsolutePath());
                return false;
            }

            // 加载白板状态
            try (FileInputStream fileIn = new FileInputStream(loadFile);
                 ObjectInputStream in = new ObjectInputStream(fileIn)) {

                WhiteboardSaveData saveData = (WhiteboardSaveData) in.readObject();

                // 清除当前状态
                whiteboardState.clear();

                // 设置新状态
                for (Shape shape : saveData.shapes) {
                    whiteboardState.addShape(shape);
                }

                logger.info("Whiteboard loaded successfully from: " + loadFile.getAbsolutePath());
                logger.info("Loaded " + saveData.shapes.size() + " shapes, created by: " + saveData.createdBy);

                // 广播清除和新状态给所有客户端
                broadcastFullReload();

                return true;
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.severe("Error loading whiteboard: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
        logger.info("Broadcasting user list: " + usernames);

    // 创建副本避免并发修改异常
    Map<String, IWhiteboardClient> clients = new HashMap<>(clientCallbacks);
    List<String> disconnectedClients = new ArrayList<>();

    for (Map.Entry<String, IWhiteboardClient> entry : clients.entrySet()) {
        String sessionId = entry.getKey();
        IWhiteboardClient client = entry.getValue();

        try {
            // 设置较短的超时时间来快速检测断连
            client.updateUserList(usernames);
        } catch (RemoteException e) {
            logger.warning("Client disconnected during broadcast: " + sessionId + ", error: " + e.getMessage());
            disconnectedClients.add(sessionId);
        }
    }

    // 清理断开连接的客户端
    for (String sessionId : disconnectedClients) {
        handleClientDisconnection(sessionId);
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
        if (client == null) {
            logger.warning("Cannot send initial state - client not registered: " + sessionId);
            return;
        }

        try {
            // 发送所有现有形状
            List<Shape> shapes = whiteboardState.getShapes();
            logger.info("Sending initial state to " + sessionId + ": " + shapes.size() + " shapes");

            for (Shape shape : shapes) {
                client.updateShape(shape);
            }

            // 发送用户列表
            List<String> users = userManager.getConnectedUsernames();
            client.updateUserList(users);

            logger.info("Initial state sent successfully to: " + sessionId);
        } catch (RemoteException e) {
            logger.warning("Error sending initial state to client: " + e.getMessage());
            clientCallbacks.remove(sessionId);
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
            logger.info("User already approved: " + username);

            // 已批准则直接通知用户
            IWhiteboardClient client = clientCallbacks.get(sessionId);
            if (client != null) {
                try {
                    client.notifyManagerDecision(true);
                    logger.info("Re-notified approved user: " + username);
                } catch (RemoteException e) {
                    logger.warning("Error notifying approved user: " + e.getMessage());
                    clientCallbacks.remove(sessionId);
                }
            }
            return;
        }

        // 如果未批准且不在等待列表，则直接返回
        if (!userManager.isPendingUser(sessionId)) {
            logger.warning("User not in pending list: " + sessionId);
            return;
        }

        // 通知管理员有用户请求加入
        notifyManagerAboutPendingUser(username);
    }

    /**
     * 通知管理员有新用户请求加入
     */
    private void notifyManagerAboutPendingUser(String username) {
        String managerId = userManager.getManagerId();
        if (managerId == null) {
            logger.warning("No manager available to notify about pending user: " + username);
            return;
        }

        IWhiteboardClient managerClient = clientCallbacks.get(managerId);
        if (managerClient == null) {
            logger.warning("Manager client callback not found, cannot notify about: " + username);
            return;
        }

        try {
            // 检查用户是否在线
            User user = userManager.getUserByUsername(username);
            boolean isOnline = (user != null &&
                    (System.currentTimeMillis() - user.getLastActivity()) <= 10000);

            managerClient.notifyPendingJoinRequest(username, isOnline);
            logger.info("Notified manager about pending user: " + username + " (online: " + isOnline + ")");
        } catch (RemoteException e) {
            logger.warning("Failed to notify manager about pending user: " + e.getMessage());
            // 考虑从回调列表中移除断开的管理员
            if (e.getCause() instanceof java.net.ConnectException) {
                clientCallbacks.remove(managerId);
                logger.warning("Removed disconnected manager from callbacks");
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

    // 客户端断连处理方法
    private void handleClientDisconnection(String sessionId) {
        logger.info("Handling client disconnection: " + sessionId);

        // 从回调列表移除
        clientCallbacks.remove(sessionId);

        // 从用户管理器移除
        User disconnectedUser = userManager.getUserBySessionId(sessionId);
        if (disconnectedUser != null) {
            logger.info("Removing disconnected user: " + disconnectedUser.getUsername());
            userManager.removeUser(sessionId);

            // 如果是管理员断开，通知所有客户端
            if (disconnectedUser.isManager()) {
                notifyManagerLeft();
            } else {
                // 重新广播用户列表（不包含断开的用户）
                broadcastUserList();
            }
        }
    }

    private void broadcastShapeUpdate(Shape shape) {
        Map<String, IWhiteboardClient> clients = new HashMap<>(clientCallbacks);
        List<String> disconnectedClients = new ArrayList<>();

        for (Map.Entry<String, IWhiteboardClient> entry : clients.entrySet()) {
            String sessionId = entry.getKey();
            IWhiteboardClient client = entry.getValue();

            try {
                client.updateShape(shape);
                logger.fine("Successfully broadcasted shape to client: " + sessionId);
            } catch (RemoteException e) {
                logger.warning("Client disconnected during shape broadcast: " + sessionId);
                disconnectedClients.add(sessionId);
            }
        }

        // 清理断开连接的客户端
        for (String sessionId : disconnectedClients) {
            handleClientDisconnection(sessionId);
        }
    }

    // 带断连检测的清除广播方法
    private void broadcastClearCanvas() {
        Map<String, IWhiteboardClient> clients = new HashMap<>(clientCallbacks);
        List<String> disconnectedClients = new ArrayList<>();

        for (Map.Entry<String, IWhiteboardClient> entry : clients.entrySet()) {
            String sessionId = entry.getKey();
            IWhiteboardClient client = entry.getValue();

            try {
                logger.info("Sending clear canvas to client: " + sessionId);
                client.receiveClearCanvas();
                logger.info("Successfully sent clear canvas to: " + sessionId);
            } catch (RemoteException e) {
                logger.warning("Client disconnected during clear broadcast: " + sessionId);
                disconnectedClients.add(sessionId);
            }
        }

        // 清理断开连接的客户端
        for (String sessionId : disconnectedClients) {
            handleClientDisconnection(sessionId);
        }
    }

    private void performActiveHeartbeatCheck() {
        Map<String, IWhiteboardClient> clients = new HashMap<>(clientCallbacks);
        List<String> disconnectedClients = new ArrayList<>();

        for (Map.Entry<String, IWhiteboardClient> entry : clients.entrySet()) {
            String sessionId = entry.getKey();
            IWhiteboardClient client = entry.getValue();

            try {
                // 主动发送心跳检测
                client.heartbeat();
                logger.fine("Heartbeat response received from: " + sessionId);
            } catch (RemoteException e) {
                logger.warning("Client failed heartbeat check: " + sessionId);
                disconnectedClients.add(sessionId);
            }
        }

        // 处理断开连接的客户端
        for (String sessionId : disconnectedClients) {
            handleClientDisconnection(sessionId);
        }
    }

    private void startActiveHeartbeatCheck() {
        Timer activeHeartbeatTimer = new Timer(true);
        activeHeartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performActiveHeartbeatCheck();
            }
        }, 10000, 10000); // 每10秒进行一次主动检测
    }

    private void broadcastFullReload() {
        logger.info("Broadcasting full whiteboard reload to all clients");

        for (Map.Entry<String, IWhiteboardClient> entry : clientCallbacks.entrySet()) {
            try {
                // 先清除
                entry.getValue().receiveClearCanvas();

                // 再发送所有形状
                List<Shape> shapes = whiteboardState.getShapes();
                for (Shape shape : shapes) {
                    entry.getValue().updateShape(shape);
                }

                logger.info("Sent full reload to client: " + entry.getKey());
            } catch (RemoteException e) {
                logger.warning("Error sending full reload to client: " + e.getMessage());
            }
        }
    }
}