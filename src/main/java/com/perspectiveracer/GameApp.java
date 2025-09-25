package com.perspectiveracer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Entry point for the "Perspective Highway" game.
 */
public final class GameApp {
    private GameApp() {
        // Utility class
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Perspective Highway");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            GamePanel panel = new GamePanel();
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.start();
        });
    }
}
