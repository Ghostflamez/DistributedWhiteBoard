package com.whiteboard.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import com.whiteboard.client.WhiteboardClient;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class WhiteboardFrame extends JFrame {
    private WhiteboardPanel whiteboardPanel;
    private ToolPanel toolPanel;
    private ColorPanel colorPanel;
    private boolean isManager = false;

    private WhiteboardClient client;

    // 聊天相关组件
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JTextArea chatArea;
    private JTextField chatInput;

    // 新添加的颜色选择器
    private ColorSelectionPanel colorSelectionPanel; // 新添加
    private boolean useNewColorSelector = true; // 控制开关
    private Map<String, JDialog> pendingUserDialogs = new HashMap<>();

    //
    private static final Logger logger = Logger.getLogger(WhiteboardFrame.class.getName());

    public WhiteboardFrame(String title, boolean isManager, WhiteboardClient client) {
        super(title);
        this.isManager = isManager;
        this.client = client;
        initComponents();
        setupUI();
        setupWindowListener();
    }

    // 构造函数重载
    public WhiteboardFrame(String title, boolean isManager) {
        this(title, isManager, null);
    }

    private void initComponents() {
        whiteboardPanel = new WhiteboardPanel();
        toolPanel = new ToolPanel(whiteboardPanel);
        // 条件创建颜色选择器
        if (useNewColorSelector) {
            colorSelectionPanel = new ColorSelectionPanel(Color.BLACK, color -> {
                whiteboardPanel.setCurrentColor(color);
            });
        } else {
            colorPanel = new ColorPanel(whiteboardPanel);
        }

        whiteboardPanel.setToolPanel(toolPanel);
    }

    private void setupWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });
    }

    private void handleWindowClosing() {
        int response = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit?",
                "Exit Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            // 断开连接
            if (client != null) {
                try {
                    client.disconnect();
                } catch (Exception e) {
                    System.err.println("Error disconnecting: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            dispose();
            System.exit(0);
        }
    }

    /**
     * 初始化用户列表面板
     */
    private JPanel createUserListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Online Users"));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 如果是管理员，添加右键菜单以踢出用户
        if (isManager) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem kickMenuItem = new JMenuItem("Kick User");
            kickMenuItem.addActionListener(e -> {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    kickUser(selectedUser);
                }
            });
            popupMenu.add(kickMenuItem);

            userList.setComponentPopupMenu(popupMenu);
        }

        JScrollPane scrollPane = new JScrollPane(userList);
        scrollPane.setPreferredSize(new Dimension(150, 200));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 初始化聊天面板
     */
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Chat"));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setPreferredSize(new Dimension(150, 200));
        panel.add(chatScrollPane, BorderLayout.CENTER);

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        chatInput.addActionListener(e -> {
            sendChatMessage();
        });

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> {
            sendChatMessage();
        });

        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 发送聊天消息
     */
    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            // 使用客户端发送消息
            if (client != null) {
                try {
                    client.sendChatMessage(message);
                } catch (Exception e) {
                    System.err.println("Error sending chat message: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // 如果没有客户端（本地模式），直接显示
                addChatMessage("Me", message);
            }

            // 清空输入
            chatInput.setText("");
        }
    }

    /**
     * 添加聊天消息到聊天区域
     */
    public void addChatMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(sender + ": " + message + "\n");
            // 滚动到底部
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    /**
     * 更新用户列表
     */
    public void updateUserList(List<String> users) {
        if (users == null) {
            logger.warning("Received null user list");
            return;
        }

        logger.info("Updating user list: " + users);

        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String username : users) {
                userListModel.addElement(username);
            }
        });
    }
    /**
     * 踢出用户（仅管理员）
     */
    private void kickUser(String username) {
        if (client != null && isManager) {
            try {
                if (client.kickUser(username)) {
                    JOptionPane.showMessageDialog(this,
                            "User '" + username + "' has been kicked.",
                            "User Kicked",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to kick user '" + username + "'.",
                            "Kick Failed",
                            JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception e) {
                System.err.println("Error kicking user: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error kicking user: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Kick functionality not available in local mode or you're not a manager.",
                    "Not Available",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // 修改setupUI方法以添加用户列表和聊天区域
    private void setupUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

        // 添加工具栏
        add(toolPanel, BorderLayout.NORTH);

        // 添加颜色选择器
        if (useNewColorSelector) {
            add(colorSelectionPanel, BorderLayout.SOUTH);
        } else {
            add(colorPanel, BorderLayout.SOUTH);
        }

        // 创建中心面板，包含画布
        JPanel centerPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(whiteboardPanel);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建侧边栏，包含用户列表和聊天
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(createUserListPanel(), BorderLayout.NORTH);
        sidePanel.add(createChatPanel(), BorderLayout.CENTER);

        // 添加分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, sidePanel);
        splitPane.setResizeWeight(0.8); // 画布占比
        splitPane.setOneTouchExpandable(true);
        add(splitPane, BorderLayout.CENTER);

        // 如果是管理员，添加文件菜单
        if (isManager) {
            setupFileMenu();
        }

        // 使窗口居中显示
        setLocationRelativeTo(null);
    }

    /**
     * 设置文件菜单（仅管理员）
     */
    private void setupFileMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> newWhiteboard());

        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> openWhiteboard());

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> saveWhiteboard());

        JMenuItem saveAsItem = new JMenuItem("Save As");
        saveAsItem.addActionListener(e -> saveWhiteboardAs());

        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(e -> closeApplication());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(closeItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    // 文件菜单功能（暂时只是占位方法）
    private void newWhiteboard() {
        if (client != null && client.isManager()) {
            try {
                logger.info("Initiating new whiteboard from menu");
                client.clearCanvas(); // This will clear locally and send to server
                logger.info("New whiteboard command sent to server");
            } catch (Exception e) {
                logger.severe("Error creating new whiteboard: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Error creating new whiteboard: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Only the manager can create a new whiteboard.",
                    "Permission Denied",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openWhiteboard() {
        // TODO: 实现打开白板功能
        JOptionPane.showMessageDialog(this, "Open whiteboard functionality not implemented yet.");
    }

    private void saveWhiteboard() {
        // TODO: 实现保存白板功能
        JOptionPane.showMessageDialog(this, "Save whiteboard functionality not implemented yet.");
    }

    private void saveWhiteboardAs() {
        // TODO: 实现另存为功能
        JOptionPane.showMessageDialog(this, "Save As functionality not implemented yet.");
    }

    private void closeApplication() {
        int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit?",
                "Exit Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (response == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    public WhiteboardPanel getWhiteboardPanel() {
        return whiteboardPanel;
    }

    public WhiteboardClient getClient() {
        return this.client;
    }

    /**
     * 显示用户加入请求对话框
     */
    public void showJoinRequest(String username, boolean isOnline) {
        logger.info("Showing join request from: " + username + " (online: " + isOnline + ")");

        SwingUtilities.invokeLater(() -> {
            // 检查是否已有该用户的对话框
            JDialog existingDialog = pendingUserDialogs.get(username);
            if (existingDialog != null && existingDialog.isVisible()) {
                // 更新现有对话框状态
                updateJoinRequestDialog(existingDialog, username, isOnline);
                return;
            }

            // 创建新的对话框
            JDialog dialog = new JDialog(this, "Join Request", false);
            pendingUserDialogs.put(username, dialog);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel messageLabel = new JLabel("User '" + username + "' wants to join.");
            JLabel statusLabel = new JLabel("Status: " + (isOnline ? "waiting" : "disconnected"));

            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton approveButton = new JButton("Approve");
            JButton rejectButton = new JButton("Reject");

            approveButton.addActionListener(e -> {
                if (client != null) {
                    if (client.approveUser(username)) {
                        JOptionPane.showMessageDialog(this,
                                "User '" + username + "' has been approved.",
                                "User Approved",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to approve user '" + username + "'.",
                                "Approval Failed",
                                JOptionPane.WARNING_MESSAGE);
                    }
                }
                dialog.dispose();
                pendingUserDialogs.remove(username);
            });

            rejectButton.addActionListener(e -> {
                if (client != null) {
                    if (client.rejectUser(username)) {
                        JOptionPane.showMessageDialog(this,
                                "User '" + username + "' has been rejected.",
                                "User Rejected",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to reject user '" + username + "'.",
                                "Rejection Failed",
                                JOptionPane.WARNING_MESSAGE);
                    }
                }
                dialog.dispose();
                pendingUserDialogs.remove(username);
            });

            buttonsPanel.add(approveButton);
            buttonsPanel.add(rejectButton);

            JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
            infoPanel.add(messageLabel);
            infoPanel.add(statusLabel);

            panel.add(infoPanel, BorderLayout.CENTER);
            panel.add(buttonsPanel, BorderLayout.SOUTH);

            dialog.add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);

            // 如果用户已断开连接，添加自动关闭计时器
            if (!isOnline) {
                startAutoCloseTimer(dialog, statusLabel, username);
            }
        });
    }

    /**
     * 更新加入请求对话框状态
     */
    private void updateJoinRequestDialog(JDialog dialog, String username, boolean isOnline) {
        // 查找状态标签并更新
        Container contentPane = dialog.getContentPane();
        if (contentPane.getComponentCount() > 0) {
            Component comp = contentPane.getComponent(0);
            if (comp instanceof JPanel) {
                JPanel mainPanel = (JPanel) comp;
                for (Component subComp : mainPanel.getComponents()) {
                    if (subComp instanceof JPanel) {
                        JPanel infoPanel = (JPanel) subComp;
                        for (Component infoComp : infoPanel.getComponents()) {
                            if (infoComp instanceof JLabel) {
                                JLabel label = (JLabel) infoComp;
                                if (label.getText().startsWith("Status:")) {
                                    label.setText("Status: " + (isOnline ? "waiting" : "disconnected"));

                                    // 如果用户已断开连接，添加自动关闭计时器
                                    if (!isOnline) {
                                        startAutoCloseTimer(dialog, label, username);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 启动对话框自动关闭计时器
     */
    private void startAutoCloseTimer(JDialog dialog, JLabel statusLabel, String username) {
        Timer timer = new Timer(true);
        final int[] secondsLeft = {5};

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    secondsLeft[0]--;
                    statusLabel.setText("Status: disconnected (close after " + secondsLeft[0] + "s)");

                    if (secondsLeft[0] <= 0) {
                        timer.cancel();
                        dialog.dispose();
                        pendingUserDialogs.remove(username);
                    }
                });
            }
        }, 1000, 1000);
    }


}