package com.example.client;


import javax.swing.*;
import java.awt.*;
 
public class ChatUI extends JFrame {
    JTextArea chatArea;
    JTextField messageField;
    JButton sendButton;

    public ChatUI(String username) {
        setTitle("Chat - " + username);
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Envoyer");
        bottom.add(messageField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        // Recevoir les messages en arrière-plan
        new Thread(() -> {
            String msg;
            try {
                while ((msg = ChatClient.readMessage()) != null) {
                    chatArea.append(msg + "\n");
                }
            } catch (Exception e) {
                chatArea.append("Déconnecté du serveur.\n");
            }
        }).start();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            ChatClient.send(text);
            messageField.setText("");
        }
    }
}
