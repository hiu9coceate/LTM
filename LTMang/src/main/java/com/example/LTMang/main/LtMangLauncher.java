package com.example.LTMang.main;

import com.example.LTMang.core.AppController;
import com.example.LTMang.services.network.server.SimpleServer;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Random;

public class LtMangLauncher {
    public static void main(String[] args) {
        try {
            new SimpleServer(8887).start();
            System.out.println(">> [LAUNCHER] Đã bật Server nội bộ.");
        } catch (Exception e) {
            System.err.println("⚠️ Không thể bật Server (Có thể đã có cái khác đang chạy): " + e.getMessage());
        }

        String myId = String.valueOf(100000 + new Random().nextInt(900000));

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("SmartStream OCR (All-in-One)");
            f.setSize(450, 200);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLayout(new BorderLayout());
            f.getContentPane().setBackground(new Color(30, 30, 30));

            JPanel pnlCenter = new JPanel(new GridBagLayout());
            pnlCenter.setOpaque(false);

            JLabel lblTitle = new JLabel("ID CỦA BẠN");
            lblTitle.setForeground(Color.LIGHT_GRAY);
            lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 14));

            JLabel lblId = new JLabel(myId);
            lblId.setFont(new Font("Arial", Font.BOLD, 40));
            lblId.setForeground(new Color(0, 255, 127));

            JButton btnCopy = new JButton("📋 COPY");
            btnCopy.setFont(new Font("SansSerif", Font.BOLD, 12));
            btnCopy.setBackground(new Color(50, 50, 50));
            btnCopy.setForeground(new Color(0, 255, 127));
            btnCopy.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 127), 2));
            btnCopy.setFocusPainted(false);
            btnCopy.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnCopy.addActionListener(e -> {
                StringSelection ss = new StringSelection(myId);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                btnCopy.setText("✅ COPIED!");
                btnCopy.setForeground(new Color(50, 200, 50));
                Timer timer = new Timer(2000, evt -> {
                    btnCopy.setText("📋 COPY");
                    btnCopy.setForeground(new Color(0, 255, 127));
                });
                timer.setRepeats(false);
                timer.start();
            });

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; gbc.gridy = 0; pnlCenter.add(lblTitle, gbc);
            gbc.gridy = 1;
            JPanel pnlIdRow = new JPanel();
            pnlIdRow.setOpaque(false);
            pnlIdRow.add(lblId);
            pnlIdRow.add(Box.createHorizontalStrut(10));
            pnlIdRow.add(btnCopy);
            pnlCenter.add(pnlIdRow, gbc);

            f.add(pnlCenter, BorderLayout.CENTER);

            JLabel status = new JLabel("  ✅ Server: ON (8887) | 🌐 App: Ready", SwingConstants.LEFT);
            status.setForeground(Color.WHITE);
            status.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            f.add(status, BorderLayout.SOUTH);

            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });

        new Thread(() -> {
            try { Thread.sleep(1000); } catch(Exception e){}
            AppController.startSharing(myId);
        }).start();
    }
}

