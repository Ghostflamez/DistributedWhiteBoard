package com.whiteboard.common.model;

import java.io.Serializable;
import com.whiteboard.client.shapes.Shape;

public class DrawOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum OperationType {
        ADD_SHAPE, REMOVE_SHAPE, CLEAR, UPDATE_SHAPE
    }

    private OperationType type;
    private Shape shape;
    private String sessionId;
    private long timestamp;

    public DrawOperation(OperationType type, Shape shape, String sessionId) {
        this.type = type;
        this.shape = shape;
        this.sessionId = sessionId;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public OperationType getType() {
        return type;
    }

    public Shape getShape() {
        return shape;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}