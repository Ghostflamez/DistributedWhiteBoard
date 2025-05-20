package com.whiteboard.client;

public class JoinWhiteBoard {
    public static void main(String[] args) {
        // 第一阶段只实现本地绘图，暂不处理网络参数
        if (args.length < 3) {
            System.out.println("本地模式: 启动为普通用户");
            WhiteboardClient client = new WhiteboardClient("Guest", false);
        } else {
            // 参数格式: <serverIPAddress> <serverPort> username
            String serverIP = args[0];
            int serverPort = Integer.parseInt(args[1]);
            String username = args[2];

            System.out.println("第一阶段仅实现本地功能，忽略网络参数");
            System.out.println("服务器IP: " + serverIP);
            System.out.println("服务器端口: " + serverPort);
            System.out.println("用户名: " + username);

            WhiteboardClient client = new WhiteboardClient(username, false);
        }
    }
}