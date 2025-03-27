package com.example.client;
// --------------------- RegisterWindow.java ---------------------
 

import javax.swing.*;
import java.awt.*;

public class RegisterWindow extends JFrame {
    public RegisterWindow() {
        setTitle("Inscription");
        setSize(300, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 2));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JTextField emailField = new JTextField();
        JButton registerBtn = new JButton("S'inscrire");

        add(new JLabel("Nom d'utilisateur:"));
        add(usernameField);
        add(new JLabel("Mot de passe:"));
        add(passwordField);
        add(new JLabel("Email:"));
        add(emailField);
        add(new JLabel());
        add(registerBtn);

        registerBtn.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String email = emailField.getText();
            boolean success = ChatClient.register(username, password, email);
            if (success) {
                int result = JOptionPane.showConfirmDialog(this, "Inscription réussie ! Se connecter ?", "Succès", JOptionPane.YES_NO_OPTION);
                dispose();
                if (result == JOptionPane.YES_OPTION) {
                    new LoginWindow();
                } else {
                    new AuthWindow();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de l'inscription.");
            }
        });

        setVisible(true);
    }
}

