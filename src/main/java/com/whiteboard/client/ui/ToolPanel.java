package com.whiteboard.client.ui;

import com.whiteboard.client.tools.*;

import javax.swing.*;
import java.awt.*;

public class ToolPanel extends JToolBar {
    private WhiteboardPanel whiteboardPanel;
    private JComboBox<String> strokeWidthCombo;
    private JComboBox<String> fontSizeCombo;

    public ToolPanel(WhiteboardPanel whiteboardPanel) {
        this.whiteboardPanel = whiteboardPanel;
        setFloatable(false);
        initializeTools();
    }

    private void initializeTools() {
        // 铅笔工具按钮
        JButton pencilButton = new JButton("铅笔");
        pencilButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new PencilTool(Color.BLACK, 2)));
        add(pencilButton);

        // 线条按钮
        JButton lineButton = new JButton("线条");
        lineButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new LineTool(Color.BLACK, 2)));
        add(lineButton);

        // 矩形按钮
        JButton rectButton = new JButton("矩形");
        rectButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new RectangleTool(Color.BLACK, 2)));
        add(rectButton);

        // 椭圆按钮
        JButton ovalButton = new JButton("椭圆");
        ovalButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new OvalTool(Color.BLACK, 2)));
        add(ovalButton);

        // 三角形按钮
        JButton triangleButton = new JButton("三角形");
        triangleButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new TriangleTool(Color.BLACK, 2)));
        add(triangleButton);

        // 文本按钮
        JButton textButton = new JButton("文本");
        textButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new TextTool(Color.BLACK, new Font("Arial", Font.PLAIN, 14))));
        add(textButton);

        // 橡皮擦按钮
        JButton eraserButton = new JButton("橡皮擦");
        eraserButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new EraserTool(20)));
        add(eraserButton);

        // 清除按钮
        JButton clearButton = new JButton("清除画布");
        clearButton.addActionListener(e -> whiteboardPanel.clearCanvas());
        add(clearButton);

        // 添加分隔符
        addSeparator();

        // 线宽选择
        add(new JLabel("线宽:"));
        strokeWidthCombo = new JComboBox<>(new String[]{"细", "中", "粗", "特粗"});
        strokeWidthCombo.setSelectedIndex(1); // 默认选中"中"
        strokeWidthCombo.addActionListener(e -> {
            int width;
            switch (strokeWidthCombo.getSelectedIndex()) {
                case 0: width = 1; break;   // 细
                case 1: width = 2; break;   // 中
                case 2: width = 4; break;   // 粗
                case 3: width = 8; break;   // 特粗
                default: width = 2;
            }
            whiteboardPanel.setCurrentStrokeWidth(width);
        });
        add(strokeWidthCombo);

        // 添加分隔符
        addSeparator();

        // 字体大小选择
        add(new JLabel("字体大小:"));
        fontSizeCombo = new JComboBox<>(new String[]{"小", "中", "大", "特大"});
        fontSizeCombo.setSelectedIndex(1); // 默认选中"中"
        fontSizeCombo.addActionListener(e -> {
            int size;
            switch (fontSizeCombo.getSelectedIndex()) {
                case 0: size = 12; break;  // 小
                case 1: size = 14; break;  // 中
                case 2: size = 18; break;  // 大
                case 3: size = 24; break;  // 特大
                default: size = 14;
            }
            Font currentFont = new Font("Arial", Font.PLAIN, size);
            whiteboardPanel.setCurrentFont(currentFont);
        });
        add(fontSizeCombo);
    }
}