package com.example.archives;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JComboBox<String> modeCombo;
    private JTextField targetField;
    private JList<String> usersList;
    private DefaultListModel<String> usersListModel;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ChatClient(String host, int port) {
        authenticateUser(host, port);
        initInterface();
        startMessageListener();
    }

    // Connexion et authentification
    private void authenticateUser(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String prompt = in.readLine();
            if (prompt == null) throw new IOException("Aucune réponse serveur");

            String[] options = { "Connexion", "Inscription" };
            int choice = JOptionPane.showOptionDialog(null, prompt, "Authentification",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                username = JOptionPane.showInputDialog("Nom d'utilisateur:");
                String password = JOptionPane.showInputDialog("Mot de passe:");
                out.println("1");
                out.println(username);
                out.println(password);
                String response = in.readLine();
                if (response == null || !response.toLowerCase().contains("réussie")) {
                    showErrorAndExit("Connexion échouée: " + response);
                }
            } else { // Inscription
                username = JOptionPane.showInputDialog("Nom d'utilisateur:");
                String password = JOptionPane.showInputDialog("Mot de passe:");
                String email = JOptionPane.showInputDialog("Email:");
                out.println("2"); // Indique le choix d'inscription
                out.println(username);
                out.println(password);
                out.println(email);
                String response = in.readLine();
                System.out.println(response);
                if (!response.contains("réussie")) {
                    JOptionPane.showMessageDialog(null, "Échec de l'inscription: " + response);
                    System.exit(0);
                }
            }

        } catch (IOException e) {
            showErrorAndExit("Erreur de connexion au serveur.");
        }
    }

    // Création de l'interface graphique
    private void initInterface() {
        setTitle("Chat Client - " + username);
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new FlowLayout());
        modeCombo = new JComboBox<>(new String[] { "Public", "Privé", "Groupe" });
        targetField = new JTextField(10);
        targetField.setToolTipText("Destinataire ou groupe");
        topPanel.add(new JLabel("Mode:"));
        topPanel.add(modeCombo);
        topPanel.add(new JLabel("Cible:"));
        topPanel.add(targetField);
        add(topPanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Envoyer");
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        usersListModel = new DefaultListModel<>();
        usersList = new JList<>(usersListModel);
        JScrollPane usersScroll = new JScrollPane(usersList);
        usersScroll.setPreferredSize(new Dimension(150, 0));
        add(usersScroll, BorderLayout.EAST);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Lecture continue des messages du serveur
    private void startMessageListener() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    String finalMsg = msg;
                    SwingUtilities.invokeLater(() -> {
                        if (finalMsg.startsWith("/users ")) {
                            updateUserList(finalMsg.substring(7));
                        } else {
                            chatArea.append(finalMsg + "\n");
                        }
                    });
                }
            } catch (IOException e) {
                chatArea.append("❌ Connexion perdue.\n");
            }
        }).start();
    }

    // Envoi de message
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;

        String mode = (String) modeCombo.getSelectedItem();
        String target = targetField.getText().trim();

        switch (mode) {
            case "Public":
                out.println(message);
                break;
            case "Privé":
                if (target.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Destinataire requis pour message privé.");
                    return;
                }
                out.println("/private " + target + " " + message);
                break;
            case "Groupe":
                if (target.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Nom du groupe requis.");
                    return;
                }
                out.println("/group " + target + " " + message);
                break;
        }

        messageField.setText("");
    }

    // Mise à jour de la liste d'utilisateurs
    private void updateUserList(String usersCSV) {
        usersListModel.clear();
        for (String user : usersCSV.split(",")) {
            if (!user.trim().isEmpty()) {
                usersListModel.addElement(user.trim());
            }
        }
    }

    // En cas d'erreur fatale
    private void showErrorAndExit(String message) {
        JOptionPane.showMessageDialog(null, message);
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient("localhost", 12345));
    }
}
