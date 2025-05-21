package com.whiteboard.client;

import javax.swing.*;
import java.rmi.RemoteException;

public class JoinWhiteBoard {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java JoinWhiteBoard <serverIPAddress> <serverPort> <username>");
            System.out.println("Running in local mode as guest");

            try {
                WhiteboardClient client = new WhiteboardClient("Guest", false);
            } catch (RemoteException e) {
                System.err.println("Error creating local client: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            String serverIP = args[0];
            int serverPort;
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1]);
                return;
            }
            String username = args[2];

            System.out.println("Connecting to server at " + serverIP + ":" + serverPort + " as " + username);

            try {
                WhiteboardClient client = new WhiteboardClient(username, serverIP, serverPort);
            } catch (RemoteException e) {
                System.err.println("Error connecting to server: " + e.getMessage());
                e.printStackTrace();

                // 提示用户是否要在本地模式运行
                int response = JOptionPane.showConfirmDialog(
                        null,
                        "Failed to connect to server. Would you like to run in local mode?",
                        "Connection Error",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (response == JOptionPane.YES_OPTION) {
                    try {
                        WhiteboardClient localClient = new WhiteboardClient(username, false);
                    } catch (RemoteException ex) {
                        System.err.println("Error creating local client: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}