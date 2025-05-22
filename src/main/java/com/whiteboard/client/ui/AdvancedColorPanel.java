package com.whiteboard.client.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;

public class AdvancedColorPanel extends JPanel {
    private Color currentColor = Color.BLACK;
    private Color originalColor = Color.BLACK; // 保存原始颜色，用于取消操作
    private Color lastAppliedColor = Color.BLACK; // 保存最后应用的颜色
    private JPanel colorPreview;
    private JSlider redSlider, greenSlider, blueSlider, alphaSlider;
    private JTextField redField, greenField, blueField, alphaField, hexField;
    private Consumer<Color> colorChangeListener;
    private boolean updatingControls = false;
    private JButton okButton, applyButton, cancelButton;
    private Dialog ownerDialog; // 引用包含此面板的对话框
    private boolean confirmed = false; // 跟踪用户是确认还是取消

    public AdvancedColorPanel(Color initialColor, Consumer<Color> colorChangeListener) {
        this.currentColor = initialColor != null ? initialColor : Color.BLACK;
        this.originalColor = new Color(
                currentColor.getRed(),
                currentColor.getGreen(),
                currentColor.getBlue(),
                currentColor.getAlpha()
        ); // 保存初始颜色的副本
        this.lastAppliedColor = this.originalColor; // 初始时，最后应用的颜色与原始颜色相同
        this.colorChangeListener = colorChangeListener;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 主面板（包含颜色预览和控件）
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        // Color preview panel on the left
        colorPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 绘制棋盘背景以显示透明度
                int squareSize = 8;
                for (int i = 0; i < getWidth(); i += squareSize) {
                    for (int j = 0; j < getHeight(); j += squareSize) {
                        if ((i / squareSize + j / squareSize) % 2 == 0) {
                            g.setColor(Color.LIGHT_GRAY);
                        } else {
                            g.setColor(Color.WHITE);
                        }
                        g.fillRect(i, j, squareSize, squareSize);
                    }
                }

                // 绘制当前颜色
                g.setColor(currentColor);
                g.fillRect(0, 0, getWidth(), getHeight());

                // Draw a border
                g.setColor(Color.GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        colorPreview.setPreferredSize(new Dimension(80, 80));
        mainPanel.add(colorPreview, BorderLayout.WEST);

        // Controls panel on the right
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));

        // Hex color field
        JPanel hexPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hexPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        // 使用固定宽度的标签面板
        JPanel hexLabelPanel = new JPanel();
        hexLabelPanel.setLayout(new BorderLayout());
        hexLabelPanel.setPreferredSize(new Dimension(50, 20));
        JLabel hexLabel = new JLabel("Hex:");
        hexLabelPanel.add(hexLabel, BorderLayout.WEST);
        hexPanel.add(hexLabelPanel);

        hexField = new JTextField(7);
        hexField.setText(String.format("#%02X%02X%02X",
                currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue()));
        hexField.getDocument().addDocumentListener(new HexFieldListener());
        hexField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateColorFromHex();
            }
        });
        hexField.addActionListener(e -> updateColorFromHex());
        hexPanel.add(hexField);
        controlsPanel.add(hexPanel);

        // RGB sliders - 使用相同的面板和标签宽度
        controlsPanel.add(createColorSlider("Red", redSlider = new JSlider(0, 255, currentColor.getRed()),
                redField = new JTextField(3), Color.RED));
        controlsPanel.add(createColorSlider("Green", greenSlider = new JSlider(0, 255, currentColor.getGreen()),
                greenField = new JTextField(3), Color.GREEN));
        controlsPanel.add(createColorSlider("Blue", blueSlider = new JSlider(0, 255, currentColor.getBlue()),
                blueField = new JTextField(3), Color.BLUE));

        // Alpha slider
        controlsPanel.add(createColorSlider("Alpha", alphaSlider = new JSlider(0, 255, currentColor.getAlpha()),
                alphaField = new JTextField(3), Color.GRAY));

        mainPanel.add(controlsPanel, BorderLayout.CENTER);

        // 添加按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            confirmed = true;
            notifyColorChange(); // 确认时通知颜色更改
            lastAppliedColor = new Color(
                currentColor.getRed(),
                currentColor.getGreen(),
                currentColor.getBlue(),
                currentColor.getAlpha()
            ); // 更新最后应用的颜色
            if (ownerDialog != null) {
                ownerDialog.dispose();
            }
        });

        applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            notifyColorChange(); // 应用时通知颜色更改
            lastAppliedColor = new Color(
                currentColor.getRed(),
                currentColor.getGreen(),
                currentColor.getBlue(),
                currentColor.getAlpha()
            ); // 更新最后应用的颜色
            // 不关闭对话框
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            // 还原到原始颜色
            currentColor = originalColor;
            // 不通知颜色变化，直接关闭
            if (ownerDialog != null) {
                ownerDialog.dispose();
            }
        });

        buttonPanel.add(okButton);
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        // 添加主面板和按钮面板
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Initialize field values
        updateControlsFromColor();
    }

    // 设置拥有此面板的对话框引用
    public void setOwnerDialog(Dialog ownerDialog) {
        this.ownerDialog = ownerDialog;
    }

    private JPanel createColorSlider(String name, JSlider slider, JTextField field, Color labelColor) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        // 使用固定宽度的标签面板确保对齐
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BorderLayout());
        labelPanel.setPreferredSize(new Dimension(50, 20));
        JLabel label = new JLabel(name);
        label.setForeground(labelColor);
        labelPanel.add(label, BorderLayout.WEST);
        panel.add(labelPanel, BorderLayout.WEST);

        // 滑块设置
        slider.setPreferredSize(new Dimension(150, 20));
        slider.addChangeListener(e -> {
            if (!updatingControls) {
                updatingControls = true;
                field.setText(String.valueOf(slider.getValue()));
                updateColorFromSliders();
                updatingControls = false;
            }
        });
        panel.add(slider, BorderLayout.CENTER);

        // 文本框设置
        field.setText(String.valueOf(slider.getValue()));
        field.setPreferredSize(new Dimension(40, 20));
        configureNumberField(field, 0, 255);
        field.getDocument().addDocumentListener(new TextFieldListener(field, slider));
        field.addActionListener(e -> updateColorFromFields());

        // 把文本框放在一个固定宽度的面板中
        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.setPreferredSize(new Dimension(45, 20));
        fieldPanel.add(field, BorderLayout.CENTER);
        panel.add(fieldPanel, BorderLayout.EAST);

        return panel;
    }

    private void configureNumberField(JTextField field, int min, int max) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string.matches("\\d*")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text.matches("\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateFieldValue(field, min, max);
            }
        });
    }

    private void validateFieldValue(JTextField field, int min, int max) {
        try {
            int value = Integer.parseInt(field.getText());
            if (value < min) value = min;
            if (value > max) value = max;
            field.setText(String.valueOf(value));
        } catch (NumberFormatException ex) {
            field.setText(String.valueOf(min));
        }
    }

    private void updateColorFromSliders() {
        currentColor = new Color(
                redSlider.getValue(),
                greenSlider.getValue(),
                blueSlider.getValue(),
                alphaSlider.getValue()
        );
        hexField.setText(String.format("#%02X%02X%02X",
                currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue()));
        colorPreview.repaint();

        // 实时预览颜色，但不需要确认
        // 我们将等待OK按钮点击后才正式通知
    }

    private void updateColorFromFields() {
        if (!updatingControls) {
            updatingControls = true;
            try {
                int r = Integer.parseInt(redField.getText());
                int g = Integer.parseInt(greenField.getText());
                int b = Integer.parseInt(blueField.getText());
                int a = Integer.parseInt(alphaField.getText());

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                a = Math.max(0, Math.min(255, a));

                redSlider.setValue(r);
                greenSlider.setValue(g);
                blueSlider.setValue(b);
                alphaSlider.setValue(a);

                currentColor = new Color(r, g, b, a);
                hexField.setText(String.format("#%02X%02X%02X", r, g, b));
                colorPreview.repaint();

                // 实时预览颜色，但不需要确认
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
            updatingControls = false;
        }
    }

    private void updateColorFromHex() {
        if (!updatingControls) {
            updatingControls = true;
            try {
                String hex = hexField.getText().replace("#", "").trim();
                if (hex.length() == 6) {
                    int r = Integer.parseInt(hex.substring(0, 2), 16);
                    int g = Integer.parseInt(hex.substring(2, 4), 16);
                    int b = Integer.parseInt(hex.substring(4, 6), 16);
                    int a = currentColor.getAlpha(); // Keep current alpha

                    redSlider.setValue(r);
                    greenSlider.setValue(g);
                    blueSlider.setValue(b);

                    redField.setText(String.valueOf(r));
                    greenField.setText(String.valueOf(g));
                    blueField.setText(String.valueOf(b));

                    currentColor = new Color(r, g, b, a);
                    colorPreview.repaint();

                    // 实时预览颜色，但不需要确认
                }
            } catch (Exception e) {
                // Restore valid hex on error
                hexField.setText(String.format("#%02X%02X%02X",
                        currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue()));
            }
            updatingControls = false;
        }
    }

    private void updateControlsFromColor() {
        updatingControls = true;

        redSlider.setValue(currentColor.getRed());
        greenSlider.setValue(currentColor.getGreen());
        blueSlider.setValue(currentColor.getBlue());
        alphaSlider.setValue(currentColor.getAlpha());

        redField.setText(String.valueOf(currentColor.getRed()));
        greenField.setText(String.valueOf(currentColor.getGreen()));
        blueField.setText(String.valueOf(currentColor.getBlue()));
        alphaField.setText(String.valueOf(currentColor.getAlpha()));

        hexField.setText(String.format("#%02X%02X%02X",
                currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue()));

        colorPreview.repaint();
        updatingControls = false;
    }

    public void setColor(Color color) {
        if (color != null) {
            this.currentColor = color;
            this.originalColor = new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    color.getAlpha()
            ); // 保存初始颜色副本
            this.lastAppliedColor = this.originalColor; // 重置最后应用的颜色
            updateControlsFromColor();
        }
    }

    public Color getColor() {
        return currentColor;
    }

    public Color getLastAppliedColor() {
        return lastAppliedColor;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void notifyColorChange() {
        if (colorChangeListener != null) {
            colorChangeListener.accept(currentColor);
        }
    }

    private class TextFieldListener implements DocumentListener {
        private JTextField field;
        private JSlider slider;

        public TextFieldListener(JTextField field, JSlider slider) {
            this.field = field;
            this.slider = slider;
        }

        private void update() {
            if (!updatingControls) {
                try {
                    int value = Integer.parseInt(field.getText());
                    value = Math.max(0, Math.min(255, value));
                    slider.setValue(value);
                    // The slider's change listener will update the color
                } catch (NumberFormatException e) {
                    // Ignore invalid input
                }
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            update();
        }
    }

    private class HexFieldListener implements DocumentListener {
        private void update() {
            if (!updatingControls && hexField.getText().length() == 7) {
                updateColorFromHex();
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            // Don't update when removing characters
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            update();
        }
    }
}