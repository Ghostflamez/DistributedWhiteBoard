package com.whiteboard.client.tools;

import com.whiteboard.client.shapes.Oval;
import com.whiteboard.client.shapes.Shape;
import java.awt.Color;
import java.awt.Point;

public class OvalTool implements DrawingTool {
    private Oval currentOval;
    private Color color;
    private int strokeWidth;

    public OvalTool(Color color, int strokeWidth) {
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    @Override
    public void mousePressed(Point p) {
        currentOval = new Oval(p, p, color, strokeWidth);
    }

    @Override
    public void mouseDragged(Point p) {
        currentOval.setEndPoint(p);
    }

    @Override
    public void mouseReleased(Point p) {
        currentOval.setEndPoint(p);
    }

    @Override
    public Shape getCreatedShape() {
        return currentOval;
    }
}