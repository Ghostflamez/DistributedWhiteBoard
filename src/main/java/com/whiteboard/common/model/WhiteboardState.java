package com.whiteboard.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
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

    /**
     * 根据ID移除形状
     * @param shapeId 要移除的形状ID
     * @return 是否成功移除
     */
    public boolean removeShape(String shapeId) {
        Iterator<Shape> iterator = shapes.iterator();
        while (iterator.hasNext()) {
            Shape shape = iterator.next();
            if (shape.getId().equals(shapeId)) {
                iterator.remove();
                version++;
                return true;
            }
        }
        return false;
    }

    public void clear() {
        shapes.clear();
        version++;
    }

    public long getVersion() {
        return version;
    }
}