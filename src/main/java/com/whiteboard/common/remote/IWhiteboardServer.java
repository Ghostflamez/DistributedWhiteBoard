package com.whiteboard.common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import com.whiteboard.client.shapes.Shape;

public interface IWhiteboardServer extends Remote {
    // 修改连接方法
    String connectUser(String username, boolean requestAsManager) throws RemoteException;

    // 其他现有方法保持不变
    boolean approveUser(String username, String managerId) throws RemoteException;
    void disconnectUser(String sessionId) throws RemoteException;
    List<String> getConnectedUsers() throws RemoteException;
    boolean kickUser(String username, String managerId) throws RemoteException;
    boolean isManager(String sessionId) throws RemoteException;
    void addShape(Shape shape, String sessionId) throws RemoteException;
    void removeShape(String shapeId, String sessionId) throws RemoteException;
    void clearCanvas(String sessionId) throws RemoteException;
    List<Shape> getAllShapes() throws RemoteException;
    void registerClient(String sessionId, IWhiteboardClient client) throws RemoteException;
    void unregisterClient(String sessionId) throws RemoteException;
    boolean saveWhiteboard(String filename, String sessionId) throws RemoteException;
    boolean loadWhiteboard(String filename, String sessionId) throws RemoteException;
    void sendChatMessage(String message, String senderSessionId) throws RemoteException;

    // 新增方法
    void requestJoin(String username, String sessionId) throws RemoteException;
    void rejectUser(String username, String managerId) throws RemoteException;
    void updateUserActivity(String sessionId) throws RemoteException;

    // 新增预览方法
    void updatePreview(Shape previewShape, String sessionId) throws RemoteException;
    void clearPreview(String sessionId) throws RemoteException;
}