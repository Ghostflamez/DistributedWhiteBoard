package com.whiteboard.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

import com.whiteboard.common.remote.IWhiteboardServer;

public class ServerStarter {
    private static final Logger logger = Logger.getLogger(ServerStarter.class.getName());

    public static void main(String[] args) {
        try {
            // 可选：设置安全管理器
            // System.setProperty("java.security.policy", "server.policy");
            // if (System.getSecurityManager() == null) {
            //     System.setSecurityManager(new SecurityManager());
            // }

            // 创建服务器实例
            WhiteboardServer server = new WhiteboardServer();

            // 导出远程对象
            IWhiteboardServer stub = (IWhiteboardServer) UnicastRemoteObject.exportObject(server, 0);

            // 创建注册表
            int port = 1099; // 默认RMI端口
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }

            Registry registry = LocateRegistry.createRegistry(port);

            // 绑定远程对象
            registry.rebind("WhiteboardServer", stub);

            logger.info("WhiteboardServer running on port " + port);
            System.out.println("WhiteboardServer running on port " + port);
        } catch (Exception e) {
            logger.severe("WhiteboardServer exception: " + e.getMessage());
            System.err.println("WhiteboardServer exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}