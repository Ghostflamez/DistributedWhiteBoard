package com.whiteboard.client;

import com.whiteboard.client.shapes.Shape;
import com.whiteboard.client.ui.WhiteboardFrame;
import com.whiteboard.common.remote.IWhiteboardClient;
import com.whiteboard.common.remote.IWhiteboardServer;

import javax.swing.*;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;



public class WhiteboardClient extends UnicastRemoteObject implements IWhiteboardClient, Serializable {
    private static final Logger logger = Logger.getLogger(WhiteboardClient.class.getName());

    private WhiteboardFrame frame = null;
    private IWhiteboardServer server;
    private String username;
    private String sessionId;
    private boolean isManager;
    private boolean isConnected = false;
    private String currentFilename = null;
    private volatile boolean uiInitialized = false;
    private Timer heartbeatTimer;
    private Timer joinRequestTimer;
    private boolean isApproved = false;

    private JDialog joinWaitingDialog;

    private volatile boolean approved = false;
    private final Object approvalLock = new Object();

    // 缓存未处理的更新
    private final List<Shape> pendingShapes = new ArrayList<>();
    private final List<String> pendingShapeRemovals = new ArrayList<>();
    private List<String> pendingUserList = null;
    private final List<ChatMessage> pendingMessages = new ArrayList<>();
    private Boolean pendingManagerDecision = null;
    private boolean pendingManagerLeft = false;
    private boolean pendingKicked = false;
    private boolean pendingClearCanvas = false;

    // 预览相关
    private Timer previewTimer;
    private Shape currentPreviewShape;

    private static class ChatMessage {
        final String sender;
        final String message;

        ChatMessage(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }
    }

    /**
     * 本地模式构造函数（不连接服务器）
     */
    public WhiteboardClient(String username, boolean isManager) throws RemoteException {
        this.username = username;
        this.isManager = isManager;

        // 初始化UI
        initializeUI();
    }

    /**
     * 连网模式构造函数（请求作为管理员连接）
     */

    public WhiteboardClient(String username, String serverAddress, int serverPort, boolean requestAsManager) throws RemoteException {
        this.username = username;

        try {
            // 连接服务器
            connectToServer(serverAddress, serverPort, requestAsManager);

            // 初始化UI
            initializeUI();

    } catch (RuntimeException e) {
        // 将 RuntimeException 转换为 RemoteException 向上抛出
        logger.severe("Error initializing client: " + e.getMessage());
        throw new RemoteException("Failed to initialize client", e);
    } catch (Exception e) {
        logger.severe("Error initializing client: " + e.getMessage());
        throw new RemoteException("Failed to initialize client", e);
    }
}

    /**
     * 连接到服务器
     */
    private void connectToServer(String serverAddress, int serverPort, boolean requestAsManager) {
        try {
            // 获取RMI注册表
            Registry registry = LocateRegistry.getRegistry(serverAddress, serverPort);

            // 查找服务器
            server = (IWhiteboardServer) registry.lookup("WhiteboardServer");


            // 连接用户
            sessionId = server.connectUser(username, requestAsManager);

        // 如果返回null，表示连接被拒绝
        if (sessionId == null) {
            String errorMsg = requestAsManager ?
                "Connection rejected: Another manager is already connected." :
                "Connection rejected by server.";

            // 抛出 RuntimeException 而不是 RemoteException，让调用者处理
            throw new RuntimeException(errorMsg);
        }

            // 确定管理员状态
            isManager = server.isManager(sessionId);
            isConnected = true;

            // 启动心跳
            startHeartbeat();

        // 如果不是管理员，显示等待对话框并启动加入请求
        if (!isManager) {
            createWaitingDialog();
            startJoinRequestTimer();
        } else {
            isApproved = true; // 管理员自动批准
        }

        logger.info("Connected to server as " + (isManager ? "manager" : "regular user") +
                ", isApproved=" + isApproved);
    } catch (RemoteException | NotBoundException e) {
        logger.severe("Could not connect to server: " + e.getMessage());
        isConnected = false;
        // 抛出 RuntimeException，让调用者处理UI显示和程序退出
        throw new RuntimeException("Failed to connect to server: " + e.getMessage(), e);
    }
}


    /**
     * 初始化UI
     */
    private void initializeUI() {
        SwingUtilities.invokeLater(() -> {
            String title = "Distributed Whiteboard - " + username + (isManager ? " (Manager)" : "");
            frame = new WhiteboardFrame(title, isManager, this);

            // 设置白板面板的绘图事件监听器
            frame.getWhiteboardPanel().setDrawingListener(shape -> {
                if (isConnected && (isManager || isApproved)) {
                    try {
                        server.addShape(shape, sessionId);
                    } catch (RemoteException e) {
                        logger.warning("Error sending shape to server: " + e.getMessage());
                    }
                }
            });

            frame.setVisible(true);

            processPendingUpdates();

            // 标记UI已初始化
            uiInitialized = true;

            // 如果是管理员或已批准，注册客户端回调
            if (isConnected) {
                // Always register client callback if connected
                if (isConnected) {
                    try {
                        server.registerClient(sessionId, this);
                        logger.info("Registered client callback with server");

                        // If manager or already approved, get current state
                        if (isManager || isApproved) {
                            // Get current shapes
                            List<Shape> shapes = server.getAllShapes();
                            for (Shape shape : shapes) {
                                frame.getWhiteboardPanel().addShape(shape);
                            }

                            // Get user list
                            List<String> users = server.getConnectedUsers();
                            frame.updateUserList(users);
                        }
                    } catch (RemoteException e) {
                        logger.warning("Error registering client: " + e.getMessage());
                    }
                }
            }

            // 处理所有挂起的更新
            processPendingUpdates();
        });
    }

    /**
     * 处理挂起的更新
     */
    private void processPendingUpdates() {
        if (!uiInitialized || frame == null) {
            return;
        }

        // 处理挂起的形状
        synchronized (pendingShapes) {
            for (Shape shape : pendingShapes) {
                frame.getWhiteboardPanel().addShape(shape);
            }
            pendingShapes.clear();
        }

        // 处理挂起的形状移除
        synchronized (pendingShapeRemovals) {
            for (String shapeId : pendingShapeRemovals) {
                frame.getWhiteboardPanel().removeShape(shapeId);
            }
            pendingShapeRemovals.clear();
        }

        // 处理挂起的用户列表
        if (pendingUserList != null) {
            frame.updateUserList(pendingUserList);
            pendingUserList = null;
        }

        // 处理挂起的聊天消息
        synchronized (pendingMessages) {
            for (ChatMessage msg : pendingMessages) {
                frame.addChatMessage(msg.sender, msg.message);
            }
            pendingMessages.clear();
        }

        // 处理挂起的管理员决定
        if (pendingManagerDecision != null) {
            boolean approved = pendingManagerDecision;
            pendingManagerDecision = null;

            if (approved) {
                JOptionPane.showMessageDialog(frame,
                        "Your request to join has been approved",
                        "Request Approved",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Your request to join has been rejected",
                        "Request Rejected",
                        JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }
        }

        // 处理挂起的管理员离开
        if (pendingManagerLeft) {
            pendingManagerLeft = false;
            JOptionPane.showMessageDialog(frame,
                    "The manager has left the session. The application will now close.",
                    "Session Ended",
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }

        // 处理挂起的踢出
        if (pendingKicked) {
            pendingKicked = false;
            JOptionPane.showMessageDialog(frame,
                    "You have been kicked out by the manager.",
                    "Kicked Out",
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }

        // 处理挂起的清除画布
        if (pendingClearCanvas) {
            pendingClearCanvas = false;
            frame.getWhiteboardPanel().clearCanvas();
        }
    }

    // IWhiteboardClient 接口实现
    @Override
    public void updateShape(Shape shape) throws RemoteException {
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.getWhiteboardPanel().addShape(shape);
            });
        } else {
            // 缓存更新
            synchronized (pendingShapes) {
                pendingShapes.add(shape);
            }
            logger.info("Cached shape update: " + shape.getId());
        }
    }

    @Override
    public void removeShape(String shapeId) throws RemoteException {
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.getWhiteboardPanel().removeShape(shapeId);
            });
        } else {
            // 缓存更新
            synchronized (pendingShapeRemovals) {
                pendingShapeRemovals.add(shapeId);
            }
            logger.info("Cached shape removal: " + shapeId);
        }
    }

    @Override
    public void updateUserList(List<String> users) throws RemoteException {
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.updateUserList(users);
            });
        } else {
            // 缓存更新
            pendingUserList = new ArrayList<>(users);
            logger.info("Cached user list update");
        }
    }

    @Override
    public void receiveMessage(String senderName, String message) throws RemoteException {
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.addChatMessage(senderName, message);
            });
        } else {
            // 缓存更新
            synchronized (pendingMessages) {
                pendingMessages.add(new ChatMessage(senderName, message));
            }
            logger.info("Cached chat message from " + senderName);
        }
    }

    @Override
    public void notifyManagerDecision(boolean approved) throws RemoteException {
        logger.info("Received manager decision: " + (approved ? "Approved" : "Rejected"));

        // 立即设置批准状态
        this.isApproved = approved;

        // 立即停止请求定时器
        if (joinRequestTimer != null) {
            joinRequestTimer.cancel();
            joinRequestTimer = null;
            logger.info("Join request timer stopped after manager decision");
        }

        // 关闭等待对话框（重要！）
        if (joinWaitingDialog != null && joinWaitingDialog.isVisible()) {
            SwingUtilities.invokeLater(() -> {
                joinWaitingDialog.dispose();
                joinWaitingDialog = null;

                if (frame != null) {
                    frame.setEnabled(true); // 重新启用主窗口
                }
            });
        }

        // 在UI线程中显示通知
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                if (approved) {
                    JOptionPane.showMessageDialog(frame,
                            "Your request to join has been approved",
                            "Request Approved",
                            JOptionPane.INFORMATION_MESSAGE);

                    // 关键修改: 批准后立即注册客户端回调并获取当前状态
                    registerAfterApproval();
                } else {
                    JOptionPane.showMessageDialog(frame,
                            "Your request to join has been rejected",
                            "Request Rejected",
                            JOptionPane.WARNING_MESSAGE);

                    // 拒绝后5秒关闭
                    Timer closeTimer = new Timer();
                    closeTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            System.exit(0);
                        }
                    }, 5000);
                }
            });
        } else {
            // 缓存批准通知，但立即处理批准后的注册
            pendingManagerDecision = approved;

            if (approved) {
                // 即使UI未初始化也尝试注册
                registerAfterApproval();
            } else if (!approved) {
                // 被拒绝时退出
                System.exit(0);
            }
        }
    }

    @Override
    public void notifyManagerLeft() throws RemoteException {
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame,
                        "The manager has left the session. The application will now close.",
                        "Session Ended",
                        JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            });
        } else {
            // 缓存更新
            pendingManagerLeft = true;
            logger.info("Cached manager left notification");
        }
    }

    @Override
    public void notifyKicked() throws RemoteException {
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame,
                        "You have been kicked out by the manager.",
                        "Kicked Out",
                        JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            });
        } else {
            // 缓存更新
            pendingKicked = true;
            logger.info("Cached kick notification");
        }
    }

    @Override
    public void receiveClearCanvas() throws RemoteException {
    logger.info("Received clear canvas command from server");

        // Process immediately if UI is ready
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    logger.info("Clearing canvas on UI thread");
                    frame.getWhiteboardPanel().clearCanvas();
                    logger.info("Canvas cleared successfully");
                } catch (Exception e) {
                    logger.severe("Error clearing canvas: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            // 缓存更新
            pendingClearCanvas = true;
            logger.info("Cached clear canvas notification (UI not ready)");
        }
    }

    // 其他方法保持不变...

    /**
     * 发送形状到服务器
     */
    public void sendShape(Shape shape) {
        if (isConnected) {
            try {
                logger.info("Sending shape to server: " + shape.getId());
                server.addShape(shape, sessionId);
            } catch (RemoteException e) {
                logger.warning("Error sending shape to server: " + e.getMessage());
                handleConnectionError(e);
            }
        }
    }

    /**
     * 发送聊天消息
     */
    public void sendChatMessage(String message) {
        if (isConnected) {
            try {
                logger.info("Sending chat message: " + message);
                server.sendChatMessage(message, sessionId);
                // 在本地显示自己的消息
//                if (uiInitialized && frame != null) {
//                    frame.addChatMessage("Me", message);
//                }
            } catch (RemoteException e) {
                logger.warning("Error sending chat message: " + e.getMessage());
                handleConnectionError(e);
            }
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (isConnected) {
            try {
                server.disconnectUser(sessionId);
                isConnected = false;
                logger.info("Disconnected from server");
            } catch (RemoteException e) {
                logger.warning("Error disconnecting from server: " + e.getMessage());
            }
        }
    }

    /**
     * 处理连接错误
     */
    private void handleConnectionError(Exception e) {
        isConnected = false;
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame,
                        "Lost connection to server: " + e.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        } else {
            logger.severe("Lost connection to server: " + e.getMessage());
        }
    }

    /**
     * 踢出用户
     */
    public boolean kickUser(String username) {
        if (isConnected && isManager) {
            try {
                return server.kickUser(username, sessionId);
            } catch (RemoteException e) {
                logger.warning("Error kicking user: " + e.getMessage());
                handleConnectionError(e);
            }
        }
        return false;
    }

    /**
     * 新建白板
     */
    public boolean newWhiteboard() {
        if (isConnected && isManager) {
            try {
                server.clearCanvas(sessionId);
                currentFilename = null;

                // 显示成功消息
                if (uiInitialized && frame != null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame,
                                "New whiteboard created successfully.",
                                "New Whiteboard",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                }
                return true;
            } catch (RemoteException e) {
                logger.warning("Error creating new whiteboard: " + e.getMessage());
                handleConnectionError(e);
            }
        }
        return false;
    }

    /**
     * 保存白板
     */
    public boolean saveWhiteboard() {
        if (!isConnected || !isManager) {
            return false;
        }

        // 如果有当前文件名，直接保存
        if (currentFilename != null && !currentFilename.trim().isEmpty()) {
            try {
                boolean success = server.saveWhiteboard(currentFilename, sessionId);
                if (success) {
                    // 显示成功消息
                    if (uiInitialized && frame != null) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(frame,
                                    "Whiteboard saved successfully.",
                                    "Save Successful",
                                    JOptionPane.INFORMATION_MESSAGE);
                        });
                    }
                } else {
                    // 显示错误消息
                    if (uiInitialized && frame != null) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(frame,
                                    "Failed to save whiteboard.",
                                    "Save Failed",
                                    JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }
                return success;
            } catch (RemoteException e) {
                logger.warning("Error saving whiteboard: " + e.getMessage());
                handleConnectionError(e);
                return false;
            }
        } else {
            // 如果没有当前文件名，调用另存为
            return saveWhiteboardAs(null);
        }
    }

    /**
     * 另存为
     */
    /**
     * 另存为
     */
    public boolean saveWhiteboardAs(String filename) {
        if (!isConnected || !isManager) {
            return false;
        }

        // Save As 总是弹出对话框，即使传入了文件名
        String inputFilename = JOptionPane.showInputDialog(frame,
                "Enter filename:",
                "Save Whiteboard As",
                JOptionPane.PLAIN_MESSAGE);

        if (inputFilename == null || inputFilename.trim().isEmpty()) {
            return false; // 用户取消
        }

        final String finalFilename = inputFilename.trim();

        try {
            boolean success = server.saveWhiteboard(finalFilename, sessionId);
            if (success) {
                this.currentFilename = finalFilename; // 更新当前文件名

                // 显示成功消息
                if (uiInitialized && frame != null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame,
                                "Whiteboard saved successfully as: " + finalFilename,
                                "Save As Successful",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                }
            } else {
                // 显示错误消息
                if (uiInitialized && frame != null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame,
                                "Failed to save whiteboard as: " + finalFilename,
                                "Save As Failed",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
            return success;
        } catch (RemoteException e) {
            logger.warning("Error saving whiteboard: " + e.getMessage());
            handleConnectionError(e);
            return false;
        }
    }

    /**
     * 加载白板
     */
    /**
     * 加载白板
     */
    public boolean loadWhiteboard(String filename) {
        if (!isConnected || !isManager) {
            return false;
        }

        // 如果没有提供文件名，弹出对话框
        if (filename == null) {
            filename = JOptionPane.showInputDialog(frame,
                    "Enter filename to load:",
                    "Load Whiteboard",
                    JOptionPane.PLAIN_MESSAGE);

            if (filename == null || filename.trim().isEmpty()) {
                return false; // 用户取消
            }
            filename = filename.trim();
        }

        // 创建final副本供lambda使用
        final String finalFilename = filename;

        try {
            boolean success = server.loadWhiteboard(finalFilename, sessionId);
            if (success) {
                this.currentFilename = finalFilename;

                // 显示成功消息
                if (uiInitialized && frame != null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame,
                                "Whiteboard loaded successfully: " + finalFilename,
                                "Load Successful",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                }
            } else {
                // 显示错误消息
                if (uiInitialized && frame != null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame,
                                "Failed to load whiteboard: " + finalFilename + "\nFile may not exist.",
                                "Load Failed",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
            return success;
        } catch (RemoteException e) {
            logger.warning("Error loading whiteboard: " + e.getMessage());
            handleConnectionError(e);
            return false;
        }
    }

    /**
     * 检查是否有文件名
     */
    public boolean hasFilename() {
        return currentFilename != null;
    }

    /**
     * 初始化心跳机制
     */
    private void startHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
        }

        heartbeatTimer = new Timer(true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            private int failureCount = 0;
            private final int MAX_FAILURES = 3;

            @Override
            public void run() {
                if (isConnected) {
                    try {
                        // 发送心跳
                        server.updateUserActivity(sessionId);
                        failureCount = 0; // 重置失败计数
                        logger.fine("Heartbeat sent successfully");
                    } catch (RemoteException e) {
                        failureCount++;
                        logger.warning("Heartbeat failed (" + failureCount + "/" + MAX_FAILURES + "): " + e.getMessage());

                        if (failureCount >= MAX_FAILURES) {
                            logger.severe("Multiple heartbeat failures, treating as disconnected");
                            handleConnectionError(e);
                            heartbeatTimer.cancel();
                        }
                    }
                }
            }
        }, 3000, 3000); // 每3秒发送一次心跳，更频繁的检测
    }

    /**
     * 初始化加入请求定时器
     */
    private void startJoinRequestTimer() {
        if (joinRequestTimer != null) {
            joinRequestTimer.cancel();
        }

        joinRequestTimer = new Timer(true);
        joinRequestTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isConnected && !isApproved && !isManager) {
                    sendJoinRequest();
                } else if (isApproved || isManager) {
                    joinRequestTimer.cancel();
                }
            }
        }, 0, 5000); // 每5秒发送一次请求
    }

    /**
     * 发送加入请求
     */
    private void sendJoinRequest() {
        // 关键修改: 增加对批准状态的明确检查
        if (!isApproved) {
            if (isConnected && !isManager && server != null) {
                try {
                    logger.info("Sending join request, approved=" + isApproved);
                    server.requestJoin(username, sessionId);
                } catch (RemoteException e) {
                    logger.warning("Error sending join request: " + e.getMessage());
                    handleConnectionError(e);
                }
            }
        } else {
            // 已批准，确保停止定时器
            if (joinRequestTimer != null) {
                logger.info("Cancelling join request timer as user is already approved");
                joinRequestTimer.cancel();
                joinRequestTimer = null;
            }
        }
    }

    /**
     * 实现新增的通知方法：待处理的加入请求
     */
    @Override
    public void notifyPendingJoinRequest(String username, boolean isOnline) throws RemoteException {
        if (uiInitialized && frame != null && isManager) {
            SwingUtilities.invokeLater(() -> {
                frame.showJoinRequest(username, isOnline);
            });
        }
    }

    /**
     * 实现新增的通知方法：服务器断开连接
     */
    @Override
    public void notifyServerDisconnected() throws RemoteException {
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame,
                        "Lost connection to server. The application will close in 5 seconds.",
                        "Server Disconnected",
                        JOptionPane.ERROR_MESSAGE);

                // 5秒后关闭应用
                Timer closeTimer = new Timer();
                closeTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }, 5000);
            });
        }
    }

    /**
     * 实现心跳响应方法
     */
    @Override
    public void heartbeat() throws RemoteException {
        // 心跳响应，无需具体操作
    }

    /**
     * 批准用户加入
     */
    public boolean approveUser(String username) {
        if (isConnected && isManager) {
            try {
                return server.approveUser(username, sessionId);
            } catch (RemoteException e) {
                logger.warning("Error approving user: " + e.getMessage());
                handleConnectionError(e);
            }
        }
        return false;
    }

    /**
     * 拒绝用户加入
     */
    public boolean rejectUser(String username) {
        if (isConnected && isManager) {
            try {
                server.rejectUser(username, sessionId);
                return true;
            } catch (RemoteException e) {
                logger.warning("Error rejecting user: " + e.getMessage());
                handleConnectionError(e);
            }
        }
        return false;
    }

    /**
     * 新增方法: 批准后注册回调
     */
    private void registerAfterApproval() {
        if (isConnected && isApproved && server != null) {
            try {
                // 注册客户端回调
                server.registerClient(sessionId, this);
                logger.info("Successfully registered client for updates after approval");

                // 获取当前白板状态
                List<Shape> shapes = server.getAllShapes();
                if (shapes != null && !shapes.isEmpty()) {
                    logger.info("Received " + shapes.size() + " shapes from server");

                    if (uiInitialized && frame != null) {
                        SwingUtilities.invokeLater(() -> {
                            for (Shape shape : shapes) {
                                frame.getWhiteboardPanel().addShape(shape);
                            }
                        });
                    } else {
                        // 缓存形状
                        synchronized (pendingShapes) {
                            pendingShapes.addAll(shapes);
                        }
                    }
                }

                // 获取用户列表
                List<String> users = server.getConnectedUsers();
                if (users != null && !users.isEmpty()) {
                    logger.info("Received user list with " + users.size() + " users");

                    if (uiInitialized && frame != null) {
                        SwingUtilities.invokeLater(() -> {
                            frame.updateUserList(users);
                        });
                    } else {
                        pendingUserList = new ArrayList<>(users);
                    }
                }
            } catch (RemoteException e) {
                logger.severe("Error registering client after approval: " + e.getMessage());
                // 不抛出异常，确保UI流程继续
            }
        }
    }

    /**
     * 新增方法: 创建非模态等待对话框
     */
    private void createWaitingDialog() {
        SwingUtilities.invokeLater(() -> {
            // 首先找到主窗口，作为父窗口
            Frame parent = null;
            if (frame != null) {
                parent = frame;
            }

            // 创建模态对话框
            JDialog waitingDialog = new JDialog((Frame)null, "Waiting for Approval", true);
            waitingDialog.setLayout(new BorderLayout());

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JLabel message = new JLabel("Waiting for manager's approval...");
            panel.add(message, BorderLayout.CENTER);

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> {
                int response = JOptionPane.showConfirmDialog(
                        waitingDialog,
                        "Are you sure you want to cancel joining?",
                        "Cancel Join Request",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (response == JOptionPane.YES_OPTION) {
                    // 断开连接并退出
                    disconnect();
                    System.exit(0);
                }
            });

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(cancelButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            waitingDialog.add(panel);
            waitingDialog.pack();
            waitingDialog.setLocationRelativeTo(frame); // 居中显示
            waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            waitingDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    cancelButton.doClick(); // 模拟点击取消按钮
                }
            });

            // 存储引用
            joinWaitingDialog = waitingDialog;

            // 由于模态对话框会阻塞 EDT 线程，所以在新线程中显示它
            new Thread(() -> {
                waitingDialog.setVisible(true);
            }).start();
        });
    }

    /**
     * Send clear canvas command to server
     */
    public void clearCanvas() {
        if (isConnected && isManager) {
            try {
                System.out.println("Sending clear canvas command to server...");
                server.clearCanvas(sessionId);
                System.out.println("Clear canvas command sent successfully");
            } catch (RemoteException e) {
                logger.warning("Error sending clear canvas command: " + e.getMessage());
                handleConnectionError(e);
            }
        } else {
            logger.warning("Only managers can clear the canvas");
            throw new RuntimeException("Only managers can clear the canvas");
        }
    }

    public boolean isManager() {
        return isManager;
    }


    // preview相关方法
    @Override
    public void receivePreviewUpdate(Shape previewShape, String fromUser) throws RemoteException {
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.getWhiteboardPanel().updatePreview(previewShape, fromUser);
            });
        }
    }

    @Override
    public void receivePreviewClear(String fromUser) throws RemoteException {
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.getWhiteboardPanel().clearPreview(fromUser);
            });
        }
    }

    // 发送预览更新
    public void sendPreviewUpdate(Shape shape) {
        if (isConnected && (isManager || isApproved)) {
            try {
                server.updatePreview(shape, sessionId);
            } catch (RemoteException e) {
                logger.warning("Error sending preview update: " + e.getMessage());
            }
        }
    }

    // 清除预览
    public void clearPreview() {
        if (isConnected && (isManager || isApproved)) {
            try {
                server.clearPreview(sessionId);
            } catch (RemoteException e) {
                logger.warning("Error clearing preview: " + e.getMessage());
            }
        }
    }

    @Override
    public void notifyDuplicateUsername(String username) throws RemoteException {
        logger.warning("Duplicate username detected: " + username);

        // 停止心跳和加入请求
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
        if (joinRequestTimer != null) {
            joinRequestTimer.cancel();
            joinRequestTimer = null;
        }

        // 关闭等待对话框
        if (joinWaitingDialog != null && joinWaitingDialog.isVisible()) {
            SwingUtilities.invokeLater(() -> {
                joinWaitingDialog.dispose();
                joinWaitingDialog = null;
            });
        }

        // 显示错误并退出
        if (uiInitialized && frame != null) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame,
                        "Username '" + username + "' is already connected to the whiteboard.\nConnection request rejected.",
                        "Duplicate Username",
                        JOptionPane.ERROR_MESSAGE);

                // 2秒后退出，给用户时间看到消息
                Timer exitTimer = new Timer();
                exitTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }, 2000);
            });
        } else {
            // UI未初始化，直接退出
            System.err.println("Username '" + username + "' is already connected. Exiting.");
            System.exit(0);
        }
    }
}