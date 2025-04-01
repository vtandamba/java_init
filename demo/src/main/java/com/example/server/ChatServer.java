package com.example.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 1000;
    private static Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static Connection db;
    public static Set<ClientHandler> getClients() {
        return clients;
    }
    
    public static void main(String[] args) {
        connectDB();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println(" Serveur en ligne sur le port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("🔌 Client connecté : " + socket);
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            db = DriverManager.getConnection("jdbc:mysql://localhost:3306/chat_app", "root", "MeeJeediexo3*");
            System.out.println(" Connexion à la base de données réussie");
        } catch (Exception e) {
            System.err.println(" Connexion BDD échouée");
            e.printStackTrace();
        }
    }

    public static boolean registerUser(String username, String email, String password) {
        try {
            PreparedStatement check = db.prepareStatement("SELECT COUNT(*) FROM utilisateurs WHERE email = ?");
            check.setString(1, email);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) > 0)

                return false;

            PreparedStatement insert = db.prepareStatement(
                    "INSERT INTO utilisateurs (nom, email, mot_de_passe, statut, created_at) VALUES (?, ?, ?, 'en ligne', NOW())");
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
            PreparedStatement stmt = db.prepareStatement("SELECT mot_de_passe FROM utilisateurs WHERE nom = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String stored = rs.getString("mot_de_passe");
                return stored.equals(password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void broadcast(String msg, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.send(msg);
            }
        }
    }

    public static void addClient(ClientHandler client) {
        clients.add(client);
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public static Connection getDB() {
        return db;
    }

}
