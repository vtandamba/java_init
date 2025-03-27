package com.example.client;

// --------------------- AuthWindow.java ---------------------
 
import javax.swing.*;
import java.awt.*;

public class AuthWindow extends JFrame {
    public AuthWindow() {
        setTitle("Bienvenue sur le Chat");
        setSize(300, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        JButton loginBtn = new JButton("Connexion");
        JButton registerBtn = new JButton("Inscription");

        loginBtn.addActionListener(e -> {
            dispose();
            new LoginWindow();
        });

        registerBtn.addActionListener(e -> {
            dispose();
            new RegisterWindow();
        });

        add(new JLabel("Choisissez une option :"));
        add(loginBtn);
        add(registerBtn);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AuthWindow::new);
    }
}


