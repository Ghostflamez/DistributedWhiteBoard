package com.whiteboard.client.shapes;

import java.awt.*;

public class Triangle extends Shape {
    public Triangle(Point start, Point end, Color color, int strokeWidth) {
        super(start, end, color, strokeWidth);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(getDrawColor());
        g.setStroke(new BasicStroke(strokeWidth));

        // 计算三角形的三个点
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];

        xPoints[0] = startPoint.x;
        yPoints[0] = endPoint.y;

        xPoints[1] = (startPoint.x + endPoint.x) / 2;
        yPoints[1] = startPoint.y;

        xPoints[2] = endPoint.x;
        yPoints[2] = endPoint.y;

        g.drawPolygon(xPoints, yPoints, 3);
    }

    @Override
    public boolean contains(Point p) {
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];

        xPoints[0] = startPoint.x;
        yPoints[0] = endPoint.y;

        xPoints[1] = (startPoint.x + endPoint.x) / 2;
        yPoints[1] = startPoint.y;

        xPoints[2] = endPoint.x;
        yPoints[2] = endPoint.y;

        for (int i = 0; i < 3; i++) {
            int j = (i + 1) % 3;
            Point lineStart = new Point(xPoints[i], yPoints[i]);
            Point lineEnd = new Point(xPoints[j], yPoints[j]);
            double dist = distanceToLine(p, lineStart, lineEnd);
            if (dist <= strokeWidth) return true;
        }
        return false;
    }

    private double distanceToLine(Point p, Point start, Point end) {
        double normalLength = Math.sqrt((end.x-start.x)*(end.x-start.x) +
                (end.y-start.y)*(end.y-start.y));
        return Math.abs((p.x-start.x)*(end.y-start.y)-(p.y-start.y)*(end.x-start.x))/normalLength;
    }
}