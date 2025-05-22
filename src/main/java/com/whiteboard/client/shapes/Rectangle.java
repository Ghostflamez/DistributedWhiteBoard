package com.whiteboard.client.shapes;

import java.awt.*;

public class Rectangle extends Shape {
    public Rectangle(Point start, Point end, Color color, int strokeWidth) {
        super(start, end, color, strokeWidth);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(getDrawColor());
        g.setStroke(new BasicStroke(strokeWidth));

        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int width = Math.abs(endPoint.x - startPoint.x);
        int height = Math.abs(endPoint.y - startPoint.y);

        g.drawRect(x, y, width, height);
    }

    @Override
    public boolean contains(Point p) {
        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int width = Math.abs(endPoint.x - startPoint.x);
        int height = Math.abs(endPoint.y - startPoint.y);

        return p.x >= x - strokeWidth && p.x <= x + width + strokeWidth &&
                p.y >= y - strokeWidth && p.y <= y + height + strokeWidth &&
                (Math.abs(p.x - x) <= strokeWidth ||
                        Math.abs(p.x - (x + width)) <= strokeWidth ||
                        Math.abs(p.y - y) <= strokeWidth ||
                        Math.abs(p.y - (y + height)) <= strokeWidth);
    }
}