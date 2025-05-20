package com.whiteboard.client.tools;

import com.whiteboard.client.shapes.Rectangle;
import com.whiteboard.client.shapes.Shape;
import java.awt.Color;
import java.awt.Point;

public class RectangleTool implements DrawingTool {
    private Rectangle currentRect;
    private Color color;
    private int strokeWidth;

    public RectangleTool(Color color, int strokeWidth) {
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    @Override
    public void mousePressed(Point p) {
        currentRect = new Rectangle(p, p, color, strokeWidth);
    }

    @Override
    public void mouseDragged(Point p) {
        currentRect.setEndPoint(p);
    }

    @Override
    public void mouseReleased(Point p) {
        currentRect.setEndPoint(p);
    }

    @Override
    public Shape getCreatedShape() {
        return currentRect;
    }
}