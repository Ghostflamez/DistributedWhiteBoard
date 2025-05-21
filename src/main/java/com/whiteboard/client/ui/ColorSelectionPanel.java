package com.whiteboard.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class ColorSelectionPanel extends JPanel {
    private Color currentColor = Color.BLACK;
    private int currentAlpha = 255; // 默认透明度为100%
    private JPanel colorPreviewCircle;
    private JLabel hexLabel;
    private JButton blackButton, redButton, advancedButton;
    private JSlider alphaSlider;
    private JTextField alphaField;
    private Consumer<Color> colorChangeListener;
    private JDialog advancedDialog;
    private AdvancedColorPanel advancedColorPanel;

    public ColorSelectionPanel(Color initialColor, Consumer<Color> colorChangeListener) {
        this.currentColor = initialColor;
        this.currentAlpha = initialColor.getAlpha();
        this.colorChangeListener = colorChangeListener;

        setLayout(new BorderLayout(5, 0));

        // 创建颜色预览圆和Hex标签
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setPreferredSize(new Dimension(80, 60));

        colorPreviewCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 4;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // 画透明背景（棋盘格）
                g2.setColor(Color.LIGHT_GRAY);
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                for (int i = 0; i < size; i += 8) {
                    for (int j = i % 16; j < size; j += 16) {
                        g2.fillRect(x + i, y + j, 8, 8);
                    }
                }

                // 画当前颜色
                g2.setColor(currentColor);
                g2.fillOval(x, y, size, size);

                // 画边框
                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x, y, size, size);
            }
        };
        colorPreviewCircle.setPreferredSize(new Dimension(50, 50));
        previewPanel.add(colorPreviewCircle, BorderLayout.CENTER);

        hexLabel = new JLabel(String.format("#%02X%02X%02X",
                currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue()));
        hexLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewPanel.add(hexLabel, BorderLayout.SOUTH);

        add(previewPanel, BorderLayout.WEST);

        // 创建按钮和滑块面板
        JPanel controlsPanel = new JPanel(new BorderLayout(5, 0));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        blackButton = createColorButton(Color.BLACK);
        redButton = createColorButton(Color.RED);
        advancedButton = new JButton("...");
        advancedButton.addActionListener(e -> showAdvancedColorDialog());

        buttonsPanel.add(blackButton);
        buttonsPanel.add(redButton);
        buttonsPanel.add(advancedButton);

        controlsPanel.add(buttonsPanel, BorderLayout.WEST);

        // 透明度滑块和输入框
        JPanel alphaPanel = new JPanel(new BorderLayout(5, 0));
        alphaPanel.add(new JLabel("Alpha:"), BorderLayout.WEST);

        alphaSlider = new JSlider(1, 100, currentAlpha * 100 / 255);
        alphaSlider.addChangeListener(e -> {
            currentAlpha = alphaSlider.getValue() * 255 / 100;
            alphaField.setText(alphaSlider.getValue() + "%");
            updateColorWithAlpha();
        });
        alphaPanel.add(alphaSlider, BorderLayout.CENTER);

        alphaField = new JTextField(4);
        alphaField.setText(alphaSlider.getValue() + "%");
        alphaField.addActionListener(e -> {
            try {
                String text = alphaField.getText().replace("%", "").trim();
                int value = Integer.parseInt(text);
                value = Math.max(1, Math.min(100, value));
                alphaSlider.setValue(value);
                // 滑块的changeListener会更新颜色
            } catch (NumberFormatException ex) {
                alphaField.setText(alphaSlider.getValue() + "%");
            }
        });
        alphaPanel.add(alphaField, BorderLayout.EAST);

        controlsPanel.add(alphaPanel, BorderLayout.CENTER);

        add(controlsPanel, BorderLayout.CENTER);
    }

    private JButton createColorButton(Color color) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(color);
                g.fillRect(3, 3, getWidth() - 6, getHeight() - 6);
            }
        };
        button.setPreferredSize(new Dimension(24, 24));
        button.addActionListener(e -> {
            setBaseColor(color);
        });
        return button;
    }

    private void showAdvancedColorDialog() {
        if (advancedDialog == null) {
            Window parent = SwingUtilities.getWindowAncestor(this);
            advancedDialog = new JDialog(parent, "Advanced Color Selector", Dialog.ModalityType.APPLICATION_MODAL);
            advancedColorPanel = new AdvancedColorPanel(currentColor, color -> {
                setBaseColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), currentAlpha));
            });
            advancedDialog.add(advancedColorPanel);
            advancedDialog.pack();
            advancedDialog.setLocationRelativeTo(parent);
            advancedDialog.setResizable(false);
        } else {
            advancedColorPanel.setColor(new Color(
                    currentColor.getRed(),
                    currentColor.getGreen(),
                    currentColor.getBlue(),
                    255)); // 使用完全不透明来编辑基础颜色
        }

        advancedDialog.setVisible(true);
    }

    private void setBaseColor(Color color) {
        // 保持当前的透明度
        this.currentColor = new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                currentAlpha
        );

        hexLabel.setText(String.format("#%02X%02X%02X",
                currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue()));
        colorPreviewCircle.repaint();
        notifyColorChange();
    }

    private void updateColorWithAlpha() {
        this.currentColor = new Color(
                currentColor.getRed(),
                currentColor.getGreen(),
                currentColor.getBlue(),
                currentAlpha
        );
        colorPreviewCircle.repaint();
        notifyColorChange();
    }

    public void setColor(Color color) {
        this.currentColor = color;
        this.currentAlpha = color.getAlpha();
        hexLabel.setText(String.format("#%02X%02X%02X",
                color.getRed(), color.getGreen(), color.getBlue()));
        alphaSlider.setValue(currentAlpha * 100 / 255);
        alphaField.setText(alphaSlider.getValue() + "%");
        colorPreviewCircle.repaint();
    }

    public Color getColor() {
        return currentColor;
    }

    private void notifyColorChange() {
        if (colorChangeListener != null) {
            colorChangeListener.accept(currentColor);
        }
    }
}