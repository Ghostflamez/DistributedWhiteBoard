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
import java.util.List;
import java.util.logging.Logger;

/**
 * WhiteboardClient类实现了IWhiteboardClient接口，负责与服务器进行通信并更新UI。
 * 它可以在本地模式下临时运行，也可以连接到远程服务器。
 */

public class WhiteboardClient extends UnicastRemoteObject implements IWhiteboardClient, Serializable {
    private static final Logger logger = Logger.getLogger(WhiteboardClient.class.getName());

    private WhiteboardFrame frame;
    private IWhiteboardServer server;
    private String username;
    private String sessionId;
    private boolean isManager;
    private boolean isConnected = false;
    private String currentFilename = null;

    /**
     * 本地模式构造函数（不连接服务器）
     */
    public WhiteboardClient(String username, boolean isManager) throws RemoteException {
        this.username = username;
        this.isManager = isManager;
        initializeUI();
    }

    /**
     * 连网模式构造函数
     */
    public WhiteboardClient(String username, String serverAddress, int serverPort) throws RemoteException {
        this.username = username;

        try {
            // 连接服务器
            connectToServer(serverAddress, serverPort);

            // 初始化UI
            initializeUI();

            // 如果已连接服务器，注册客户端回调
            if (isConnected) {
                server.registerClient(sessionId, this);

                // 检索并显示当前的所有形状
                List<Shape> shapes = server.getAllShapes();
                SwingUtilities.invokeLater(() -> {
                    for (Shape shape : shapes) {
                        frame.getWhiteboardPanel().addShape(shape);
                    }
                });
            }
        } catch (Exception e) {
            logger.severe("Error initializing client: " + e.getMessage());
            throw new RemoteException("Failed to initialize client", e);
        }
    }

    /**
     * 连接到服务器
     */
    private void connectToServer(String serverAddress, int serverPort) {
        try {
            // 获取RMI注册表
            Registry registry = LocateRegistry.getRegistry(serverAddress, serverPort);

            // 查找服务器
            server = (IWhiteboardServer) registry.lookup("WhiteboardServer");

            // 连接用户
            sessionId = server.connectUser(username);

            // 确定是否为管理员
            isManager = sessionId != null && server.isManager(sessionId);

            // 重要：注册回调
            server.registerClient(sessionId, this);
            isConnected = true;

            logger.info("Connected to server as " + (isManager ? "manager" : "regular user"));
        } catch (RemoteException | NotBoundException e) {
            logger.severe("Could not connect to server: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Error connecting to server: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            isConnected = false;
        }
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        SwingUtilities.invokeLater(() -> {
            String title = "Distributed Whiteboard - " + username + (isManager ? " (Manager)" : "");
            frame = new WhiteboardFrame(title, isManager, this);
            frame.setVisible(true);

            // 设置白板面板的绘图事件监听器
            frame.getWhiteboardPanel().setDrawingListener(shape -> {
                if (isConnected) {
                    try {
                        server.addShape(shape, sessionId);
                    } catch (RemoteException e) {
                        logger.warning("Error sending shape to server: " + e.getMessage());
                    }
                }
            });
        });
    }

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
     * 发送清除画布请求
     */
    public void clearCanvas() {
        if (isConnected && isManager) {
            try {
                server.clearCanvas(sessionId);
            } catch (RemoteException e) {
                logger.warning("Error clearing canvas: " + e.getMessage());
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
                frame.addChatMessage("Me", message);
            } catch (RemoteException e) {
                logger.warning("Error sending chat message: " + e.getMessage());
                handleConnectionError(e);
            }
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
                // 在服务器清除画布
                server.clearCanvas(sessionId);
                currentFilename = null;
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
        if (isConnected && isManager && currentFilename != null) {
            try {
                return server.saveWhiteboard(currentFilename, sessionId);
            } catch (RemoteException e) {
                logger.warning("Error saving whiteboard: " + e.getMessage());
                handleConnectionError(e);
            }
        }
        return false;
    }

    /**
     * 另存为
     */
    public boolean saveWhiteboardAs(String filename) {
        if (isConnected && isManager) {
            try {
                boolean success = server.saveWhiteboard(filename, sessionId);
                if (success) {
                    this.currentFilename = filename;
                }
                return success;
            } catch (RemoteException e) {
                logger.warning("Error saving whiteboard: " + e.getMessage());
                handleConnectionError(e);
            }
        }
        return false;
    }

    /**
     * 加载白板
     */
    public boolean loadWhiteboard(String filename) {
        if (isConnected && isManager) {
            try {
                boolean success = server.loadWhiteboard(filename, sessionId);
                if (success) {
                    this.currentFilename = filename;
                }
                return success;
            } catch (RemoteException e) {
                logger.warning("Error loading whiteboard: " + e.getMessage());
                handleConnectionError(e);
            }
        }
        return false;
    }

    /**
     * 检查是否有文件名
     */
    public boolean hasFilename() {
        return currentFilename != null;
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
        JOptionPane.showMessageDialog(frame,
                "Lost connection to server: " + e.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
    }

    // IWhiteboardClient 接口实现
    @Override
    public void updateShape(Shape shape) throws RemoteException {
        logger.info("Received shape update: " + shape.getId());
        SwingUtilities.invokeLater(() -> {
            frame.getWhiteboardPanel().addShape(shape);
        });
    }

    @Override
    public void removeShape(String shapeId) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            frame.getWhiteboardPanel().removeShape(shapeId);
        });
    }

    @Override
    public void updateUserList(List<String> users) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            frame.updateUserList(users);
        });
    }

    @Override
    public void receiveMessage(String senderName, String message) throws RemoteException {
        logger.info("Received message from " + senderName + ": " + message);
        SwingUtilities.invokeLater(() -> {
            frame.addChatMessage(senderName, message);
        });
    }

    @Override
    public void notifyManagerDecision(boolean approved) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
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
        });
    }

    @Override
    public void notifyManagerLeft() throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame,
                    "The manager has left the session. The application will now close.",
                    "Session Ended",
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        });
    }

    @Override
    public void notifyKicked() throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame,
                    "You have been kicked out by the manager.",
                    "Kicked Out",
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        });
    }

    @Override
    public void receiveClearCanvas() throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            frame.getWhiteboardPanel().clearCanvas();
        });
    }
}