package com.whiteboard.client.ui;

import com.whiteboard.client.shapes.Shape;
//test
//import com.whiteboard.client.shapes.Rectangle;

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

    public WhiteboardPanel() {
        shapes = new ArrayList<>();
        currentColor = Color.BLACK;
        currentStrokeWidth = 2;
        currentFont = new Font("Arial", Font.PLAIN, 14);

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
                    String input = JOptionPane.showInputDialog(WhiteboardPanel.this,
                            "输入文本:", "文本工具",
                            JOptionPane.PLAIN_MESSAGE);
                    if (input != null && !input.isEmpty()) {
                        ((TextTool) currentTool).setText(input);
                        currentTool.mousePressed(e.getPoint());
                        Shape shape = currentTool.getCreatedShape();
                        if (shape != null) {
                            shapes.add(shape);
                            repaint();
                        }
                    }
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
                    // 橡皮擦处理
                    EraserTool eraserTool = (EraserTool) currentTool;
                    eraserTool.mouseReleased(e.getPoint());
                    processEraser(eraserTool);
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
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void processEraser(EraserTool eraserTool) {
        Point p = eraserTool.getCurrentPoint();
        int size = eraserTool.getEraserSize();

        // 创建橡皮擦区域
        java.awt.Rectangle eraserRect = new java.awt.Rectangle(
                p.x - size/2, p.y - size/2, size, size);

        // 检查哪些形状与橡皮擦相交
        List<Shape> toRemove = new ArrayList<>();
        for (Shape shape : shapes) {
            // 简单检查：如果形状的起点或终点在橡皮擦范围内
            if (eraserRect.contains(shape.getStartPoint()) ||
                    eraserRect.contains(shape.getEndPoint())) {
                toRemove.add(shape);
            }
            // 也可以实现更复杂的相交检测
        }

        // 移除相交的形状
        shapes.removeAll(toRemove);
        repaint();
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
        if (currentTool != null && !(currentTool instanceof TextTool) &&
                !(currentTool instanceof EraserTool)) {
            Shape currentShape = currentTool.getCreatedShape();
            if (currentShape != null) {
                currentShape.draw(g2d);
            }
        }

        // 如果是橡皮擦工具，绘制橡皮擦指示器
        if (currentTool instanceof EraserTool) {
            EraserTool eraserTool = (EraserTool) currentTool;
            Point p = eraserTool.getCurrentPoint();
            if (p != null) {
                int size = eraserTool.getEraserSize();
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawRect(p.x - size/2, p.y - size/2, size, size);
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