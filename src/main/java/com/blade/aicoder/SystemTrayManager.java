package com.blade.aicoder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class SystemTrayManager {
  private TrayIcon trayIcon;
  private SystemTray systemTray;
  private FloatingWindow floatingWindow;

  public SystemTrayManager() {
    if (!SystemTray.isSupported()) {
      System.err.println("System tray not supported!");
      return;
    }

    createTrayIcon();
  }

  private void createTrayIcon() {
    systemTray = SystemTray.getSystemTray();

    // Create tray icon image using the new method
    Image image = createModernTrayImage();

    // Create popup menu
    PopupMenu popup = new PopupMenu();

    MenuItem showItem = new MenuItem("📱 Show/Hide Window");
    MenuItem exitItem = new MenuItem("❌ Exit");

    showItem.addActionListener(e -> toggleWindow());
    exitItem.addActionListener(e -> exitApplication());

    popup.add(showItem);
    popup.addSeparator();
    popup.add(exitItem);

    // Create tray icon
    trayIcon = new TrayIcon(image, "AI Coding Assistant", popup);
    trayIcon.setImageAutoSize(true);

    // Add double-click listener
    trayIcon.addActionListener(e -> toggleWindow());

    // Add tooltip
    trayIcon.setToolTip("AI Coding Assistant\nDouble-click to show/hide");

    try {
      systemTray.add(trayIcon);
    } catch (AWTException e) {
      System.err.println("TrayIcon could not be added.");
    }
  }

  // NEW METHOD: Load icon from file with fallback
  private Image createModernTrayImage() {
    try {
      // Load from file (put your image in src/main/resources/icons/)
      // Note: This will look for the file in the classpath
      Image image = Toolkit.getDefaultToolkit().getImage(
          getClass().getResource("/icons/ai-icon.png"));
      return image;
    } catch (Exception e) {
      // Fallback to generated icon if file loading fails
      System.out.println("Could not load icon file, using fallback: " + e.getMessage());
      return createFallbackIcon();
    }
  }

  // NEW METHOD: Fallback icon generator
  private Image createFallbackIcon() {
    int size = 32;
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Simple fallback design
    g2d.setColor(new Color(100, 150, 255));
    g2d.fillOval(2, 2, size - 4, size - 4);

    g2d.setColor(Color.WHITE);
    g2d.setFont(new Font("Hack Nerd Mono", Font.BOLD, 16));
    g2d.drawString("AI", 10, 22);

    g2d.dispose();
    return image;
  }

  // REMOVE OR COMMENT OUT THE OLD createModernTrayImage METHOD
  /*
   * private Image createModernTrayImage() {
   * int size = 32;
   * BufferedImage image = new BufferedImage(size, size,
   * BufferedImage.TYPE_INT_ARGB);
   * Graphics2D g2d = image.createGraphics();
   * 
   * // ... old code ...
   * }
   */

  private void toggleWindow() {
    if (floatingWindow != null) {
      if (floatingWindow.isVisible()) {
        floatingWindow.hideWindow();
        showTrayNotification("AI Assistant minimized to tray", TrayIcon.MessageType.INFO);
      } else {
        floatingWindow.showWindow();
      }
    }
  }

  private void exitApplication() {
    // Remove tray icon
    if (trayIcon != null) {
      systemTray.remove(trayIcon);
    }
    System.exit(0);
  }

  private void showTrayNotification(String message, TrayIcon.MessageType type) {
    if (trayIcon != null) {
      trayIcon.displayMessage("AI Coding Assistant", message, type);
    }
  }

  public void setFloatingWindow(FloatingWindow floatingWindow) {
    this.floatingWindow = floatingWindow;
  }
}
