package com.whiteboard.common.model;

import com.whiteboard.client.shapes.Shape;
import java.io.Serializable;
import java.util.List;

public class WhiteboardSaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<Shape> shapes;
    public long version;
    public long timestamp;
    public String createdBy;

    public WhiteboardSaveData() {
    }
}