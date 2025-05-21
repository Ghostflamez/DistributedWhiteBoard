package com.whiteboard.client;

import com.whiteboard.client.ui.WhiteboardFrame;

import javax.swing.*;

public class WhiteboardClient {
    private WhiteboardFrame frame;
    private String username;
    private boolean isManager;

    public WhiteboardClient(String username, boolean isManager) {
        this.username = username;
        this.isManager = isManager;
        initialize();
    }

    private void initialize() {
        // 确保在EDT线程中创建UI
        SwingUtilities.invokeLater(() -> {
            String title = "Distributed whiteboard - " + username + (isManager ? " (Manager)" : "");
            frame = new WhiteboardFrame(title, isManager);
            frame.setVisible(true);
        });
    }

    public static void main(String[] args) {
        // 默认以本地模式启动，管理员身份
        WhiteboardClient client = new WhiteboardClient("LocalUser", true);
    }
}