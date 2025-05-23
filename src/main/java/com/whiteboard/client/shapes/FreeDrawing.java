package com.whiteboard.client.shapes;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FreeDrawing extends Shape {
    private static final long serialVersionUID = 1L;

    private List<Point> points;

    public FreeDrawing(Point start, Color color, int strokeWidth) {
        super(start, start, color, strokeWidth);
        points = new ArrayList<>();
        points.add(start);

        System.out.println("FreeDrawing created with color: " + color + " (RGB: " + color.getRGB() + ")");
    }

    public void addPoint(Point p) {
        points.add(p);
        endPoint = p;
    }

    @Override
    public void draw(Graphics2D g) {
        if (points.size() < 2) {
            // 如果只有一个点，绘制一个小圆点
            if (points.size() == 1) {
                Point p = points.get(0);
                g.setColor(getDrawColor());
                g.fillOval(p.x - strokeWidth/2, p.y - strokeWidth/2, strokeWidth, strokeWidth);
            }
            return;
        }

        // 保存原始设置
        Stroke originalStroke = g.getStroke();
        Color originalColor = g.getColor();

        try {
            // 设置绘制属性
            g.setColor(getDrawColor());
            g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // 绘制路径
            for (int i = 0; i < points.size() - 1; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        } finally {
            // 恢复原始设置
            g.setStroke(originalStroke);
            g.setColor(originalColor);
        }
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