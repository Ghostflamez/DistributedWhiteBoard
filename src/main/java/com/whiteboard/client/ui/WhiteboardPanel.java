package com.whiteboard.client.ui;
import com.whiteboard.client.shapes.ErasureShape;
import com.whiteboard.client.shapes.Shape;
import com.whiteboard.client.shapes.Text;
import com.whiteboard.client.shapes.Rectangle;
import com.whiteboard.client.shapes.Oval;
import com.whiteboard.client.shapes.Line;
import com.whiteboard.client.shapes.Triangle;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;


import com.whiteboard.client.tools.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardPanel extends JPanel {
    private List<Shape> shapes;
    private DrawingTool currentTool;
    private Color currentColor;
    private int currentStrokeWidth;
    private Font currentFont;
    private ToolPanel toolPanel;
    private Point currentPoint;

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
                } else if (currentTool instanceof EraserTool) {
                    // 橡皮擦处理
                    EraserTool eraserTool = (EraserTool) currentTool;
                    eraserTool.mousePressed(e.getPoint());
                    processEraser(eraserTool);
                } else {
                    // 其他工具处理
                    currentTool.mousePressed(e.getPoint());
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentTool instanceof TextTool) {
                    // 文本工具不处理拖动
                    return;
                } else if (currentTool instanceof EraserTool) {
                    // 橡皮擦处理
                    EraserTool eraserTool = (EraserTool) currentTool;
                    eraserTool.mouseDragged(e.getPoint());
                    processEraser(eraserTool);
                } else {
                    // 其他工具处理
                    currentTool.mouseDragged(e.getPoint());
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentTool instanceof TextTool) {
                    // 文本工具不处理释放
                    return;
                } else if (currentTool instanceof EraserTool) {
                    // 橡皮擦工具处理
                    EraserTool eraserTool = (EraserTool) currentTool;
                    eraserTool.mouseReleased(e.getPoint());
                    processEraser(eraserTool);

                    // 确保路径被清除
                    eraserTool.clearPath();
                    repaint();
                } else {
                    // 其他工具处理
                    currentTool.mouseReleased(e.getPoint());
                    Shape shape = currentTool.getCreatedShape();
                    if (shape != null) {
                        shapes.add(shape);
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

    private void processEraser(EraserTool eraserTool) {
        if (eraserTool.getCurrentPoint() == null) return;

        Point p = eraserTool.getCurrentPoint();
        int size = eraserTool.getEraserSize();

        if (eraserTool.getMode() == EraserTool.EraseMode.OBJECT) {
            // 对象擦除模式
            //processObjectEraser(eraserTool, p, size);
        } else {
            // 自由擦除模式
            processFreeEraser(eraserTool);
        }

        repaint();
    }

    // 修改对象擦除处理方法
    private void processObjectEraser(EraserTool eraserTool, Point p, int size) {
        // 创建橡皮擦圆形区域
        java.awt.geom.Ellipse2D eraserShape =
                new java.awt.geom.Ellipse2D.Double(p.x - size/2, p.y - size/2, size, size);

        // 检查哪些形状与橡皮擦相交
        List<Shape> toPreview = new ArrayList<>();

        for (Shape shape : shapes) {
            // 检测相交 - 简化版实现，可以根据需要改进
            if (eraserIntersectsShape(eraserShape, shape)) {
                toPreview.add(shape);
            }
        }

        // 清除之前的预览但现在不在预览列表中的形状
        for (Shape shape : eraserTool.getShapesToPreview()) {
            if (!toPreview.contains(shape)) {
                // 不再预览的形状，恢复正常
                shape.clearTempAlpha();
            }
        }

        // 设置新的预览
        eraserTool.clearShapesToPreview();

        for (Shape shape : toPreview) {
            // 设置半透明预览（50%透明度）
            shape.setTempAlpha(127); // 半透明，Alpha值为127
            eraserTool.addShapeToPreview(shape);
        }

        // 鼠标松开时才执行实际擦除
        if (!eraserTool.getErasePath().isEmpty() &&
                p.equals(eraserTool.getErasePath().get(eraserTool.getErasePath().size() - 1))) {

            // 移除所有预览中的形状
            shapes.removeAll(eraserTool.getShapesToPreview());
            eraserTool.clearShapesToPreview();

            // 清除路径
            eraserTool.clearPath();
        }

        repaint();
    }

    // 检查橡皮擦与形状相交的逻辑（改进版）
    private boolean eraserIntersectsShape(java.awt.geom.Ellipse2D eraser, Shape shape) {
        // 1. 检查形状的起点和终点是否在橡皮擦范围内
        if (eraser.contains(shape.getStartPoint()) || eraser.contains(shape.getEndPoint())) {
            return true;
        }

        // 2. 对于特定类型的形状，执行更精确的检测
        if (shape instanceof Line) {
            Line line = (Line) shape;
            // 使用Line2D检测直线与圆的相交
            java.awt.geom.Line2D lineShape = new java.awt.geom.Line2D.Double(
                    line.getStartPoint(), line.getEndPoint());

            // 圆的边界矩形
            double x = eraser.getX();
            double y = eraser.getY();
            double w = eraser.getWidth();
            double h = eraser.getHeight();

            // 检测线段是否与圆相交
            return lineShape.intersects(x, y, w, h);
        }
        else if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            // 创建Rectangle2D对象
            java.awt.geom.Rectangle2D rectShape = createRectangleFromShape(rect);
            // 检测矩形是否与圆相交
            return rectShape.intersects(eraser.getX(), eraser.getY(), eraser.getWidth(), eraser.getHeight());
        }
        else if (shape instanceof Oval) {
            Oval oval = (Oval) shape;
            // 创建Ellipse2D对象
            java.awt.geom.Ellipse2D ovalShape = createEllipseFromShape(oval);
            // 检测椭圆是否与圆相交 - 近似检测
            return ovalShape.intersects(eraser.getX(), eraser.getY(), eraser.getWidth(), eraser.getHeight());
        }

        // 3. 对于其他类型或更复杂的形状，使用简化检测
        return shape.contains(new Point((int)eraser.getCenterX(), (int)eraser.getCenterY()));
    }

    // 辅助方法：从形状创建Rectangle2D对象
    private java.awt.geom.Rectangle2D createRectangleFromShape(Rectangle rect) {
        int x = Math.min(rect.getStartPoint().x, rect.getEndPoint().x);
        int y = Math.min(rect.getStartPoint().y, rect.getEndPoint().y);
        int width = Math.abs(rect.getEndPoint().x - rect.getStartPoint().x);
        int height = Math.abs(rect.getEndPoint().y - rect.getStartPoint().y);

        return new java.awt.geom.Rectangle2D.Double(x, y, width, height);
    }

    // 辅助方法：从形状创建Ellipse2D对象
    private java.awt.geom.Ellipse2D createEllipseFromShape(Oval oval) {
        int x = Math.min(oval.getStartPoint().x, oval.getEndPoint().x);
        int y = Math.min(oval.getStartPoint().y, oval.getEndPoint().y);
        int width = Math.abs(oval.getEndPoint().x - oval.getStartPoint().x);
        int height = Math.abs(oval.getEndPoint().y - oval.getStartPoint().y);

        return new java.awt.geom.Ellipse2D.Double(x, y, width, height);
    }

    // 修改WhiteboardPanel中的processFreeEraser方法
    private void processFreeEraser(EraserTool eraserTool) {
        List<Point> path = eraserTool.getErasePath();
        if (path.size() < 2) return;

        int size = eraserTool.getEraserSize();

        // 鼠标释放时，创建一个擦除形状
        if (!path.isEmpty() &&
                eraserTool.getCurrentPoint().equals(path.get(path.size() - 1))) {

            // 创建一个使用背景色的擦除形状
            ErasureShape erasureShape = new ErasureShape(
                    new ArrayList<>(path), // 复制当前路径
                    size,
                    getBackground() // 使用画布背景色
            );

            // 添加到形状列表
            shapes.add(erasureShape);

            // 清除路径以便下次擦除
            eraserTool.clearPath();
        }

        repaint();
    }

    // 检查线段是否与形状相交
    private boolean lineIntersectsShape(java.awt.geom.Line2D line, Shape shape, double tolerance) {
        // 检查多个点沿线段是否与形状相交
        for (int i = 0; i <= 10; i++) {
            double t = i / 10.0;
            int x = (int)(line.getX1() * (1-t) + line.getX2() * t);
            int y = (int)(line.getY1() * (1-t) + line.getY2() * t);

            Point p = new Point(x, y);

            // 计算点到形状的距离
            if (shape.contains(p)) {
                return true;
            }
        }

        return false;
    }

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
                if (textShape != null) {
                    shapes.add(textShape);
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

    // 修改处理字体大小变更的方法，添加字体系列参数
    private void processFontSizeChange(JTextField fontSizeField, TextTool textTool, JComboBox<String> fontFamilyCombo) {
        try {
            String input = fontSizeField.getText().trim();
            double doubleValue = Double.parseDouble(input);
            int size = (int) Math.round(doubleValue);
            size = Math.max(1, size);
            size = Math.min(size, 72); // 最大字体大小为72

            fontSizeField.setText(String.valueOf(size));

            String family = (String) fontFamilyCombo.getSelectedItem();
            Font newFont = new Font(family, Font.PLAIN, size);
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
            } else if (!(currentTool instanceof EraserTool)) {
                Shape currentShape = currentTool.getCreatedShape();
                if (currentShape != null) {
                    currentShape.draw(g2d);
                }
            }
        }

        if (currentTool instanceof EraserTool) {
            EraserTool eraserTool = (EraserTool) currentTool;
            Point p = eraserTool.getCurrentPoint();
            if (p != null) {
                int size = eraserTool.getEraserSize();

                // 绘制轨迹（对象模式显示轨迹，自由模式不显示）
                if (eraserTool.getMode() == EraserTool.EraseMode.OBJECT) {
                    List<Point> path = eraserTool.getErasePath();
                    if (path.size() > 1) {
                        g2d.setColor(new Color(200, 200, 200, 100)); // 半透明灰色
                        g2d.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                        for (int i = 0; i < path.size() - 1; i++) {
                            Point p1 = path.get(i);
                            Point p2 = path.get(i + 1);
                            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                        }
                    }
                }

                // 绘制当前位置指示器（圆形）
                g2d.setColor(new Color(200, 200, 200, 150));
                g2d.fillOval(p.x - size/2, p.y - size/2, size, size);
                g2d.setColor(Color.DARK_GRAY);
                g2d.drawOval(p.x - size/2, p.y - size/2, size, size);

            }
        }
    }

    // 设置当前工具
    public void setCurrentTool(DrawingTool tool) {
        this.currentTool = tool;
    }

    // 设置当前颜色
    public void setCurrentColor(Color color) {
        this.currentColor = color;
        updateToolColor();

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
        // EraserTool不受颜色影响
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
            currentTool = new EraserTool(currentStrokeWidth * 5); // 橡皮擦尺寸为线宽的5倍
        }
        // TextTool不受线宽影响
    }

    // 清除画布
    public void clearCanvas() {
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
                currentTool = new EraserTool(size);
            }
        }

        repaint();
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


}