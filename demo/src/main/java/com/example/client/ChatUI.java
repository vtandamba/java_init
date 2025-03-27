package com.example.client;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatUI extends JFrame {
    private JTextField messageField;
    private JTextField targetField;
    private JButton sendButton;
    private JComboBox<String> modeSelector;
    private JComboBox<String> filterSelector;
    private JTabbedPane chatTabs;
    private Map<String, JTextArea> chatPanels;
    private String currentUser;

    public ChatUI(String username) {
        this.currentUser = username;
        setTitle("Chat - " + username);
        setSize(650, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatTabs = new JTabbedPane();
        chatPanels = new HashMap<>();
        add(chatTabs, BorderLayout.CENTER);

        // Panel haut (mode + destinataire + filtre)
        JPanel topPanel = new JPanel(new FlowLayout());
        modeSelector = new JComboBox<>(new String[] { "Public", "Privé", "Groupe" });
        targetField = new JTextField(10);
        filterSelector = new JComboBox<>(new String[] { "Répertoire", "Public", "Privé", "Groupe", "All" });
        filterSelector.addActionListener(e -> {
            String selected = (String) filterSelector.getSelectedItem();
            if (selected.equals("Répertoire")) {
                // Vide l'onglet Répertoire avant de recharger
                JTextArea area = chatPanels.get("Répertoire");
                if (area != null) area.setText("");
        
                // Demande au serveur tous les messages reçus
                ChatClient.send("/repertoire");
            }
        });
        
        topPanel.add(new JLabel("Mode:"));
        topPanel.add(modeSelector);
        topPanel.add(new JLabel("Destinataire:"));
        topPanel.add(targetField);
        topPanel.add(new JLabel("Filtre:"));
        topPanel.add(filterSelector);
        add(topPanel, BorderLayout.NORTH);

        // Panel bas (champ de texte + bouton envoyer)
        JPanel bottom = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Envoyer");
        bottom.add(messageField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        // Onglet par défaut
        addChatTab("Répertoire");

        // Thread de réception des messages
        new Thread(() -> {
            String msg;
            try {
                while ((msg = ChatClient.readMessage()) != null) {
                    displayIncomingMessage(msg);
                }
            } catch (Exception e) {
                displayIncomingMessage("[Erreur] Déconnecté du serveur.");
            }
        }).start();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        String mode = (String) modeSelector.getSelectedItem();
        String target = targetField.getText().trim();
        String time = new SimpleDateFormat("HH:mm").format(new Date());

        if (mode.equals("Public")) {
            ChatClient.send(text);
            displayOutgoingMessage("[Public] Moi (" + time + ") : " + text);
        } else if (mode.equals("Privé")) {
            if (!target.isEmpty()) {
                ChatClient.send("/private " + target + " " + text);
                displayOutgoingMessage("[" + target + "] Moi (" + time + ") : " + text);
            }
        } else if (mode.equals("Groupe")) {
            if (!target.isEmpty()) {
                ChatClient.send("/group " + target + " " + text);
                displayOutgoingMessage("[" + target + "] Moi (" + time + ") : " + text);
            }
        }

        messageField.setText("");
    }

    private void displayOutgoingMessage(String msg) {
        String key = "Répertoire";
        if (msg.startsWith("[") && msg.contains("]")) {
            int end = msg.indexOf("]");
            key = msg.substring(1, end);
            addChatTab(key);
        }
        JTextArea targetArea = chatPanels.getOrDefault(key, chatPanels.get("Répertoire"));
        targetArea.append(msg + "\n");
    }

    private void addChatTab(String name) {
        if (!chatPanels.containsKey(name)) {
            JTextArea area = new JTextArea();
            area.setEditable(false);
            chatPanels.put(name, area);
            chatTabs.addTab(name, new JScrollPane(area));
        }
    }

    private void displayIncomingMessage(String msg) {
        String key = "Répertoire";
        if (msg.startsWith("[") && msg.contains("]")) {
            int end = msg.indexOf("]");
            key = msg.substring(1, end);
            addChatTab(key);
        }

        String filter = (String) filterSelector.getSelectedItem();
        if (filter.equals("All") || key.equals(filter) || filter.equals("Répertoire")) {
            JTextArea targetArea = chatPanels.getOrDefault(key, chatPanels.get("Répertoire"));
            targetArea.append(msg + "\n");
        }
    }
}
