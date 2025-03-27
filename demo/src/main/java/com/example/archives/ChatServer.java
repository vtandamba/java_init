package com.example.archives;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JOptionPane;

public class ChatServer {

    private static final int PORT = 12345;
    private static Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static Connection dbConnection;

    public static void main(String[] args) {
        initDB();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Serveur lancé sur le port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("🟢 Nouveau client connecté : " + socket);

                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                new Thread(client).start();
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur serveur : " + e.getMessage());
        }
    }

    // Connexion à la base de données
    private static void initDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dbConnection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/chat_app", "root", "MeeJeediexo3*"
            );
            System.out.println("✅ Connexion à la base de données OK");
        } catch (Exception e) {
            System.err.println("❌ Échec connexion BDD");
            e.printStackTrace();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public static boolean registerUser(String username, String email, String password) {
        try {
            PreparedStatement check = dbConnection.prepareStatement(
                "SELECT COUNT(*) FROM utilisateurs WHERE email = ?"
            );
            check.setString(1, email);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return false;

            PreparedStatement insert = dbConnection.prepareStatement(
                "INSERT INTO utilisateurs (nom, email, mot_de_passe, statut, created_at) VALUES (?, ?, ?, 'en ligne', NOW())"
            );
            insert.setString(1, username);
            insert.setString(2, email);
            insert.setString(3, password);
            return insert.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean authenticateUser(String username, String password) {
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT mot_de_passe FROM utilisateurs WHERE nom = ?"
            );
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String stored = rs.getString("mot_de_passe");
                return stored.equals(password); // Remplace par BCrypt si besoin
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void saveMessage(String sender, String message) {
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO messages (id_expediteur, contenu, timestamp) VALUES (?, ?, NOW())"
            );
            stmt.setInt(1, 1); // ← à remplacer par vrai ID si tu veux plus tard
            stmt.setString(2, sender + ": " + message);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur save message DB");
            e.printStackTrace();
        }
    }

    // Classe interne pour chaque client
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Bienvenue ! Connexion (1) ou Inscription (2)");
                String[] options = { "Connexion", "Inscription" };
                String prompt = in.readLine();
                if (prompt == null) {
                    JOptionPane.showMessageDialog(null, "Erreur : le serveur n'a pas fourni de réponse.");
                    System.exit(0);
                }
    
            int choice = JOptionPane.showOptionDialog(null, prompt, "Authentification",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (choice == 0) { // Connexion
                username = JOptionPane.showInputDialog("Nom d'utilisateur:");
                String password = JOptionPane.showInputDialog("Mot de passe:");
                out.println("1"); // Indique le choix de connexion
                out.println(username);
                out.println(password);
                String response = "failed";
                if (username != null && password != null) {
                    response = "réussie";
                    JOptionPane.showMessageDialog(null, "✅ Connexion réussie ! Redirection vers la connexion.");
                }
                if (!response.contains("réussie")) {
                    JOptionPane.showMessageDialog(null, "Échec de l'inscription: " + response);
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
                String response ="failed";
                if (username != null && password != null && email != null) { 
                    response = "réussie";
                    JOptionPane.showMessageDialog(null, "✅ Inscription réussie ! Redirection vers la connexion.");
                }
                if (!response.contains("réussie")) {
                    JOptionPane.showMessageDialog(null, "Échec de l'inscription: " + response);
                    System.exit(0);
                }
            }

            } catch (IOException e) {
                System.err.println("❌ Erreur client");
            } finally {
                closeConnection();
            }
        }

        private void listenForMessages() throws IOException {
            String msg;
            while ((msg = in.readLine()) != null) {
                System.out.println("📩 " + username + " : " + msg);
                saveMessage(username, msg);
                broadcast(username + ": " + msg, this);
            }
            System.out.println("❌ Fin de la lecture des messages : flux coupé.");
        }
        

        public void sendMessage(String msg) {
            out.println(msg);
        }

        private void closeConnection() {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (username != null) {
                System.out.println("🔴 " + username + " s'est déconnecté.");
                removeClient(this);
                broadcast(username + " a quitté le chat.", this);
            }
        }
    }
}
