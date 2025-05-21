package com.whiteboard.client.shapes;

import java.awt.*;

public class Text extends Shape {
    private String text;
    private Font font;

    public Text(Point position, String text, Color color, Font font) {
        super(position, position, color, 1);
        this.text = text;
        this.font = font;
    }

    @Override
    public void draw(Graphics2D g) {
        // 保存原始字体
        Font originalFont = g.getFont();

        // 设置正确的绘制属性
        g.setColor(getDrawColor());

        // 确保使用原始的Unicode支持字体
        Font unicodeFont = new Font("Arial Unicode MS", font.getStyle(), font.getSize());
        try {
            g.setFont(unicodeFont);
        } catch (Exception e) {
            // 如果无法设置首选字体，使用原始字体
            g.setFont(font);
        }

        // 绘制文本
        g.drawString(text, startPoint.x, startPoint.y);

        // 恢复原始字体
        g.setFont(originalFont);
    }

    @Override
    public boolean contains(Point p) {
        FontMetrics metrics = new FontMetrics(font) {};
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();

        return p.x >= startPoint.x && p.x <= startPoint.x + textWidth &&
                p.y >= startPoint.y - textHeight && p.y <= startPoint.y;
    }

    public String getText() {
        return text;
    }

    public Font getFont() {
        return font;
    }
}
