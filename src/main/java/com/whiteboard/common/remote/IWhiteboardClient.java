package com.whiteboard.common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import com.whiteboard.client.shapes.Shape;

public interface IWhiteboardClient extends Remote {
    // 原有方法
    void updateShape(Shape shape) throws RemoteException;
    void removeShape(String shapeId) throws RemoteException;
    void updateUserList(List<String> users) throws RemoteException;
    void receiveMessage(String senderName, String message) throws RemoteException;
    void notifyManagerDecision(boolean approved) throws RemoteException;
    void notifyManagerLeft() throws RemoteException;
    void notifyKicked() throws RemoteException;
    void receiveClearCanvas() throws RemoteException;

    // 新增方法
    void notifyPendingJoinRequest(String username, boolean isOnline) throws RemoteException;
    void notifyServerDisconnected() throws RemoteException;
    void heartbeat() throws RemoteException; // 心跳检测

    // 新增预览回调
    void receivePreviewUpdate(Shape previewShape, String fromUser) throws RemoteException;
    void receivePreviewClear(String fromUser) throws RemoteException;
    // 添加预览开始回调
    void receivePreviewStart(Shape previewShape, String fromUser, long timestamp) throws RemoteException;

    void notifyDuplicateUsername(String username) throws RemoteException;


}