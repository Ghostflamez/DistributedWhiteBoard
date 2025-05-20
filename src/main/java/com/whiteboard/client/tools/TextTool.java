package com.whiteboard.client.tools;

import com.whiteboard.client.shapes.Text;
import com.whiteboard.client.shapes.Shape;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;

public class TextTool implements DrawingTool {
    private Text currentText;
    private Color color;
    private Font font;
    private String text;

    public TextTool(Color color, Font font) {
        this.color = color;
        this.font = font;
        this.text = "";
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void mousePressed(Point p) {
        if (!text.isEmpty()) {
            currentText = new Text(p, text, color, font);
        }
    }

    @Override
    public void mouseDragged(Point p) {
        // 文本工具拖动时不做任何操作
    }

    @Override
    public void mouseReleased(Point p) {
        // 文本工具释放时不做任何操作
    }

    @Override
    public Shape getCreatedShape() {
        return currentText;
    }
}