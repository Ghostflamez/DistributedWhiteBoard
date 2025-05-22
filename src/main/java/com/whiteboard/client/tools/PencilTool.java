package com.whiteboard.client.tools;

import com.whiteboard.client.shapes.FreeDrawing;
import com.whiteboard.client.shapes.Shape;
import java.awt.Color;
import java.awt.Point;

public class PencilTool implements DrawingTool {
    private FreeDrawing currentFreeDraw;
    private Color color;
    private int strokeWidth;

    public PencilTool(Color color, int strokeWidth) {
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    @Override
    public void mousePressed(Point p) {
        currentFreeDraw = new FreeDrawing(p, color, strokeWidth);
    }

    @Override
    public void mouseDragged(Point p) {
        currentFreeDraw.addPoint(p);
    }

    @Override
    public void mouseReleased(Point p) {
        currentFreeDraw.addPoint(p);
    }

    @Override
    public Shape getCreatedShape() {
        return currentFreeDraw;
    }
}