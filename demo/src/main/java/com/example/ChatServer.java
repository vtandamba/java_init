package com.example;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    // Port d'écoute du serveur
    private static final int PORT = 12345;
    
    // Ensemble thread-safe pour stocker les clients connectés
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    
    // Connexion à la base de données
    private static Connection dbConnection;

    public static void main(String[] args) {
        // Initialisation de la connexion à la base de données
        initDBConnection();

        // Création du serveur socket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Le serveur de chat est en écoute sur le port " + PORT);
            // Boucle infinie pour accepter les connexions clients
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion de " + clientSocket);
                
                // Création et démarrage d'un thread pour gérer le client
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    // Méthode pour initialiser la connexion JDBC à la base de données
    private static void initDBConnection() {
        try {
            // Chargement du driver (exemple avec MySQL)
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/chatdb?useSSL=false&serverTimezone=UTC";
            String user = "username";      // Remplacer par ton nom d'utilisateur DB
            String password = "password";  // Remplacer par ton mot de passe DB
            dbConnection = DriverManager.getConnection(url, user, password);
            System.out.println("Connexion à la base de données réussie.");
        } catch (Exception e) {
            System.err.println("Erreur lors de la connexion à la base de données.");
            e.printStackTrace();
        }
    }
    
    // Méthode pour diffuser un message à tous les clients, sauf celui qui a envoyé
    public static void broadcast(String message, ClientHandler excludeClient) {
        for (ClientHandler client : clientHandlers) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }
    
    // Méthode pour retirer un client de l'ensemble lors de la déconnexion
    public static void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
    }
    
    // Méthode pour enregistrer un message dans la base de données
    // Ici, nous insérons le message dans la table 'messages' en considérant qu'il s'agit d'un message privé
    public static void saveMessageToDB(String sender, String message) {
        // Pour simplifier, on considère ici que l'ID de l'expéditeur est récupéré via une requête ou passé directement.
        // Dans cet exemple, nous utilisons une valeur fictive (1). Dans un projet complet, il faudra convertir le nom d'utilisateur en ID.
        String sql = "INSERT INTO messages (id_expediteur, contenu, type_message, date_envoi) VALUES (?, ?, 'prive', NOW())";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, 1); // Remplacer par l'ID réel de l'expéditeur
            stmt.setString(2, sender + ": " + message);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'enregistrement du message dans la BDD.");
            e.printStackTrace();
        }
    }
    
    // Classe interne gérant la connexion d'un client
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                // Initialisation des flux d'entrée et de sortie
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // Authentification simplifiée : demande du nom d'utilisateur
                out.println("Entrez votre nom :");
                clientName = in.readLine();
                System.out.println("Client connecté : " + clientName);
                
                // Notification à tous que ce client a rejoint le chat
                broadcast(clientName + " a rejoint le chat.", this);
                
                // Lecture continue des messages envoyés par le client
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    System.out.println(clientName + " : " + clientMessage);
                    // Enregistrement du message dans la base de données
                    saveMessageToDB(clientName, clientMessage);
                    // Diffusion du message à tous les autres clients
                    broadcast(clientName + " : " + clientMessage, this);
                }
            } catch (IOException e) {
                System.err.println("Erreur de communication avec " + clientName);
                e.printStackTrace();
            } finally {
                // Nettoyage lors de la déconnexion du client
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(clientName + " s'est déconnecté.");
                removeClient(this);
                broadcast(clientName + " a quitté le chat.", this);
            }
        }
        
        // Méthode pour envoyer un message à ce client
        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
