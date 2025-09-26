package com.blade.aicoder;

import javax.swing.*;

public class AICodingAssistant {
  public static void main(String[] args) {
    // Set system look and feel
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Run in EDT
    SwingUtilities.invokeLater(() -> {
      // Create system tray first
      SystemTrayManager trayManager = new SystemTrayManager();

      // Create floating window
      FloatingWindow floatingWindow = new FloatingWindow();

      // Create Ollama client
      OllamaClient ollamaClient = new OllamaClient(floatingWindow);

      // Connect components
      floatingWindow.setOllamaClient(ollamaClient);
      trayManager.setFloatingWindow(floatingWindow);

      // Show initial message (ONLY ONCE)
      floatingWindow.addMessage("System", "AI Coding Assistant started! Make sure Ollama is running.", false);
    });
  }
}
