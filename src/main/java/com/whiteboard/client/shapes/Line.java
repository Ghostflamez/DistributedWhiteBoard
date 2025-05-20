package com.whiteboard.client.shapes;

import java.awt.*;

public class Line extends Shape {
    public Line(Point start, Point end, Color color, int strokeWidth) {
        super(start, end, color, strokeWidth);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(color);
        g.setStroke(new BasicStroke(strokeWidth));
        g.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
    }

    @Override
    public boolean contains(Point p) {
        double distanceToLine = distanceToLine(p, startPoint, endPoint);
        return distanceToLine <= strokeWidth + 2;
    }

    private double distanceToLine(Point p, Point start, Point end) {
        double normalLength = Math.sqrt((end.x-start.x)*(end.x-start.x) +
                (end.y-start.y)*(end.y-start.y));
        return Math.abs((p.x-start.x)*(end.y-start.y)-(p.y-start.y)*(end.x-start.x))/normalLength;
    }
}