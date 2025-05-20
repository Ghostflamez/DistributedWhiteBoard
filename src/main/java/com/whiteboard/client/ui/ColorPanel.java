package com.whiteboard.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ColorPanel extends JPanel {
    private WhiteboardPanel whiteboardPanel;

    public ColorPanel(WhiteboardPanel whiteboardPanel) {
        this.whiteboardPanel = whiteboardPanel;
        setLayout(new GridLayout(2, 8));
        initializeColors();
    }

    private void initializeColors() {
        // 基本颜色
        addColorButton(Color.BLACK);
        addColorButton(Color.DARK_GRAY);
        addColorButton(Color.GRAY);
        addColorButton(Color.LIGHT_GRAY);
        addColorButton(Color.WHITE);
        addColorButton(Color.RED);
        addColorButton(Color.PINK);
        addColorButton(Color.ORANGE);

        // 更多颜色
        addColorButton(Color.YELLOW);
        addColorButton(Color.GREEN);
        addColorButton(new Color(0, 128, 0)); // 深绿色
        addColorButton(Color.CYAN);
        addColorButton(Color.BLUE);
        addColorButton(new Color(0, 0, 128)); // 深蓝色
        addColorButton(Color.MAGENTA);
        addColorButton(new Color(128, 0, 128)); // 紫色
    }

    private void addColorButton(Color color) {
        JButton button = new JButton();
        button.setBackground(color);
        button.setPreferredSize(new Dimension(24, 24));
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        button.addActionListener(e -> whiteboardPanel.setCurrentColor(color));

        add(button);
    }
}