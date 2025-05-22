package com.whiteboard.client.tools;

import com.whiteboard.client.shapes.Shape;
import java.awt.Point;

public interface DrawingTool {
    void mousePressed(Point p);
    void mouseDragged(Point p);
    void mouseReleased(Point p);
    Shape getCreatedShape();
}