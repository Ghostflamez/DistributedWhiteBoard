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
    // for temporary use
    protected int tempAlpha = -1;
    protected long timestamp;
// constructor
    public Shape(Point startPoint, Point endPoint, Color color, int strokeWidth) {
        this.id = UUID.randomUUID().toString();
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.color = color;
        this.strokeWidth = strokeWidth;
        // Set the timestamp to the current time
        this.timestamp = System.currentTimeMillis();
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

    // 新增时间戳方法
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // 其余方法保持不变
    protected Color getDrawColor() {
        if (tempAlpha >= 0) {
            return new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    tempAlpha
            );
        }
        return color;
    }

    public int getAlpha() {
        return color.getAlpha();
    }

    public void setTempAlpha(int alpha) {
        this.tempAlpha = alpha;
    }

    public void clearTempAlpha() {
        this.tempAlpha = -1;
    }

    public boolean hasTempAlpha() {
        return tempAlpha >= 0;
    }
}