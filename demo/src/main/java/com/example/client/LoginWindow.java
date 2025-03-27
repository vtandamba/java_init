package com.example.client;

// --------------------- LoginWindow.java ---------------------
 

import javax.swing.*;
import java.awt.*;

public class LoginWindow extends JFrame {
    public LoginWindow() {
        setTitle("Connexion");
        setSize(300, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 2));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton loginBtn = new JButton("Se connecter");

        add(new JLabel("Nom d'utilisateur:"));
        add(usernameField);
        add(new JLabel("Mot de passe:"));
        add(passwordField);
        add(new JLabel());
        add(loginBtn);

        loginBtn.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            boolean connected = ChatClient.connect(username, password);
            if (connected) {
                JOptionPane.showMessageDialog(this, "Connexion réussie !");
                dispose();
                new ChatUI(username);
            } else {
                JOptionPane.showMessageDialog(this, "Identifiants incorrects.");
            }
        });

        setVisible(true);
    }
}
