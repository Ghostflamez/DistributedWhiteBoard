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
    private final Color eraserColor; // 橡皮擦颜色（白色）
    private int eraserSize;
    private Point currentPoint;
    private List<Point> erasePath = new ArrayList<>();
    private List<Shape> shapesToRemove = new ArrayList<>();
    private List<Shape> shapesToPreview = new ArrayList<>();
    private boolean released = false;
    private ErasureShape finalErasureShape = null;

    public EraserTool(int eraserSize, Color backgroundColor) {
        this.eraserSize = eraserSize;
        // 橡皮擦始终使用白色，忽略传入的背景色
        this.eraserColor = Color.WHITE;

        System.out.println("EraserTool created with size: " + eraserSize + ", using WHITE color");
    }

    @Override
    public void mousePressed(Point p) {
        System.out.println("EraserTool mousePressed at: " + p);

        // 创建白色的自由绘制对象
        currentErasure = new FreeDrawing(p, eraserColor, eraserSize);

        // 不需要设置擦除标志，就当普通的白色笔刷使用
        currentPoint = p;

        System.out.println("Created FreeDrawing with color: " + currentErasure.getColor());
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
            currentPoint = p;
        }

        System.out.println("EraserTool mouseReleased, shape ready for sending");
    }

    @Override
    public Shape getCreatedShape() {
        return currentErasure;
    }

    public void resetErasureShape() {
        System.out.println("Resetting eraser shape");
        currentErasure = null;
    }

    // 添加带插值的点，解决快速移动时的路径不连续问题
    private void addPointWithInterpolation(Point newPoint) {
        if (currentErasure == null) return;

        List<Point> existingPoints = currentErasure.getPoints();
        if (existingPoints.isEmpty()) {
            currentErasure.addPoint(newPoint);
            return;
        }

        // 获取最后一个点
        Point lastPoint = existingPoints.get(existingPoints.size() - 1);

        // 计算距离
        double distance = lastPoint.distance(newPoint);

        // 如果距离太大，插入中间点
        if (distance > 5) { // 降低阈值到5像素以获得更平滑的曲线
            int steps = (int)(distance / 3) + 1; // 每3像素一个点

            for (int i = 1; i < steps; i++) {
                double ratio = (double)i / steps;
                int x = (int)(lastPoint.x + (newPoint.x - lastPoint.x) * ratio);
                int y = (int)(lastPoint.y + (newPoint.y - lastPoint.y) * ratio);

                Point interpolatedPoint = new Point(x, y);
                currentErasure.addPoint(interpolatedPoint);
            }
        }

        // 添加新点
        currentErasure.addPoint(newPoint);
    }

    // 简化后的getter方法
    public Point getCurrentPoint() {
        return currentPoint;
    }

    public int getEraserSize() {
        return eraserSize;
    }

    public Color getBackgroundColor() {
        return eraserColor; // 始终返回白色
    }

    public void setEraserSize(int eraserSize) {
        this.eraserSize = eraserSize;
    }

    public void setCurrentPoint(Point p) {
        this.currentPoint = p;
    }
}