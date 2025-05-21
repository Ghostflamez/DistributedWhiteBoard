package com.whiteboard.client;

import com.whiteboard.client.ui.WhiteboardFrame;

import javax.swing.*;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        private Font findBestUnicodeFont() {
            // 首选字体列表（按优先级排序）
            String[] preferredFonts = {
                    "Arial Unicode MS",
                    "MS Gothic",
                    "Microsoft YaHei",
                    "SimSun",
                    "Noto Sans",
                    "DejaVu Sans",
                    "Dialog"
            };

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] availableFonts = ge.getAvailableFontFamilyNames();

            // 转换为Set以加速查找
            Set<String> availableFontSet = new HashSet<>(Arrays.asList(availableFonts));

            // 查找第一个可用的优先字体
            for (String fontName : preferredFonts) {
                if (availableFontSet.contains(fontName)) {
                    return new Font(fontName, Font.PLAIN, 14);
                }
            }

            // 如果没有找到任何优先字体，返回默认字体
            return new Font("Dialog", Font.PLAIN, 14);

    }

    public static void main(String[] args) {
        // 默认以本地模式启动，管理员身份
        WhiteboardClient client = new WhiteboardClient("LocalUser", true);


    }
}