package com.whiteboard.client.ui;
import com.whiteboard.client.shapes.ErasureShape;
import com.whiteboard.client.shapes.Shape;
import com.whiteboard.client.shapes.Text;
import com.whiteboard.client.shapes.Rectangle;
import com.whiteboard.client.shapes.Oval;
import com.whiteboard.client.shapes.Line;
import com.whiteboard.client.shapes.Triangle;
import com.whiteboard.client.WhiteboardClient;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;


import com.whiteboard.client.tools.*;
import java.util.ArrayList;

public class WhiteboardPanel extends JPanel {
    private List<Shape> shapes;
    private DrawingTool currentTool;
    private Color currentColor;
    private int currentStrokeWidth;
    private Font currentFont;
    private ToolPanel toolPanel;
    private Point currentPoint;
    private Consumer<Shape> drawingListener;

    // 预览功能相关 - 暂时禁用以解决显示问题
    private Map<String, Shape> userPreviews = new HashMap<>(); // username -> preview shape
    private boolean isDrawing = false; // 标记是否正在绘制
    private boolean enablePreview = false; // 暂时禁用预览功能

    private static final Logger logger = Logger.getLogger(WhiteboardPanel.class.getName());




    public WhiteboardPanel() {
        shapes = new ArrayList<>();
        currentColor = Color.BLACK;
        currentStrokeWidth = 2;
        currentFont = new Font("Arial Unicode MS", Font.PLAIN, 14);

        // 默认工具为铅笔（自由绘制）
        currentTool = new PencilTool(currentColor, currentStrokeWidth);

        setBackground(Color.WHITE);
        setupMouseListeners();

        // 添加透明度测试
//        testTransparency();
    }

    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentTool instanceof TextTool) {
                    // 弹出文本输入对话框
                    TextTool textTool = (TextTool) currentTool;
                    textTool.mousePressed(e.getPoint());
                    showTextInputDialog(textTool);
                    repaint(); // 确保立即显示光标和预览
                } else {
                    // 其他工具处理
                    currentTool.mousePressed(e.getPoint());
                    isDrawing = true; // 开始绘制
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentTool instanceof TextTool) {
                    // 文本工具不处理拖动
                    return;
                } else {
                    // 其他工具处理
                    currentTool.mouseDragged(e.getPoint());

                    // 暂时禁用预览功能
                    // if (enablePreview && isDrawing) {
                    //     Shape previewShape = currentTool.getCreatedShape();
                    //     if (previewShape != null) {
                    //         sendPreviewUpdate(previewShape);
                    //     }
                    // }

                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentTool instanceof TextTool) {
                    // 文本工具处理...
                    TextTool textTool = (TextTool) currentTool;
                    Shape textShape = textTool.getCreatedShape();
                    if (textShape != null) {
                        // 发送到服务器，等待服务器返回统一时间戳的版本
                        if (drawingListener != null) {
                            drawingListener.accept(textShape);
                        }
                    }
                } else {
                    // 其他工具处理
                    currentTool.mouseReleased(e.getPoint());

                    // 清除预览
                    if (isDrawing) {
                        if (enablePreview) {
                            clearPreview();
                        }
                        isDrawing = false;
                    }

                    Shape shape = currentTool.getCreatedShape();
                    if (shape != null) {
                        // 发送到服务器
                        if (drawingListener != null) {
                            drawingListener.accept(shape);
                        }

                        // 重置橡皮擦工具
                        if (currentTool instanceof EraserTool) {
                            EraserTool eraserTool = (EraserTool) currentTool;
                            eraserTool.resetErasureShape();
                        }
                    }
                    repaint();
                }
            }
            // 添加鼠标移动监听
            @Override
            public void mouseMoved(MouseEvent e) {
                // 如果当前工具是橡皮擦，更新位置
                if (currentTool instanceof EraserTool) {
                    EraserTool eraserTool = (EraserTool) currentTool;
                    // 仅更新当前位置，不添加到路径
                    eraserTool.setCurrentPoint(e.getPoint());
                    repaint();
                }
            }

        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    // ================== 预览功能相关方法 ==================

    /**
     * 更新其他用户的预览
     */
    public void updatePreview(Shape previewShape, String fromUser) {
        if (enablePreview) {
            logger.fine("Updating preview from user: " + fromUser);
            userPreviews.put(fromUser, previewShape);
            repaint();
        }
    }

    /**
     * 清除特定用户的预览
     */
    public void clearPreview(String fromUser) {
        if (enablePreview) {
            logger.fine("Clearing preview from user: " + fromUser);
            userPreviews.remove(fromUser);
            repaint();
        }
    }

    /**
     * 发送预览更新到服务器
     */
    private void sendPreviewUpdate(Shape shape) {
        if (!enablePreview) return;

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof WhiteboardFrame) {
            WhiteboardFrame frame = (WhiteboardFrame) window;
            WhiteboardClient client = frame.getClient();
            if (client != null) {
                client.sendPreviewUpdate(shape);
            }
        }
    }

    /**
     * 清除预览
     */
    private void clearPreview() {
        if (!enablePreview) return;

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof WhiteboardFrame) {
            WhiteboardFrame frame = (WhiteboardFrame) window;
            WhiteboardClient client = frame.getClient();
            if (client != null) {
                client.clearPreview();
            }
        }
    }

    // ================== 橡皮擦处理方法 ==================

//    private void processEraser(EraserTool eraserTool) {
//        if (eraserTool.getCurrentPoint() == null) return;
//
//        Point p = eraserTool.getCurrentPoint();
//        int size = eraserTool.getEraserSize();
//
//        if (eraserTool.getMode() == EraserTool.EraseMode.OBJECT) {
//            // 对象擦除模式
//            //processObjectEraser(eraserTool, p, size);
//        } else {
//            // 自由擦除模式
//            processFreeEraser(eraserTool);
//        }
//
//        repaint();
//    }

    // 修改WhiteboardPanel中的processFreeEraser方法
//    private void processFreeEraser(EraserTool eraserTool) {
//        List<Point> path = eraserTool.getErasePath();
//        if (path.size() < 2) return;
//
//        int size = eraserTool.getEraserSize();
//
//        // 鼠标释放时，创建一个擦除形状
//        if (!path.isEmpty() &&
//                eraserTool.getCurrentPoint().equals(path.get(path.size() - 1))) {
//
//            // 创建一个使用背景色的擦除形状
//            ErasureShape erasureShape = new ErasureShape(
//                    new ArrayList<>(path), // 复制当前路径
//                    size,
//                    getBackground() // 使用画布背景色
//            );
//
//            // 添加到形状列表
//            shapes.add(erasureShape);
//
//            // 清除路径以便下次擦除
//            eraserTool.clearPath();
//        }
//
//        repaint();
//    }

    // ================== 文本输入对话框 ==================

    // 修改文本输入对话框方法，使用固定字体确保支持Unicode字符
    private void showTextInputDialog(TextTool textTool) {
        // 创建文本输入对话框
        JDialog dialog = new JDialog();
        dialog.setTitle("Text Tool");
        dialog.setModal(false);
        dialog.setLayout(new BorderLayout());

        // 找到当前窗口并设置相对位置
        if (SwingUtilities.getWindowAncestor(this) != null) {
            dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        } else {
            dialog.setLocationRelativeTo(null); // 居中显示
        }

        // 使用固定的多语言支持字体
        // 这些字体都有很好的Unicode支持
        final Font UNICODE_FONT = new Font("Arial Unicode MS", Font.PLAIN, 14);
        final Font FALLBACK_FONT = new Font("Dialog", Font.PLAIN, 14); // 备用字体

        // 文本输入区域 - 确保支持Unicode
        JTextArea textArea = new JTextArea(5, 20);
        try {
            textArea.setFont(UNICODE_FONT);
        } catch (Exception e) {
            textArea.setFont(FALLBACK_FONT); // 如果首选字体不可用，使用备用字体
        }
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.enableInputMethods(true); // 确保启用输入法

        JScrollPane scrollPane = new JScrollPane(textArea);

        // 简化后的字体大小控制面板
        JPanel fontPanel = new JPanel(new BorderLayout(5, 0));
        JPanel fontControls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 字体大小控制
        JLabel fontSizeLabel = new JLabel("Font Size:");
        JTextField fontSizeField = new JTextField(String.valueOf(textTool.getFont().getSize()), 3);

        fontControls.add(fontSizeLabel);
        fontControls.add(fontSizeField);

        // 字体大小变化监听
        fontSizeField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processFontSizeChange(fontSizeField, textTool);
            }
        });

        fontSizeField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                processFontSizeChange(fontSizeField, textTool);
            }
        });

        fontPanel.add(fontControls, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        // 文本区域的keyup事件监听，实时更新预览
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePreview();
            }

            private void updatePreview() {
                textTool.setText(textArea.getText());
                repaint(); // 更新画布上的预览
            }
        });

        // 确定按钮动作
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textTool.setText(textArea.getText());
                textTool.finishEditing();

                // 如果创建了有效的文本对象，则添加到画布
                Shape textShape = textTool.getCreatedShape();
                if (textShape != null && drawingListener != null) {
                    drawingListener.accept(textShape);
                }

                dialog.dispose();
                repaint();
            }
        });

        // 取消按钮动作
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textTool.finishEditing();
                dialog.dispose();
                repaint();
            }
        });

        // 组装对话框
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(fontPanel, BorderLayout.NORTH);

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(controlPanel, BorderLayout.NORTH);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setVisible(true);
    }

    // 修改处理字体大小变更的方法，保持固定字体系列
    private void processFontSizeChange(JTextField fontSizeField, TextTool textTool) {
        try {
            String input = fontSizeField.getText().trim();
            double doubleValue = Double.parseDouble(input);
            int size = (int) Math.round(doubleValue);
            size = Math.max(1, size);
            size = Math.min(size, 72); // 最大字体大小为72

            fontSizeField.setText(String.valueOf(size));

            // 使用固定字体系列，仅改变大小
            Font newFont = new Font("Arial Unicode MS", Font.PLAIN, size);
            textTool.setFont(newFont);
            repaint(); // 更新预览
        } catch (NumberFormatException ex) {
            // 如果输入无效，恢复为当前字体大小
            fontSizeField.setText(String.valueOf(textTool.getFont().getSize()));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制所有形状
        for (Shape shape : shapes) {
            shape.draw(g2d);
        }

        // 暂时禁用其他用户预览的绘制
        if (enablePreview) {
            Composite originalComposite = g2d.getComposite();
            for (Map.Entry<String, Shape> entry : userPreviews.entrySet()) {
                Shape previewShape = entry.getValue();
                if (previewShape != null) {
                    // 设置透明度
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                    previewShape.draw(g2d);
                }
            }
            // 恢复透明度
            g2d.setComposite(originalComposite);
        }

        // 绘制当前正在创建的形状
        if (currentTool != null) {
            if (currentTool instanceof TextTool) {
                TextTool textTool = (TextTool) currentTool;
                if (textTool.isEditing()) {
                    // 绘制文本预览
                    Shape textShape = textTool.getCreatedShape();
                    if (textShape != null) {
                        textShape.draw(g2d);

                        // 绘制文本光标
                        if (textShape instanceof Text) {
                            Text text = (Text) textShape;
                            Point p = text.getStartPoint();
                            FontMetrics metrics = g2d.getFontMetrics(text.getFont());
                            int textWidth = metrics.stringWidth(text.getText());

                            // 闪烁光标效果
                            if (System.currentTimeMillis() % 1000 < 500) {
                                g2d.setColor(Color.BLACK);
                                g2d.drawLine(p.x + textWidth, p.y - metrics.getAscent(),
                                        p.x + textWidth, p.y + metrics.getDescent());
                            }
                        }
                    }
                }
            } else if (currentTool instanceof EraserTool) {
                // 橡皮擦工具：绘制当前正在创建的擦除路径和位置指示器
                EraserTool eraserTool = (EraserTool) currentTool;

                // 绘制当前的擦除路径（如果正在绘制）
                Shape currentErasure = eraserTool.getCreatedShape();
                if (currentErasure != null && isDrawing) {
                    currentErasure.draw(g2d);
                }

                // 绘制当前位置指示器（圆形光标）
                Point p = eraserTool.getCurrentPoint();
                if (p != null) {
                    int size = eraserTool.getEraserSize();

                    // 绘制半透明的圆形指示器
                    g2d.setColor(new Color(200, 200, 200, 150));
                    g2d.fillOval(p.x - size/2, p.y - size/2, size, size);

                    // 绘制圆形边框
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawOval(p.x - size/2, p.y - size/2, size, size);
                }
            } else {
                // 其他工具：绘制当前正在创建的形状
                Shape currentShape = currentTool.getCreatedShape();
                if (currentShape != null) {
                    currentShape.draw(g2d);
                }
            }
        }
    }

    // 设置当前工具
    public void setCurrentTool(DrawingTool tool) {
        logger.info("Switching tool to: " + (tool != null ? tool.getClass().getSimpleName() : "null"));
        this.currentTool = tool;
        repaint();
    }

    // 设置当前颜色
    public void setCurrentColor(Color color) {
        logger.info("Setting color to: " + color);
        this.currentColor = color;

        // 只有在当前工具不是橡皮擦时才更新工具颜色
        if (!(currentTool instanceof EraserTool)) {
            updateToolColor();
        }

        // 通知ToolPanel更新预览
        if (toolPanel != null) {
            toolPanel.updatePreview();
        }
    }

    // 设置当前线宽
    public void setCurrentStrokeWidth(int width) {
        this.currentStrokeWidth = width;
        updateToolStrokeWidth();
    }

    // 设置当前字体
    public void setCurrentFont(Font font) {
        this.currentFont = font;
        if (currentTool instanceof TextTool) {
            currentTool = new TextTool(currentColor, font);
        }
    }

    // 更新工具颜色
    private void updateToolColor() {
    // 橡皮擦不参与颜色更新，直接返回
        if (currentTool instanceof EraserTool) {
            return;
        }

        if (currentTool instanceof LineTool) {
            currentTool = new LineTool(currentColor, currentStrokeWidth);
        } else if (currentTool instanceof RectangleTool) {
            currentTool = new RectangleTool(currentColor, currentStrokeWidth);
        } else if (currentTool instanceof OvalTool) {
            currentTool = new OvalTool(currentColor, currentStrokeWidth);
        } else if (currentTool instanceof TriangleTool) {
            currentTool = new TriangleTool(currentColor, currentStrokeWidth);
        } else if (currentTool instanceof PencilTool) {
            currentTool = new PencilTool(currentColor, currentStrokeWidth);
        } else if (currentTool instanceof TextTool) {
            currentTool = new TextTool(currentColor, currentFont);
        }
    }

    // 更新工具线宽
    private void updateToolStrokeWidth() {
        if (currentTool instanceof LineTool) {
            currentTool = new LineTool(currentColor, currentStrokeWidth);
        } else if (currentTool instanceof RectangleTool) {
            currentTool = new RectangleTool(currentColor, currentStrokeWidth);
        } else if (currentTool instanceof OvalTool) {
            currentTool = new OvalTool(currentColor, currentStrokeWidth);
        } else if (currentTool instanceof TriangleTool) {
            currentTool = new TriangleTool(currentColor, currentStrokeWidth);
        } else if (currentTool instanceof PencilTool) {
            currentTool = new PencilTool(currentColor, currentStrokeWidth);
        } else if (currentTool instanceof EraserTool) {
            // 橡皮擦只更新大小，保持白色
            EraserTool oldEraser = (EraserTool) currentTool;
            int newSize = Math.max((int)Math.round(currentStrokeWidth * 1.25), 8); // 橡皮擦至少8像素

            // 始终使用白色作为橡皮擦颜色
            EraserTool newEraser = new EraserTool(newSize, Color.WHITE);

        // 保持当前位置
        if (oldEraser.getCurrentPoint() != null) {
            newEraser.setCurrentPoint(oldEraser.getCurrentPoint());
        }

        currentTool = newEraser;
    }
    // TextTool不受线宽影响
}

    // 清除画布
    public void clearCanvas() {
        logger.info("Clearing canvas in WhiteboardPanel");
        shapes.clear();

        // 重置当前工具状态，取消任何正在进行的绘制操作
        if (currentTool != null) {
            // 对于不同类型的工具可能需要不同的处理
            if (currentTool instanceof PencilTool) {
                // 重新创建一个新的同类型工具，保持颜色和线宽不变
                currentTool = new PencilTool(currentColor, currentStrokeWidth);
            } else if (currentTool instanceof LineTool) {
                currentTool = new LineTool(currentColor, currentStrokeWidth);
            } else if (currentTool instanceof RectangleTool) {
                currentTool = new RectangleTool(currentColor, currentStrokeWidth);
            } else if (currentTool instanceof OvalTool) {
                currentTool = new OvalTool(currentColor, currentStrokeWidth);
            } else if (currentTool instanceof TriangleTool) {
                currentTool = new TriangleTool(currentColor, currentStrokeWidth);
            } else if (currentTool instanceof TextTool) {
                Font currentFont = new Font("Arial", Font.PLAIN,
                        toolPanel != null ? toolPanel.getCurrentFontSize() : 14);
                currentTool = new TextTool(currentColor, currentFont);
            } else if (currentTool instanceof EraserTool) {
                EraserTool eraserTool = (EraserTool) currentTool;
                int size = eraserTool.getEraserSize();
                currentTool = new EraserTool(size, Color.WHITE);
            }
        }

        repaint();
        // Log the action
        logger.info("Canvas cleared successfully, shapes count: " + shapes.size());
    }

    // 获取所有形状
    public List<Shape> getShapes() {
        return new ArrayList<>(shapes);
    }

    // 设置形状列表
    public void setShapes(List<Shape> shapes) {
        this.shapes = new ArrayList<>(shapes);
        repaint();
    }

    public void setToolPanel(ToolPanel toolPanel) {
        this.toolPanel = toolPanel;
    }

    /*
     * test methods
     */

    // 临时测试透明度功能，可在初始化后调用或通过特定按键触发
//    private void testTransparency() {
//        // 创建两个重叠的形状
//        Point p1 = new Point(100, 100);
//        Point p2 = new Point(300, 300);
//
//        Rectangle rect = new Rectangle(p1, p2, Color.RED, 2);
//        shapes.add(rect);
//
//        Point p3 = new Point(200, 100);
//        Point p4 = new Point(400, 300);
//
//        Rectangle rect2 = new Rectangle(p3, p4, Color.BLUE, 2);
//        // 设置临时透明度为半透明
//        rect2.setTempAlpha(128);
//        shapes.add(rect2);
//
//        repaint();
//    }

    // getters for current tool and color
    public DrawingTool getCurrentTool() {
        return currentTool;
    }

    public Color getCurrentColor() {
        return currentColor;
    }

    public Font getCurrentFont() {
        return currentFont;
    }

    // Connection related methods
    // 设置绘制监听器
    public void setDrawingListener(Consumer<Shape> listener) {
        this.drawingListener = listener;
    }

    public void addShape(Shape shape) {
        if (shape != null) {
            logger.info("Adding shape: " + shape.getClass().getSimpleName() + " ID: " + shape.getId());

        // 检查是否已存在相同ID的形状，防止重复添加
        boolean alreadyExists = shapes.stream()
                .anyMatch(existingShape -> existingShape.getId().equals(shape.getId()));

            if (!alreadyExists) {
                shapes.add(shape);

            // 按时间戳排序 - 这是关键，确保所有客户端的显示顺序一致
            shapes.sort((s1, s2) -> Long.compare(s1.getTimestamp(), s2.getTimestamp()));

                repaint();
            } else {
                logger.warning("Duplicate shape ignored: " + shape.getId());
            }
        }
    }

    public void removeShape(String shapeId) {
        shapes.removeIf(shape -> shape.getId().equals(shapeId));
        repaint();
    }

    // 在 WhiteboardPanel.java 中添加这个方法
    public Consumer<Shape> getDrawingListener() {
        return drawingListener;
    }

}