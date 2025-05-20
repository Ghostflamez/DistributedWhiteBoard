package com.whiteboard.client.shapes;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FreeDrawing extends Shape {
    private List<Point> points;

    public FreeDrawing(Point start, Color color, int strokeWidth) {
        super(start, start, color, strokeWidth);
        points = new ArrayList<>();
        points.add(start);
    }

    public void addPoint(Point p) {
        points.add(p);
        endPoint = p;
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(color);
        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
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
        return Math.abs((p.x-start.x)*(end.y-start.y)-(p.y-start.y)*(end.x-start.x))/normalLength;
    }
}