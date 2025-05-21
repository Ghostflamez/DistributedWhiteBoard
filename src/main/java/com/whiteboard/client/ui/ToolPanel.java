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

public class ToolPanel extends JToolBar {
    private WhiteboardPanel whiteboardPanel;
    private JSlider strokeWidthSlider;
    private JTextField strokeWidthField;
    private JTextField fontSizeField; // 字体大小输入框
    private JPanel strokePreviewPanel; // 用于显示线宽预览

    public ToolPanel(WhiteboardPanel whiteboardPanel) {
        this.whiteboardPanel = whiteboardPanel;
        setFloatable(false);
        initializeTools();
    }

    private void initializeTools() {
        // 铅笔工具按钮
        JButton pencilButton = new JButton("Pencil");
        pencilButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new PencilTool(Color.BLACK, getCurrentStrokeWidth())));
        add(pencilButton);

        // 线条按钮
        JButton lineButton = new JButton("Line");
        lineButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new LineTool(Color.BLACK, getCurrentStrokeWidth())));
        add(lineButton);

        // 矩形按钮
        JButton rectButton = new JButton("Rectangle");
        rectButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new RectangleTool(Color.BLACK, getCurrentStrokeWidth())));
        add(rectButton);

        // 椭圆按钮
        JButton ovalButton = new JButton("Oval");
        ovalButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new OvalTool(Color.BLACK, getCurrentStrokeWidth())));
        add(ovalButton);

        // 三角形按钮
        JButton triangleButton = new JButton("Triangle");
        triangleButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new TriangleTool(Color.BLACK, getCurrentStrokeWidth())));
        add(triangleButton);

        // 文本按钮
        JButton textButton = new JButton("Text");
        textButton.addActionListener(e ->
                whiteboardPanel.setCurrentTool(new TextTool(Color.BLACK,
                        new Font("Arial", Font.PLAIN, getCurrentFontSize()))));
        add(textButton);

        // 橡皮擦按钮
        setupEraserButton();

        // 清除按钮
        JButton clearButton = new JButton("Clear All");
        clearButton.addActionListener(e -> whiteboardPanel.clearCanvas());
        add(clearButton);

        // 添加分隔符
        addSeparator();

        // 线宽控制
        setupStrokeWidthControl();

        // 添加分隔符
        addSeparator();

        // 字体大小控制
        setupFontSizeControl();
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

    private void setupFontSizeControl() {
        JPanel fontPanel = new JPanel(new BorderLayout(5, 0));
        fontPanel.add(new JLabel("Font Size:"), BorderLayout.WEST);

        // 字体大小文本输入框
        fontSizeField = new JTextField(3);
        fontSizeField.setText("14"); // 默认字体大小
        fontSizeField.setHorizontalAlignment(SwingConstants.CENTER);

        // 字体大小文本框事件监听
        fontSizeField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processFontSizeInput();
            }
        });

        fontSizeField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                processFontSizeInput();
            }
        });

        fontPanel.add(fontSizeField, BorderLayout.CENTER);

        add(fontPanel);
    }

    private void processFontSizeInput() {
        try {
            // 尝试解析输入值
            String input = fontSizeField.getText().trim();

            // 检查是否包含小数点
            double doubleValue = Double.parseDouble(input);

            // 四舍五入到整数
            int size = (int) Math.round(doubleValue);

            // 确保值大于0
            size = Math.max(1, size);

            // 可选：设置一个合理的最大值
            size = Math.min(size, 72); // 假设72为最大字体大小

            // 更新字段
            fontSizeField.setText(String.valueOf(size));

            // 更新字体
            Font currentFont = new Font("Arial", Font.PLAIN, size);
            whiteboardPanel.setCurrentFont(currentFont);

            // 如果当前工具是文本工具，更新其字体
            if (whiteboardPanel.getCurrentTool() instanceof TextTool) {
                TextTool textTool = (TextTool) whiteboardPanel.getCurrentTool();
                whiteboardPanel.setCurrentTool(
                        new TextTool(whiteboardPanel.getCurrentColor(), currentFont));
            }
        } catch (NumberFormatException ex) {
            // 如果输入无效，恢复为默认值或当前值
            int currentSize = 14; // 默认值
            if (whiteboardPanel.getCurrentFont() != null) {
                currentSize = whiteboardPanel.getCurrentFont().getSize();
            }
            fontSizeField.setText(String.valueOf(currentSize));
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
                EraserTool newEraserTool = new EraserTool(Math.toIntExact(Math.round(width * 1.25))); // 橡皮擦尺寸为线宽的1.25倍
                // 设置橡皮擦模式
                newEraserTool.setMode(oldEraserTool.getMode());
                whiteboardPanel.setCurrentTool(newEraserTool);
                // 如果当前点存在，保留它
                if (oldEraserTool.getCurrentPoint() != null) {
                    newEraserTool.setCurrentPoint(oldEraserTool.getCurrentPoint());
                }
            }
        }
        // 更新预览
        strokePreviewPanel.repaint();
    }

    // 在ToolPanel类中添加橡皮擦控制
    private void setupEraserButton() {
        JButton eraserButton = new JButton("ERASER");

        // 创建橡皮擦选项面板
        JPopupMenu eraserMenu = new JPopupMenu();
        JPanel eraserPanel = new JPanel(new BorderLayout(5, 5));
        eraserPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 橡皮擦模式单选按钮
        JPanel modePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        modePanel.setBorder(BorderFactory.createTitledBorder("Eraser Mode"));

        ButtonGroup modeGroup = new ButtonGroup();
        JRadioButton objectModeButton = new JRadioButton(EraserTool.EraseMode.OBJECT.getDisplayName());
        JRadioButton freeModeButton = new JRadioButton(EraserTool.EraseMode.FREE.getDisplayName());

        // 禁用对象擦除模式按钮
        objectModeButton.setEnabled(false);

        freeModeButton.setSelected(true); // 默认选中自由擦除模式

        modeGroup.add(objectModeButton);
        modeGroup.add(freeModeButton);

        modePanel.add(objectModeButton);
        modePanel.add(freeModeButton);

        // 应用按钮
        JButton applyButton = new JButton("Apply");

        // 事件监听
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 创建橡皮擦并设置模式
                createEraserWithCurrentSettings(
                        objectModeButton.isSelected() ?
                                EraserTool.EraseMode.OBJECT : EraserTool.EraseMode.FREE);

                // 关闭菜单
                eraserMenu.setVisible(false);
            }
        });

        // 组装面板
        eraserPanel.add(modePanel, BorderLayout.CENTER);
        eraserPanel.add(applyButton, BorderLayout.SOUTH);

        eraserMenu.add(eraserPanel);

        // 左键点击 - 快速使用默认模式橡皮擦
        eraserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 使用对象擦除模式
                createEraserWithCurrentSettings(EraserTool.EraseMode.FREE);
            }
        });

        // 右键点击 - 显示模式选择菜单
        eraserButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    eraserMenu.show(eraserButton, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    eraserMenu.show(eraserButton, e.getX(), e.getY());
                }
            }
        });

        add(eraserButton);
    }

    // 辅助方法：使用当前笔刷大小创建橡皮擦
    private void createEraserWithCurrentSettings(EraserTool.EraseMode mode) {
        int currentSize = getCurrentStrokeWidth();
        int eraserSize = (int) Math.round(currentSize * 1.25); // 橡皮擦尺寸为笔刷大小的1.25倍
        EraserTool eraserTool = new EraserTool(eraserSize);
        eraserTool.setMode(mode);
        whiteboardPanel.setCurrentTool(eraserTool);
    }


    public int getCurrentStrokeWidth() {
        return strokeWidthSlider.getValue();
    }

    public int getCurrentFontSize() {
        try {
            return Integer.parseInt(fontSizeField.getText());
        } catch (NumberFormatException e) {
            return 14; // 默认值
        }
    }

    public void updatePreview() {
        if (strokePreviewPanel != null) {
            strokePreviewPanel.repaint();
        }
    }
}