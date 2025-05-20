package com.whiteboard.client.tools;

import java.awt.Color;
import java.awt.Font;

public class ToolFactory {
    public enum ToolType {
        PENCIL, LINE, RECTANGLE, OVAL, TRIANGLE, TEXT, ERASER
    }

    public static DrawingTool createTool(ToolType type, Color color, int strokeWidth) {
        switch (type) {
            case PENCIL:
                return new PencilTool(color, strokeWidth);
            case LINE:
                return new LineTool(color, strokeWidth);
            case RECTANGLE:
                return new RectangleTool(color, strokeWidth);
            case OVAL:
                return new OvalTool(color, strokeWidth);
            case TRIANGLE:
                return new TriangleTool(color, strokeWidth);
            case TEXT:
                return new TextTool(color, new Font("Arial", Font.PLAIN, 14));
            case ERASER:
                return new EraserTool(strokeWidth * 5);
            default:
                return new PencilTool(color, strokeWidth);
        }
    }
}