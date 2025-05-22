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
    private String text = "";
    private Point position;
    private boolean isEditing = false;
    private Point currentPosition;

    public TextTool(Color color, Font font) {
        this.color = color;
        this.font = font;
    }

    public void setText(String text) {
        this.text = text;
        // 如果当前正在编辑，更新预览文本
        if (isEditing && position != null) {
            currentText = new Text(position, text, color, font);
        }
    }

    public void setPosition(Point p) {
        this.position = p;
    }

    public void startEditing() {
        isEditing = true;
        text = "";
        if (position != null) {
            // 创建预览文本
            currentText = new Text(position, text, color, font);
        }
    }

    public void finishEditing() {
        isEditing = false;
        // 如果文本为空或只有空格，不创建文本对象
        if (text != null && !text.trim().isEmpty()) {
            currentText = new Text(position, text, color, font);
        } else {
            currentText = null;
        }
    }

    public boolean isEditing() {
        return isEditing;
    }

    @Override
    public void mousePressed(Point p) {
        position = p;
        startEditing();
    }

    @Override
    public void mouseDragged(Point p) {
        // 文本工具不处理拖动
    }

    @Override
    public void mouseReleased(Point p) {
        // 文本工具不处理释放
    }

    @Override
    public Shape getCreatedShape() {
        return currentText;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        if (isEditing && position != null) {
            currentText = new Text(position, text, color, font);
        }
    }

    public String getText() {
        return text;
    }

    // 为了完全清除状态，可以添加一个重置方法
    public void reset() {
        text = "";
        currentText = null;
        isEditing = false;
    }
}