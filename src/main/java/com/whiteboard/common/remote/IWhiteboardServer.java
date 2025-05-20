package com.whiteboard.common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import com.whiteboard.client.shapes.Shape;

public interface IWhiteboardServer extends Remote {
    // 用户管理相关
    String connectUser(String username) throws RemoteException;
    boolean approveUser(String username, String managerId) throws RemoteException;
    void disconnectUser(String sessionId) throws RemoteException;
    List<String> getConnectedUsers() throws RemoteException;

    // 绘图相关
    void addShape(Shape shape, String sessionId) throws RemoteException;
    void clearCanvas(String sessionId) throws RemoteException;
    List<Shape> getAllShapes() throws RemoteException;

    // 文件操作（管理员专属）
    boolean saveWhiteboard(String filename, String sessionId) throws RemoteException;
    boolean loadWhiteboard(String filename, String sessionId) throws RemoteException;

    // 聊天功能
    void sendChatMessage(String message, String senderSessionId) throws RemoteException;
}