package com.whiteboard.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class WhiteboardFrame extends JFrame {
    private WhiteboardPanel whiteboardPanel;
    private ToolPanel toolPanel;
    private ColorPanel colorPanel;
    private boolean isManager = false;

    public WhiteboardFrame(String title, boolean isManager) {
        super(title);
        this.isManager = isManager;
        initComponents();
        setupUI();
        setupWindowListener();
    }

    private void initComponents() {
        whiteboardPanel = new WhiteboardPanel();
        toolPanel = new ToolPanel(whiteboardPanel);
        colorPanel = new ColorPanel(whiteboardPanel);
    }

    private void setupUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

        // 添加工具栏
        add(toolPanel, BorderLayout.NORTH);

        // 添加颜色面板
        add(colorPanel, BorderLayout.SOUTH);

        // 添加画布
        JScrollPane scrollPane = new JScrollPane(whiteboardPanel);
        add(scrollPane, BorderLayout.CENTER);

        // 使窗口居中显示
        setLocationRelativeTo(null);
    }

    private void setupWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });
    }

    private void handleWindowClosing() {
        int response = JOptionPane.showConfirmDialog(
                this,
                "确定要退出白板应用吗？",
                "确认退出",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            // 这里将来会添加网络断开连接的代码
            dispose();
            System.exit(0);
        }
    }

    public WhiteboardPanel getWhiteboardPanel() {
        return whiteboardPanel;
    }
}