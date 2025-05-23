package com.whiteboard.client.tools;

import com.whiteboard.client.shapes.Shape;
import com.whiteboard.client.shapes.FreeDrawing;
import java.awt.Point;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import com.whiteboard.client.shapes.ErasureShape;
import com.whiteboard.client.shapes.FreeDrawing;

public class EraserTool implements DrawingTool {
    private FreeDrawing currentErasure;
    private final Color backgroundColor; // 设为final，防止意外修改
    private int eraserSize;
    private Point currentPoint;
    private List<Point> erasePath = new ArrayList<>();
    private List<Shape> shapesToRemove = new ArrayList<>();
    private List<Shape> shapesToPreview = new ArrayList<>();
    private boolean released = false;
    private ErasureShape finalErasureShape = null;

    public EraserTool(int eraserSize, Color backgroundColor) {
        this.eraserSize = eraserSize;
        // 确保背景色不会被意外修改
        this.backgroundColor = new Color(backgroundColor.getRGB());
    }

    @Override
    public void mousePressed(Point p) {
        System.out.println("=== ERASER MOUSE PRESSED DEBUG ===");
        System.out.println("Creating FreeDrawing with background color: " + backgroundColor);

        // 强制使用背景色
        currentErasure = new FreeDrawing(p, backgroundColor, eraserSize);
        // 标记这是一个擦除对象
        currentErasure.setIsEraser(true);

        System.out.println("FreeDrawing created with color: " + currentErasure.getColor());
        System.out.println("FreeDrawing isEraser: " + currentErasure.isEraser());

        erasePath.clear();
        erasePath.add(p);
        currentPoint = p;
        System.out.println("=== END ERASER DEBUG ===");
    }

    @Override
    public void mouseDragged(Point p) {
        if (currentErasure != null) {
            addPointWithInterpolation(p);
            currentPoint = p;
        }
    }

    @Override
    public void mouseReleased(Point p) {
        if (currentErasure != null) {
            currentErasure.addPoint(p);
            erasePath.add(p);
            currentPoint = p;
        }
    }

    @Override
    public Shape getCreatedShape() {
        return currentErasure;
    }

    public void resetErasureShape() {
        currentErasure = null;
    }

    // 添加带插值的点，解决快速移动时的路径不连续问题
    private void addPointWithInterpolation(Point newPoint) {
        if (erasePath.isEmpty()) {
            erasePath.add(newPoint);
            currentErasure.addPoint(newPoint);
            return;
        }

    // Get the last point
    Point lastPoint = erasePath.get(erasePath.size() - 1);

    // Calculate distance
    double distance = lastPoint.distance(newPoint);

    // If distance is too large, interpolate points between
    if (distance > 8) { // Lower threshold to 8 pixels for smoother curves
        int steps = (int)(distance / 4) + 1; // More points (every 4 pixels)

        for (int i = 1; i < steps; i++) {
            double ratio = (double)i / steps;
            int x = (int)(lastPoint.x + (newPoint.x - lastPoint.x) * ratio);
            int y = (int)(lastPoint.y + (newPoint.y - lastPoint.y) * ratio);

            Point interpolatedPoint = new Point(x, y);
            erasePath.add(interpolatedPoint);
            currentErasure.addPoint(interpolatedPoint);
        }
    }

    // Add the new point
    erasePath.add(newPoint);
    currentErasure.addPoint(newPoint);
}

    // 简化后的getter方法
    public Point getCurrentPoint() {
        return currentPoint;
    }

    public int getEraserSize() {
        return eraserSize;
    }

    public List<Point> getErasePath() {
        return erasePath;
    }

    // 颜色保护：不允许外部修改橡皮擦的颜色
    public Color getBackgroundColor() {
        return new Color(backgroundColor.getRGB()); // 返回副本，防止修改
    }

    public void setEraserSize(int eraserSize) {
        this.eraserSize = eraserSize;
    }

    public void clearPath() {
        erasePath.clear();
    }
    public void setCurrentPoint(Point p) {
        this.currentPoint = p;
    }
}