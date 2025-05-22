// 临时测试文件
package com.whiteboard.client;

import com.whiteboard.client.shapes.Line;
import java.awt.Color;
import java.awt.Point;

public class TestShape {
    public static void main(String[] args) {
        Line line = new Line(new Point(0,0), new Point(10,10), Color.BLACK, 2);
        System.out.println("Shape has timestamp: " + line.getTimestamp());
        System.out.println("Timestamp value: " + line.getTimestamp());
    }
}