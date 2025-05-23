package com.whiteboard.client.shapes;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FreeDrawing extends Shape {
    private static final long serialVersionUID = 1L;

    private List<Point> points;
    private boolean isEraser = false; // 新增：标记是否为擦除模式

    public FreeDrawing(Point start, Color color, int strokeWidth) {
        super(start, start, color, strokeWidth);
        points = new ArrayList<>();
        points.add(start);
    }

    public void addPoint(Point p) {
        points.add(p);
        endPoint = p;
    }

    // 新增：设置擦除模式
    public void setIsEraser(boolean isEraser) {
        System.out.println("=== FREEDRAWING ERASER FLAG DEBUG ===");
        System.out.println("Setting isEraser to: " + isEraser);
        System.out.println("Shape color: " + this.color);
        System.out.println("Thread: " + Thread.currentThread().getName());
        this.isEraser = isEraser;
        System.out.println("=== END FREEDRAWING DEBUG ===");
    }

    // 新增：获取擦除模式状态
    public boolean isEraser() {
        return isEraser;
    }

    @Override
    public void draw(Graphics2D g) {
        System.out.println("=== FREEDRAWING DRAW DEBUG ===");
        System.out.println("Drawing FreeDrawing - isEraser: " + isEraser + ", color: " + color);

        if (isEraser) {
            System.out.println("Using eraser mode (Clear composite)");
            // 擦除模式代码...
        } else {
            System.out.println("Using normal drawing mode with color: " + getDrawColor());
            // 正常绘制代码...
        }
        System.out.println("=== END FREEDRAWING DRAW DEBUG ===");
    }

    @Override
    public boolean contains(Point p) {
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            double dist = distanceToLine(p, p1, p2);
            if (dist <= strokeWidth + 2) return true;
        }
        return false;
    }

    private double distanceToLine(Point p, Point start, Point end) {
        double normalLength = Math.sqrt((end.x-start.x)*(end.x-start.x) +
                (end.y-start.y)*(end.y-start.y));
        if (normalLength == 0) return p.distance(start);
        return Math.abs((p.x-start.x)*(end.y-start.y)-(p.y-start.y)*(end.x-start.x))/normalLength;
    }

    // 新增：获取点列表（用于调试或其他用途）
    public List<Point> getPoints() {
        return new ArrayList<>(points);
    }
}