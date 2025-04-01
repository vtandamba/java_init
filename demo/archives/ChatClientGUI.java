package com.example.archives;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class ChatClientGUI {
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String username;

    public ChatClientGUI() {
        showLoginWindow();
    }

    private void showLoginWindow() {
        JFrame loginFrame = new JFrame("Connexion ou Inscription");
        loginFrame.setSize(350, 250);
        loginFrame.setLayout(new GridLayout(5, 1));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JTextField emailField = new JTextField();
        JButton loginButton = new JButton("Connexion");
        JButton registerButton = new JButton("Inscription");

        loginFrame.add(new JLabel("Nom d'utilisateur :"));
        loginFrame.add(usernameField);
        loginFrame.add(new JLabel("Mot de passe :"));
        loginFrame.add(passwordField);
        loginFrame.add(new JLabel("Email (pour inscription) :"));
        loginFrame.add(emailField);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        loginFrame.add(buttonPanel);

        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setVisible(true);

        loginButton.addActionListener(e -> {
            try {
                connectToServer();
                out.println("1");
                out.println(usernameField.getText());
                out.println(new String(passwordField.getPassword()));
                String response = in.readLine();
                if (response.contains("réussie")) {
                    this.username = usernameField.getText();
                    loginFrame.dispose();
                    showChatWindow();
                    new Thread(this::receiveMessages).start();
                } else {
                    JOptionPane.showMessageDialog(loginFrame, "Échec de la connexion");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        registerButton.addActionListener(e -> {
            try {
                connectToServer();
                out.println("2");
                out.println(usernameField.getText());
                out.println(new String(passwordField.getPassword()));
                out.println(emailField.getText());
                String response = in.readLine();
                JOptionPane.showMessageDialog(loginFrame, response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void showChatWindow() {
        frame = new JFrame("Chat - " + username);
        frame.setSize(500, 400);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField();
        sendButton = new JButton("Envoyer");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void connectToServer() throws IOException {
        socket = new Socket("localhost", 12345);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void sendMessage() {
        String message = inputField.getText();
        out.println(message);
        inputField.setText("");
    }

    private void receiveMessages() {
        try {
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                messageArea.append(serverMessage + "\n");
            }
        } catch (IOException e) {
            messageArea.append("Connexion au serveur perdue.\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}
