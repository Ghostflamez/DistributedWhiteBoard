package com.whiteboard.common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import com.whiteboard.client.shapes.Shape;
import com.whiteboard.common.model.DrawOperation;
import com.whiteboard.common.model.User;

public interface IWhiteboardServer extends Remote {
    // 用户管理方法
    String connectUser(String username) throws RemoteException;
    boolean approveUser(String username, String managerId) throws RemoteException;
    void disconnectUser(String sessionId) throws RemoteException;
    List<String> getConnectedUsers() throws RemoteException;
    boolean kickUser(String username, String managerId) throws RemoteException;

    /**
     * 检查会话是否属于管理员
     * @param sessionId 会话ID
     * @return 如果是管理员会话返回true，否则返回false
     * @throws RemoteException RMI异常
     */
    boolean isManager(String sessionId) throws RemoteException;

    // 绘图操作方法
    void addShape(Shape shape, String sessionId) throws RemoteException;
    void removeShape(String shapeId, String sessionId) throws RemoteException;
    void clearCanvas(String sessionId) throws RemoteException;
    List<Shape> getAllShapes() throws RemoteException;

    // 客户端注册方法
    void registerClient(String sessionId, IWhiteboardClient client) throws RemoteException;
    void unregisterClient(String sessionId) throws RemoteException;

    // 文件操作方法
    boolean saveWhiteboard(String filename, String sessionId) throws RemoteException;
    boolean loadWhiteboard(String filename, String sessionId) throws RemoteException;

    // 聊天功能
    void sendChatMessage(String message, String senderSessionId) throws RemoteException;
}