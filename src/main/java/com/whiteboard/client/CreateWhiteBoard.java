package com.whiteboard.client;

import javax.swing.*;
import java.rmi.RemoteException;

// CreateWhiteBoard.java
public class CreateWhiteBoard {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java CreateWhiteBoard <serverIPAddress> <serverPort> <username>");
            System.exit(1); // 直接退出，不显示UI
        }

        String serverIP = args[0];
        String serverPortStr = args[1];
        String username = args[2];

        int serverPort;
        try {
            serverPort = Integer.parseInt(serverPortStr);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + serverPortStr);
            System.exit(1);
            return; // 让编译器知道程序会退出
        }

        System.out.println("Connecting to server at " + serverIP + ":" + serverPort + " as " + username);

        try {
            // 请求作为管理员连接
            WhiteboardClient client = new WhiteboardClient(username, serverIP, serverPort, true);
            // 连接成功，客户端会自动显示UI
        } catch (RemoteException | RuntimeException e) {
            System.err.println("Error connecting to server: " + e.getMessage());

            // 显示错误对话框，但提供明确的选择
            String[] options = {"Run in Local Mode", "Exit"};
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Failed to connect to server at " + serverIP + ":" + serverPort +
                    "\n\nError: " + e.getMessage() +
                    "\n\nWould you like to run in local mode instead?",
                    "Connection Failed",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null,
                    options,
                    options[1] // 默认选择 "Exit"
            );

            if (choice == 0) { // Run in Local Mode
                try {
                    System.out.println("Starting in local mode...");
                    new WhiteboardClient(username, true);
                } catch (RemoteException ex) {
                    System.err.println("Error creating local client: " + ex.getMessage());
                    JOptionPane.showMessageDialog(null,
                            "Failed to start local mode: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            } else {
                // 用户选择退出
                System.out.println("User chose to exit.");
                System.exit(0);
            }
        }
    }
}

