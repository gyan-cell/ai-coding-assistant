package com.blade.aicoder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;

public class FloatingWindow {
  private JFrame frame;
  private JTextPane chatArea;
  private JTextArea inputArea;
  private JButton sendButton;
  private JButton minimizeButton;
  private JButton copyButton;
  private JButton clearButton;
  private OllamaClient ollamaClient;

  // Color scheme - Modern dark theme
  private final Color BACKGROUND = new Color(30, 30, 35);
  private final Color SURFACE = new Color(45, 45, 50);
  private final Color PRIMARY = new Color(100, 150, 255);
  private final Color SECONDARY = new Color(70, 130, 230);
  private final Color TEXT_PRIMARY = new Color(240, 240, 245);
  private final Color TEXT_SECONDARY = new Color(180, 180, 190);
  private final Color CODE_BACKGROUND = SURFACE; // Darker, more distinct background
  private final Color CODE_BORDER = new Color(60, 70, 80);

  public FloatingWindow() {
    createUI();
  }

  private void createUI() {
    // Create main frame
    frame = new JFrame("AI Coding Assistant") {
      @Override
      public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(BACKGROUND);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
        super.paint(g);
      }
    };

    frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    frame.setSize(700, 800);
    frame.setLayout(new BorderLayout());
    frame.setAlwaysOnTop(false);
    frame.getContentPane().setBackground(BACKGROUND);
    frame.setUndecorated(true);
    frame.setBackground(new Color(0, 0, 0, 0));

    // Make window draggable
    DraggableWindow draggable = new DraggableWindow(frame);
    frame.addMouseListener(draggable);
    frame.addMouseMotionListener(draggable);

    // Create title bar
    JPanel titleBar = createTitleBar();
    frame.add(titleBar, BorderLayout.NORTH);

    // Create chat area with JTextPane for styled text
    chatArea = new JTextPane() {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Paint the main background
        g2d.setColor(SURFACE);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

        // Paint code blocks with distinct background
        super.paintComponent(g);
      }
    };

    chatArea.setEditable(false);
    chatArea.setBackground(SURFACE);
    chatArea.setForeground(TEXT_PRIMARY);
    chatArea.setBorder(new EmptyBorder(15, 15, 15, 15));
    chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    chatArea.setCaretColor(PRIMARY);

    // Set styled document
    StyledDocument doc = chatArea.getStyledDocument();
    addStylesToDocument(doc);

    JScrollPane chatScroll = new JScrollPane(chatArea);
    chatScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    chatScroll.getViewport().setBackground(SURFACE);

    // Style the scrollbar
    JScrollBar verticalScrollBar = chatScroll.getVerticalScrollBar();
    verticalScrollBar.setBackground(SURFACE);
    verticalScrollBar.setForeground(PRIMARY);

    // Create input area
    inputArea = new JTextArea(3, 20) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(SURFACE);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        super.paintComponent(g);
      }
    };

    inputArea.setLineWrap(true);
    inputArea.setWrapStyleWord(true);
    inputArea.setBackground(SURFACE);
    inputArea.setForeground(TEXT_PRIMARY);
    inputArea.setCaretColor(PRIMARY);
    inputArea.setBorder(new EmptyBorder(12, 12, 12, 12));
    inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));

    // Add placeholder text
    inputArea.setText("Ask me anything about coding... (Ctrl+Enter to send)");
    inputArea.setForeground(TEXT_SECONDARY);

    inputArea.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (inputArea.getText().equals("Ask me anything about coding... (Ctrl+Enter to send)")) {
          inputArea.setText("");
          inputArea.setForeground(TEXT_PRIMARY);
        }
      }

      @Override
      public void focusLost(FocusEvent e) {
        if (inputArea.getText().isEmpty()) {
          inputArea.setText("Ask me anything about coding... (Ctrl+Enter to send)");
          inputArea.setForeground(TEXT_SECONDARY);
        }
      }
    });

    inputArea.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
          sendMessage();
        }
      }
    });

    JScrollPane inputScroll = new JScrollPane(inputArea);
    inputScroll.setBorder(BorderFactory.createEmptyBorder());
    inputScroll.getViewport().setBackground(SURFACE);

    // Create buttons with modern styling (no icons)
    sendButton = createStyledButton("Send", PRIMARY);
    minimizeButton = createStyledButton("Minimize", new Color(100, 100, 110));
    copyButton = createStyledButton("Copy Code", new Color(80, 180, 120));
    clearButton = createStyledButton("Clear", new Color(220, 100, 100));

    sendButton.addActionListener(e -> sendMessage());
    minimizeButton.addActionListener(e -> frame.setVisible(false));
    copyButton.addActionListener(e -> copyLastCodeBlock());
    clearButton.addActionListener(e -> clearChat());

    // Create button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    buttonPanel.setBackground(BACKGROUND);
    buttonPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
    buttonPanel.add(clearButton);
    buttonPanel.add(copyButton);
    buttonPanel.add(minimizeButton);
    buttonPanel.add(sendButton);

    // Create input panel
    JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
    inputPanel.setBackground(BACKGROUND);
    inputPanel.setBorder(new EmptyBorder(10, 15, 15, 15));
    inputPanel.add(inputScroll, BorderLayout.CENTER);
    inputPanel.add(buttonPanel, BorderLayout.SOUTH);

    // Add components to frame
    frame.add(chatScroll, BorderLayout.CENTER);
    frame.add(inputPanel, BorderLayout.SOUTH);

    centerWindow();
    frame.setVisible(true);
  }

  private void addStylesToDocument(StyledDocument doc) {
    // Regular text style
    Style regular = doc.addStyle("regular", null);
    StyleConstants.setFontFamily(regular, "Segoe UI");
    StyleConstants.setFontSize(regular, 13);
    StyleConstants.setForeground(regular, TEXT_PRIMARY);
    StyleConstants.setBold(regular, false);

    // Bold style for emphasis
    Style bold = doc.addStyle("bold", regular);
    StyleConstants.setBold(bold, true);

    // Code block style - MUCH DARKER background for better contrast
    Style code = doc.addStyle("code", regular);
    StyleConstants.setBackground(code, CODE_BACKGROUND);
    StyleConstants.setForeground(code, new Color(230, 230, 240)); // Even brighter text
    StyleConstants.setFontFamily(code, "JetBrains Mono");
    StyleConstants.setFontSize(code, 12);
    StyleConstants.setBold(code, false);
    StyleConstants.setLeftIndent(code, 10.0f);
    StyleConstants.setRightIndent(code, 10.0f);

    // Code header style (for language tags like "python")
    Style codeHeader = doc.addStyle("codeHeader", regular);
    StyleConstants.setForeground(codeHeader, new Color(150, 200, 255)); // Light blue
    StyleConstants.setFontSize(codeHeader, 11);
    StyleConstants.setItalic(codeHeader, true);
    StyleConstants.setBold(codeHeader, true);

    // Timestamp style
    Style timestamp = doc.addStyle("timestamp", regular);
    StyleConstants.setForeground(timestamp, TEXT_SECONDARY);
    StyleConstants.setFontSize(timestamp, 11);
    StyleConstants.setBold(timestamp, false);

    // Sender style
    Style sender = doc.addStyle("sender", regular);
    StyleConstants.setBold(sender, true);
    StyleConstants.setForeground(sender, PRIMARY);
  }

  private JPanel createTitleBar() {
    JPanel titleBar = new JPanel(new BorderLayout());
    titleBar.setBackground(SURFACE);
    titleBar.setBorder(new EmptyBorder(8, 15, 8, 15));
    titleBar.setPreferredSize(new Dimension(frame.getWidth(), 40));

    DraggableWindow draggable = new DraggableWindow(frame);
    titleBar.addMouseListener(draggable);
    titleBar.addMouseMotionListener(draggable);

    // Title label (no icon)
    JLabel titleLabel = new JLabel("AI Coding Assistant");
    titleLabel.setForeground(TEXT_PRIMARY);
    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

    // Close button
    JButton closeButton = new JButton("×");
    closeButton.setFont(new Font("Arial", Font.BOLD, 18));
    closeButton.setForeground(TEXT_SECONDARY);
    closeButton.setBackground(null);
    closeButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
    closeButton.setFocusPainted(false);
    closeButton.setContentAreaFilled(false);
    closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

    closeButton.addActionListener(e -> frame.setVisible(false));

    closeButton.addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {
        closeButton.setForeground(new Color(220, 100, 100));
      }

      public void mouseExited(MouseEvent e) {
        closeButton.setForeground(TEXT_SECONDARY);
      }
    });

    titleBar.add(titleLabel, BorderLayout.WEST);
    titleBar.add(closeButton, BorderLayout.EAST);

    return titleBar;
  }

  private JButton createStyledButton(String text, Color color) {
    JButton button = new JButton(text) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (getModel().isPressed()) {
          g2d.setColor(color.darker());
        } else if (getModel().isRollover()) {
          g2d.setColor(color.brighter());
        } else {
          g2d.setColor(color);
        }

        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        super.paintComponent(g);
      }
    };

    button.setForeground(Color.WHITE);
    button.setFont(new Font("Segoe UI", Font.BOLD, 12));
    button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    button.setContentAreaFilled(false);
    button.setFocusPainted(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    return button;
  }

  private void centerWindow() {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (screenSize.width - frame.getWidth()) / 2;
    int y = (screenSize.height - frame.getHeight()) / 4;
    frame.setLocation(x, y);
  }

  private void sendMessage() {
    String message = inputArea.getText().trim();
    if (!message.isEmpty() && !message.equals("Ask me anything about coding... (Ctrl+Enter to send)")
        && ollamaClient != null) {

      addMessage("You", message, false);
      inputArea.setText("");

      sendButton.setEnabled(false);
      sendButton.setText("Processing...");

      new Thread(() -> {
        try {
          String response = ollamaClient.sendMessage(message);
          SwingUtilities.invokeLater(() -> {
            String cleanResponse = cleanDuplicateContent(response);
            addMessage("AI", cleanResponse, true);
            sendButton.setEnabled(true);
            sendButton.setText("Send");
          });
        } catch (Exception e) {
          SwingUtilities.invokeLater(() -> {
            addMessage("System", "Error: " + e.getMessage(), true);
            sendButton.setEnabled(true);
            sendButton.setText("Send");
          });
        }
      }).start();
    }
  }

  private String cleanDuplicateContent(String text) {
    if (text == null || text.isEmpty())
      return text;

    String[] lines = text.split("\n");
    StringBuilder cleaned = new StringBuilder();
    String previousLine = "";

    for (String line : lines) {
      String trimmedLine = line.trim();
      if (!trimmedLine.equals(previousLine)) {
        cleaned.append(line).append("\n");
        previousLine = trimmedLine;
      }
    }

    return cleaned.toString().trim();
  }

  private void copyLastCodeBlock() {
    try {
      String plainText = chatArea.getText();
      if (!plainText.isEmpty()) {
        String[] parts = plainText.split("```");
        if (parts.length > 1) {
          for (int i = parts.length - 1; i > 0; i--) {
            if (i % 2 == 1) {
              String codeBlock = parts[i].trim();
              if (codeBlock.contains("\n")) {
                String firstLine = codeBlock.substring(0, codeBlock.indexOf("\n")).trim();
                if (firstLine.length() < 20 && !firstLine.contains(" ")) {
                  codeBlock = codeBlock.substring(codeBlock.indexOf("\n") + 1);
                }
              }
              StringSelection selection = new StringSelection(codeBlock.trim());
              Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
              addMessage("System", "Code copied to clipboard", false);
              return;
            }
          }
        }
        addMessage("System", "No code block found to copy", false);
      }
    } catch (Exception e) {
      addMessage("System", "Failed to copy code: " + e.getMessage(), false);
    }
  }

  private void clearChat() {
    chatArea.setText("");
  }

  public void addMessage(String sender, String message, boolean isAI) {
    SwingUtilities.invokeLater(() -> {
      try {
        StyledDocument doc = chatArea.getStyledDocument();
        String timestamp = String.format("[%1$tH:%1$tM]", new java.util.Date());

        doc.insertString(doc.getLength(), timestamp + " ", doc.getStyle("timestamp"));
        doc.insertString(doc.getLength(), sender + ":\n", doc.getStyle("sender"));

        processMessageContent(doc, message);

        doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
        chatArea.setCaretPosition(doc.getLength());
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private void processMessageContent(StyledDocument doc, String message) throws BadLocationException {
    if (message == null || message.trim().isEmpty())
      return;

    if (message.contains("```")) {
      String[] parts = message.split("```", -1);

      for (int i = 0; i < parts.length; i++) {
        if (i % 2 == 0) {
          addFormattedText(doc, parts[i].trim(), "regular", i > 0);
        } else {
          String codeContent = parts[i].trim();
          String language = "";
          String actualCode = codeContent;

          int firstNewline = codeContent.indexOf("\n");
          if (firstNewline != -1) {
            String firstLine = codeContent.substring(0, firstNewline).trim();
            if (firstLine.length() > 0 && firstLine.length() < 20 && !firstLine.contains(" ")) {
              language = firstLine;
              actualCode = codeContent.substring(firstNewline + 1).trim();
            }
          }

          if (!language.isEmpty()) {
            doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
            doc.insertString(doc.getLength(), "▸ " + language.toUpperCase() + " CODE\n", doc.getStyle("codeHeader"));
          }

          addFormattedText(doc, actualCode, "code", true);
        }
      }
    } else {
      addFormattedText(doc, message.trim(), "regular", false);
    }
  }

  private void addFormattedText(StyledDocument doc, String text, String styleName, boolean addNewline)
      throws BadLocationException {
    if (text.isEmpty())
      return;

    Style style = doc.getStyle(styleName);

    if (addNewline) {
      doc.insertString(doc.getLength(), "\n", style);
    }

    if ("code".equals(styleName)) {
      String[] lines = text.split("\n");
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        if (!line.trim().isEmpty()) {
          doc.insertString(doc.getLength(), "    " + line, style);
        } else {
          doc.insertString(doc.getLength(), "    ", style);
        }
        if (i < lines.length - 1) {
          doc.insertString(doc.getLength(), "\n", style);
        }
      }
      doc.insertString(doc.getLength(), "\n", style);
    } else {
      String[] lines = text.split("\n");
      for (int i = 0; i < lines.length; i++) {
        if (!lines[i].trim().isEmpty()) {
          doc.insertString(doc.getLength(), lines[i], style);
          if (i < lines.length - 1) {
            doc.insertString(doc.getLength(), "\n", style);
          }
        }
      }
    }
  }

  public void setOllamaClient(OllamaClient ollamaClient) {
    this.ollamaClient = ollamaClient;
  }

  public void showWindow() {
    frame.setVisible(true);
    frame.toFront();
    frame.setState(Frame.NORMAL);
  }

  public void hideWindow() {
    frame.setVisible(false);
  }

  public boolean isVisible() {
    return frame.isVisible();
  }

  // Inner class for draggable window
  private static class DraggableWindow extends MouseAdapter {
    private final JFrame frame;
    private Point mouseDownCompCoords = null;

    public DraggableWindow(JFrame frame) {
      this.frame = frame;
    }

    public void mouseReleased(MouseEvent e) {
      mouseDownCompCoords = null;
    }

    public void mousePressed(MouseEvent e) {
      mouseDownCompCoords = e.getPoint();
    }

    public void mouseDragged(MouseEvent e) {
      Point currCoords = e.getLocationOnScreen();
      frame.setLocation(currCoords.x - mouseDownCompCoords.x,
          currCoords.y - mouseDownCompCoords.y);
    }
  }
}
