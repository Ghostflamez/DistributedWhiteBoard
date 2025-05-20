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
        g.setColor(color);
        g.setFont(font);
        g.drawString(text, startPoint.x, startPoint.y);
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
}
