package com.whiteboard.client.tools;

import com.whiteboard.client.shapes.Line;
import com.whiteboard.client.shapes.Shape;
import java.awt.Color;
import java.awt.Point;

public class LineTool implements DrawingTool {
    private Line currentLine;
    private Color color;
    private int strokeWidth;

    public LineTool(Color color, int strokeWidth) {
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    @Override
    public void mousePressed(Point p) {
        currentLine = new Line(p, p, color, strokeWidth);
    }

    @Override
    public void mouseDragged(Point p) {
        currentLine.setEndPoint(p);
    }

    @Override
    public void mouseReleased(Point p) {
        currentLine.setEndPoint(p);
    }

    @Override
    public Shape getCreatedShape() {
        return currentLine;
    }
}