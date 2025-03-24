package com.example;

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
        // Procédure d'authentification/inscription
        authenticateUser(host, port);

        // Configuration de l'interface principale
        setTitle("Chat Client - " + username);
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Zone d'affichage du chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        add(chatScroll, BorderLayout.CENTER);

        // Panel supérieur pour choisir le mode et la cible
        JPanel topPanel = new JPanel(new FlowLayout());
        modeCombo = new JComboBox<>(new String[] { "Public", "Privé", "Groupe" });
        targetField = new JTextField(10);
        targetField.setToolTipText("Destinataire ou nom du groupe (pour Privé/Groupe)");
        topPanel.add(new JLabel("Mode:"));
        topPanel.add(modeCombo);
        topPanel.add(new JLabel("Cible:"));
        topPanel.add(targetField);
        add(topPanel, BorderLayout.NORTH);

        // Panel inférieur pour saisir et envoyer le message
        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Envoyer");
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Panel latéral pour afficher la liste des utilisateurs connectés
        usersListModel = new DefaultListModel<>();
        usersList = new JList<>(usersListModel);
        JScrollPane usersScroll = new JScrollPane(usersList);
        usersScroll.setPreferredSize(new Dimension(150, 0));
        add(usersScroll, BorderLayout.EAST);

        // Actions d'envoi (clic sur le bouton ou appui sur Entrée)
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        messageField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        setVisible(true);

        // Thread pour lire les messages du serveur
        new Thread(new Runnable() {
            public void run() {
                readMessages();
            }
        }).start();
    }

    // Procédure d'authentification et/ou d'inscription
    private void authenticateUser(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Le serveur demande de choisir connexion ou inscription
            String prompt = in.readLine();
            String[] options = { "Connexion", "Inscription" };
            int choice = JOptionPane.showOptionDialog(null, prompt, "Authentification",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice == 0) { // Connexion
                username = JOptionPane.showInputDialog("Nom d'utilisateur:");
                String password = JOptionPane.showInputDialog("Mot de passe:");
                out.println("1"); // Indique le choix de connexion
                out.println(username);
                out.println(password);
                String response = in.readLine();
                if (!response.contains("réussie")) {
                    JOptionPane.showMessageDialog(null, "Échec de la connexion: " + response);
                    System.exit(0);
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
                if (!response.contains("réussie")) {
                    JOptionPane.showMessageDialog(null, "Échec de l'inscription: " + response);
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Erreur de connexion au serveur.");
            e.printStackTrace();
            System.exit(0);
        }
    }

    // Envoi du message en fonction du mode choisi
    private void sendMessage() {
        String mode = (String) modeCombo.getSelectedItem();
        String target = targetField.getText().trim();
        String message = messageField.getText().trim();
        if (message.isEmpty())
            return;

        if (mode.equals("Public")) {
            out.println(message);
        } else if (mode.equals("Privé")) {
            if (target.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Veuillez spécifier le destinataire pour un message privé.");
                return;
            }
            out.println("/private " + target + " " + message);
        } else if (mode.equals("Groupe")) {
            if (target.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Veuillez spécifier le nom du groupe pour un message de groupe.");
                return;
            }
            out.println("/group " + target + " " + message);
        }
        messageField.setText("");
    }

    // Lecture des messages envoyés par le serveur
    private void readMessages() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                // Mise à jour de la liste des utilisateurs si le message commence par "/users "
                if (msg.startsWith("/users ")) {
                    String users = msg.substring(7);
                    String[] userArray = users.split(",");
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            usersListModel.clear();
                            for (String user : userArray) {
                                if (!user.trim().isEmpty()) {
                                    usersListModel.addElement(user);
                                }
                            }
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                           // chatArea.append(msg.toString() + "\n");
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient("localhost", 12345));
    }
}
