package com.whiteboard.client.shapes;

import java.awt.*;
import java.io.Serializable;
import java.util.UUID;

// abstract class Shape
public abstract class Shape implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected Color color;
    protected Point startPoint;
    protected Point endPoint;
    protected int strokeWidth;
// constructor
    public Shape(Point startPoint, Point endPoint, Color color, int strokeWidth) {
        this.id = UUID.randomUUID().toString();
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    public abstract void draw(Graphics2D g);
    public abstract boolean contains(Point p);

    // Getters and setters
    public String getId() { return id; }
    public Color getColor() { return color; }
    public Point getStartPoint() { return startPoint; }
    public Point getEndPoint() { return endPoint; }
    public void setEndPoint(Point endPoint) { this.endPoint = endPoint; }
    public int getStrokeWidth() { return strokeWidth; }
}