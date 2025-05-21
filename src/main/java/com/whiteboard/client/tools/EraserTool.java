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
    // 擦除模式枚举
    public enum EraseMode {
        OBJECT("Object Eraser"),
        FREE("Freehand Eraser");
        private final String displayName;

        EraseMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private FreeDrawing currentErasure;
    private Color backgroundColor;
    private int eraserSize;
    private Point currentPoint;
    private EraseMode mode = EraseMode.FREE; // 默认为自由擦除模式
    private List<Point> erasePath = new ArrayList<>();
    private List<Shape> shapesToRemove = new ArrayList<>();
    private List<Shape> shapesToPreview = new ArrayList<>();
    private boolean released = false;

    public EraserTool(int eraserSize, Color backgroundColor) {
        this.eraserSize = eraserSize;
        this.backgroundColor = backgroundColor;
    }

    @Override
    public void mousePressed(Point p) {
        // 使用背景色创建FreeDrawing对象
        currentErasure = new FreeDrawing(p, backgroundColor, eraserSize);
        erasePath.clear();
        erasePath.add(p);
        currentPoint = p;
        released = false;
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
            released = true;
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



    // Getters and setters
    public void setMode(EraseMode mode) {
        this.mode = mode;
    }

    public Point getCurrentPoint() {
        return currentPoint;
    }

    public int getEraserSize() {
        return eraserSize;
    }

    public List<Shape> getShapesToRemove() {
        return shapesToRemove;
    }

    public void clearShapesToRemove() {
        shapesToRemove.clear();
    }

    public void addShapeToRemove(Shape shape) {
        if (!shapesToRemove.contains(shape)) {
            shapesToRemove.add(shape);
        }
    }

    public List<Shape> getShapesToPreview() {
        return shapesToPreview;
    }

    public void addShapeToPreview(Shape shape) {
        if (!shapesToPreview.contains(shape)) {
            shapesToPreview.add(shape);
        }
    }

    public void clearShapesToPreview() {
        shapesToPreview.clear();
    }

    public List<Point> getErasePath() {
        return erasePath;
    }

    public EraseMode getMode() {
        return mode;
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

    public boolean isReleased() {
        return released;
    }
}