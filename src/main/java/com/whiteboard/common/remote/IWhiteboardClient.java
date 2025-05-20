package com.whiteboard.common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import com.whiteboard.client.shapes.Shape;

public interface IWhiteboardClient extends Remote {
    // 回调方法，服务器推送更新
    void updateShape(Shape shape) throws RemoteException;
    void updateUserList(List<String> users) throws RemoteException;
    void receiveMessage(String senderName, String message) throws RemoteException;
    void notifyManagerLeft() throws RemoteException;
    void receiveClearCanvas() throws RemoteException;
}