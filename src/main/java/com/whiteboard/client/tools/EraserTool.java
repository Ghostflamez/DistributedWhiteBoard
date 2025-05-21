package com.whiteboard.client.tools;

import com.whiteboard.client.shapes.Shape;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class EraserTool implements DrawingTool {
    private int eraserSize;
    private Point currentPoint;
    private List<Shape> shapesToRemove;

    public EraserTool(int eraserSize) {
        this.eraserSize = eraserSize;
        this.shapesToRemove = new ArrayList<>();
    }

    @Override
    public void mousePressed(Point p) {
        currentPoint = p;
    }

    @Override
    public void mouseDragged(Point p) {
        currentPoint = p;
    }

    @Override
    public void mouseReleased(Point p) {
        currentPoint = p;
    }

    @Override
    public Shape getCreatedShape() {
        // 橡皮擦不创建形状，返回null
        return null;
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

    public void setEraserSize(int eraserSize) {
        this.eraserSize = eraserSize;
    }
}