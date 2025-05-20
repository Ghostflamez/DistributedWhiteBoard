package com.whiteboard.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.whiteboard.client.shapes.Shape;

public class WhiteboardState implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Shape> shapes;
    private long version;

    public WhiteboardState() {
        shapes = new ArrayList<>();
        version = 0;
    }

    public List<Shape> getShapes() {
        return new ArrayList<>(shapes);
    }

    public void setShapes(List<Shape> shapes) {
        this.shapes = new ArrayList<>(shapes);
        version++;
    }

    public void addShape(Shape shape) {
        shapes.add(shape);
        version++;
    }

    public void clear() {
        shapes.clear();
        version++;
    }

    public long getVersion() {
        return version;
    }
}