package com.whiteboard.client.ui;

import com.whiteboard.client.tools.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.logging.Logger;
import com.whiteboard.client.WhiteboardClient;

public class ToolPanel extends JToolBar {
    private WhiteboardPanel whiteboardPanel;
    private JSlider strokeWidthSlider;
    private JTextField strokeWidthField;
    private JPanel strokePreviewPanel; // 用于显示线宽预览
    // At the top of each class file:
    private static final Logger logger = Logger.getLogger(ToolPanel.class.getName());

    public ToolPanel(WhiteboardPanel whiteboardPanel) {
        this.whiteboardPanel = whiteboardPanel;
        setFloatable(false);
        initializeTools();
    }

    private void initializeTools() {
        // 铅笔工具按钮
        JButton pencilButton = new JButton("Pencil");
        pencilButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new PencilTool(whiteboardPanel.getCurrentColor(), getCurrentStrokeWidth())));
        add(pencilButton);

        // 线条按钮
        JButton lineButton = new JButton("Line");
        lineButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new LineTool(whiteboardPanel.getCurrentColor(), getCurrentStrokeWidth())));
        add(lineButton);

        // 矩形按钮
        JButton rectButton = new JButton("Rectangle");
        rectButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new RectangleTool(whiteboardPanel.getCurrentColor(), getCurrentStrokeWidth())));
        add(rectButton);

        // 椭圆按钮
        JButton ovalButton = new JButton("Oval");
        ovalButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new OvalTool(whiteboardPanel.getCurrentColor(), getCurrentStrokeWidth())));
        add(ovalButton);

        // 三角形按钮
        JButton triangleButton = new JButton("Triangle");
        triangleButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new TriangleTool(whiteboardPanel.getCurrentColor(), getCurrentStrokeWidth())));
        add(triangleButton);

        // 文本按钮
        JButton textButton = new JButton("Text");
        textButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new TextTool(whiteboardPanel.getCurrentColor(),
                        new Font("Arial", Font.PLAIN, getCurrentFontSize()))));
        add(textButton);

        // 橡皮擦按钮 - 简化版本
        JButton eraserButton = new JButton("Eraser");
        eraserButton.addActionListener(e -> {
            // 创建橡皮擦工具，大小为当前笔刷大小的2倍（最小8像素）
            int eraserSize = Math.max(getCurrentStrokeWidth() * 2, 8);
            EraserTool eraserTool = new EraserTool(eraserSize, Color.WHITE);
            whiteboardPanel.setCurrentTool(eraserTool);
            logger.info("Eraser tool activated with size: " + eraserSize);
        });
        add(eraserButton);

        // 清除按钮
        JButton clearButton = new JButton("Clear All");
clearButton.addActionListener(e -> {
    // 首先检查权限，而不是直接清除本地画布
    Window window = SwingUtilities.getWindowAncestor(whiteboardPanel);
    if (window instanceof WhiteboardFrame) {
        WhiteboardFrame frame = (WhiteboardFrame) window;
        WhiteboardClient client = frame.getClient();

                if (client != null && client.isManager()) {
                    // 管理员：显示确认对话框
                    int response = JOptionPane.showConfirmDialog(whiteboardPanel,
                            "Clear all drawings?\nThis action cannot be undone.",
                            "Clear Canvas - Confirm",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);

                    if (response == JOptionPane.YES_OPTION) {
                        try {
                            logger.info("Manager clearing canvas locally and sending to server");

                // 先清除本地画布
                whiteboardPanel.clearCanvas();

                // 然后发送清除命令到服务器
                client.clearCanvas();

                logger.info("Clear canvas command sent successfully");
            } catch (Exception ex) {
                logger.severe("Error sending clear canvas command: " + ex.getMessage());
                ex.printStackTrace();

                            // 如果发送到服务器失败，显示错误消息
                            JOptionPane.showMessageDialog(whiteboardPanel,
                                    "Error sending clear command to server: " + ex.getMessage(),
                                    "Network Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else if (client != null && !client.isManager()) {
                    // 非管理员：只显示权限错误
                    logger.warning("Non-manager attempted to clear canvas");
                    JOptionPane.showMessageDialog(whiteboardPanel,
                            "Only the manager can clear the canvas.",
                            "Permission Denied",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    // 本地模式：直接清除本地画布
                    int response = JOptionPane.showConfirmDialog(whiteboardPanel,
                            "Clear all drawings?\nThis action cannot be undone.",
                            "Clear Canvas - Confirm",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);

                    if (response == JOptionPane.YES_OPTION) {
                        logger.info("Local mode: clearing canvas locally");
                        whiteboardPanel.clearCanvas();
                    }
                }
            } else {
                // 如果无法获取客户端引用，默认为本地模式
                int response = JOptionPane.showConfirmDialog(whiteboardPanel,
                        "Clear all drawings?\nThis action cannot be undone.",
                        "Clear Canvas - Confirm",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (response == JOptionPane.YES_OPTION) {
                    logger.info("Unable to get client reference, clearing locally");
                    whiteboardPanel.clearCanvas();
                }
            }
        });
        add(clearButton);

        // 添加分隔符
        addSeparator();

        // 线宽控制
        setupStrokeWidthControl();
    }

    private void setupStrokeWidthControl() {
        JPanel strokePanel = new JPanel(new BorderLayout(5, 0));
        strokePanel.add(new JLabel("Brush Size:"), BorderLayout.WEST);

        // 线宽滑块
        strokeWidthSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 2);
        strokeWidthSlider.setPreferredSize(new Dimension(100, 20));
        strokeWidthSlider.setMajorTickSpacing(5);
        strokeWidthSlider.setMinorTickSpacing(1);
        strokeWidthSlider.setPaintTicks(true);

        // 线宽文本输入框
        strokeWidthField = new JTextField(2);
        strokeWidthField.setText(String.valueOf(strokeWidthSlider.getValue()));
        strokeWidthField.setHorizontalAlignment(SwingConstants.CENTER);

        // 线宽预览面板
        strokePreviewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // 设置抗锯齿
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // 获取当前线宽和颜色
                int width = getCurrentStrokeWidth();
                Color color = whiteboardPanel.getCurrentColor();

                // 绘制预览圆形
                g2d.setColor(color);

                // 根据笔刷大小计算圆形直径
                int diameter = width*2;

                // 居中绘制
                int Brush_x = (getWidth() - diameter) / 2;
                int Brush_y = (getHeight() - diameter) / 2;

                g2d.fillOval(Brush_x, Brush_y, diameter, diameter);
            }
        };
        strokePreviewPanel.setPreferredSize(new Dimension(40, 40));
        strokePreviewPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        // 线宽滑块事件监听
        strokeWidthSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int width = strokeWidthSlider.getValue();
                strokeWidthField.setText(String.valueOf(width));
                updateStrokeWidth(width);
                strokePreviewPanel.repaint();
            }
        });

        // 线宽文本框事件监听
        strokeWidthField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processStrokeWidthInput();
            }
        });

        strokeWidthField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                processStrokeWidthInput();
            }
        });

        JPanel rightPanel = new JPanel(new BorderLayout(5, 0));
        rightPanel.add(strokeWidthField, BorderLayout.CENTER);
        rightPanel.add(strokePreviewPanel, BorderLayout.EAST);

        strokePanel.add(strokeWidthSlider, BorderLayout.CENTER);
        strokePanel.add(rightPanel, BorderLayout.EAST);

        add(strokePanel);
    }

    private void processStrokeWidthInput() {
        try {
            // 尝试解析输入值
            String input = strokeWidthField.getText().trim();

            // 检查是否包含小数点
            double doubleValue = Double.parseDouble(input);

            // 四舍五入到整数
            int width = (int) Math.round(doubleValue);

            // 确保值大于0
            width = Math.max(1, width);

            // 确保不超过滑块最大值
            width = Math.min(width, strokeWidthSlider.getMaximum());

            // 更新滑块和字段
            strokeWidthSlider.setValue(width);
            strokeWidthField.setText(String.valueOf(width));

            // 更新工具
            updateStrokeWidth(width);

            // 更新预览
            strokePreviewPanel.repaint();
        } catch (NumberFormatException ex) {
            // 如果输入无效，恢复为当前滑块值
            strokeWidthField.setText(String.valueOf(strokeWidthSlider.getValue()));
        }
    }

    private void updateStrokeWidth(int width) {
        whiteboardPanel.setCurrentStrokeWidth(width);

        // 如果当前工具已存在，更新其线宽
        if (whiteboardPanel.getCurrentTool() != null &&
                !(whiteboardPanel.getCurrentTool() instanceof TextTool)) {
            // 当前工具的类型
            DrawingTool currentTool = whiteboardPanel.getCurrentTool();

            // 基于当前工具类型创建新工具，保持颜色但更新线宽
            if (currentTool instanceof PencilTool) {
                whiteboardPanel.setCurrentTool(
                        new PencilTool(whiteboardPanel.getCurrentColor(), width));
            } else if (currentTool instanceof LineTool) {
                whiteboardPanel.setCurrentTool(
                        new LineTool(whiteboardPanel.getCurrentColor(), width));
            } else if (currentTool instanceof RectangleTool) {
                whiteboardPanel.setCurrentTool(
                        new RectangleTool(whiteboardPanel.getCurrentColor(), width));
            } else if (currentTool instanceof OvalTool) {
                whiteboardPanel.setCurrentTool(
                        new OvalTool(whiteboardPanel.getCurrentColor(), width));
            } else if (currentTool instanceof TriangleTool) {
                whiteboardPanel.setCurrentTool(
                        new TriangleTool(whiteboardPanel.getCurrentColor(), width));
            } else if (currentTool instanceof EraserTool) {
                // 更新橡皮擦大小，保持当前模式
                EraserTool oldEraserTool = (EraserTool) currentTool;
                int eraserSize = Math.max(width * 2, 8); // 橡皮擦大小为笔刷的2倍，最小8像素
                EraserTool newEraserTool = new EraserTool(eraserSize, Color.WHITE);

                // 如果当前点存在，保留它
                if (oldEraserTool.getCurrentPoint() != null) {
                    newEraserTool.setCurrentPoint(oldEraserTool.getCurrentPoint());
                }

                whiteboardPanel.setCurrentTool(newEraserTool);
            }
        }
        // 更新预览
        strokePreviewPanel.repaint();
    }

    public int getCurrentStrokeWidth() {
        return strokeWidthSlider.getValue();
    }

    public int getCurrentFontSize() {
        // 不再使用文本字段，直接返回默认字体大小
        return 14; // 默认值
    }

    public void updatePreview() {
        if (strokePreviewPanel != null) {
            strokePreviewPanel.repaint();
        }
    }
}