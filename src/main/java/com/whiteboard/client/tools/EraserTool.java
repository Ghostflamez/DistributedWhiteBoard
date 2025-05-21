package com.whiteboard.client.tools;

import com.whiteboard.client.shapes.Shape;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

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

    private EraseMode mode = EraseMode.FREE; // default mode
    private int eraserSize;
    private Point currentPoint;
    private List<Shape> shapesToRemove = new ArrayList<>();
    private List<Shape> shapesToPreview = new ArrayList<>(); // 预览透明度改变的形状
    private List<Point> erasePath = new ArrayList<>(); // 记录橡皮擦轨迹

    public EraserTool(int eraserSize) {
        this.eraserSize = eraserSize;
    }

    @Override
    public void mousePressed(Point p) {
        currentPoint = p;
        erasePath.clear();
        erasePath.add(p);

        if (mode == EraseMode.OBJECT) {
            // 对象擦除模式
            shapesToPreview.clear();
        }
    }

    @Override
    public void mouseDragged(Point p) {
        currentPoint = p;
        erasePath.add(p);
    }

    @Override
    public Shape getCreatedShape() {
        // 橡皮擦不创建形状，返回null
        return null;
    }

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
    // 在EraserTool类中添加释放状态标志
    private boolean released = false;


    @Override
    public void mouseReleased(Point p) {
        currentPoint = p;
        erasePath.add(p);
        released = true; // 设置释放标志

        // 实际擦除逻辑将在WhiteboardPanel中处理
    }

    public boolean isReleased() {
        return released;
    }
}