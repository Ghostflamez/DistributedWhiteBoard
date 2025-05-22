package com.whiteboard.client.shapes;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ErasureShape extends Shape {
    private List<Point> path;
    private int eraserSize;

    public ErasureShape(List<Point> path, int eraserSize, Color backgroundColor) {
        // 起点和终点使用路径的首尾点
        super(path.get(0), path.get(path.size()-1), backgroundColor, eraserSize);
        this.path = new ArrayList<>(path);
        this.eraserSize = eraserSize;
    }

    @Override
    public void draw(Graphics2D g) {
        // 保存原始设置
        Stroke originalStroke = g.getStroke();
        Color originalColor = g.getColor();

        // 设置擦除参数
        g.setColor(getDrawColor());
        g.setStroke(new BasicStroke(eraserSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // 绘制路径
        for (int i = 0; i < path.size() - 1; i++) {
            Point p1 = path.get(i);
            Point p2 = path.get(i + 1);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // 恢复原始设置
        g.setStroke(originalStroke);
        g.setColor(originalColor);
    }

    @Override
    public boolean contains(Point p) {
        // 擦除形状不需要被选中/检测，返回false
        return false;
    }
}