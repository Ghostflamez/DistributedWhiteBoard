package com.whiteboard.client.shapes;

import java.awt.*;

public class Oval extends Shape {
    public Oval(Point start, Point end, Color color, int strokeWidth) {
        super(start, end, color, strokeWidth);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(color);
        g.setStroke(new BasicStroke(strokeWidth));

        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int width = Math.abs(endPoint.x - startPoint.x);
        int height = Math.abs(endPoint.y - startPoint.y);

        g.drawOval(x, y, width, height);
    }

    @Override
    public boolean contains(Point p) {
        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int width = Math.abs(endPoint.x - startPoint.x);
        int height = Math.abs(endPoint.y - startPoint.y);

        double rx = width / 2.0;
        double ry = height / 2.0;
        double cx = x + rx;
        double cy = y + ry;

        double normX = (p.x - cx) / rx;
        double normY = (p.y - cy) / ry;
        double dist = normX * normX + normY * normY;

        return Math.abs(dist - 1.0) <= (strokeWidth / Math.min(rx, ry));
    }
}