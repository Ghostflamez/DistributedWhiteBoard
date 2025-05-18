package com.whiteboard.client.shapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.Serializable;

/**
 * 形状抽象类，所有可绘制对象的基类
 */
public abstract class Shape implements Serializable {
    protected Color color;
    protected Point startPoint;
    protected Point endPoint;

    /**
     * 构造函数
     *
     * @param color 形状颜色
     * @param startPoint 起始点
     * @param endPoint 结束点
     */
    public Shape(Color color, Point startPoint, Point endPoint) {
        this.color = color;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    /**
     * 绘制形状
     *
     * @param g 图形上下文
     */
    public abstract void draw(Graphics2D g);

    /**
     * 判断点是否在形状内
     *
     * @param p 待判断的点
     * @return 如果点在形状内返回true，否则返回false
     */
    public abstract boolean contains(Point p);

    /**
     * 更新形状的终点
     *
     * @param endPoint 新的终点
     */
    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }

    /**
     * 获取形状颜色
     *
     * @return 形状颜色
     */
    public Color getColor() {
        return color;
    }

    /**
     * 获取起始点
     *
     * @return 起始点
     */
    public Point getStartPoint() {
        return startPoint;
    }

    /**
     * 获取终点
     *
     * @return 终点
     */
    public Point getEndPoint() {
        return endPoint;
    }
}