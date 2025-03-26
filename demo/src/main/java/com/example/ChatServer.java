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
        boolean success = registerUser("roo", "nouvelemail@example.com", "monMotDePasseHaché");
        if (success) {
            System.out.println("L'utilisateur a été inscrit avec succès !");
        } else {
            System.out.println("Erreur lors de l'inscription de l'utilisateur.");
        }


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
            String url = "jdbc:mysql://localhost:3306/chat_app";
            String user = "root";      // Remplacer par ton nom d'utilisateur DB
            String password = "MeeJeediexo3*";  // Remplacer par ton mot de passe DB
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
        String sql = "INSERT INTO messages (id_expediteur, contenu, timestamp) VALUES (?, ?, NOW())";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, 1); // Remplacer par l'ID réel de l'expéditeur
            stmt.setString(2, sender + ": " + message);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'enregistrement du message dans la BDD.");
            e.printStackTrace();
        }
    }
    public static boolean registerUser(String username, String email, String password) {
        if (dbConnection == null) {
            System.err.println("Erreur : La connexion à la base de données n'est pas initialisée.");
            return false;
        }

        String statut = "en ligne";

        try {
            // Nettoyage des paramètres
            email = email.trim(); // Supprime les espaces avant et après
            username = username.trim();

            // Log pour vérifier les entrées
            System.out.println("Tentative d'inscription : username=" + username + ", email=" + email + ", password=" + password);

            // Vérification de l'email
            String checkEmailSql = "SELECT COUNT(*) FROM utilisateurs WHERE email = ?";
            try (PreparedStatement checkStmt = dbConnection.prepareStatement(checkEmailSql)) {
                checkStmt.setString(1, email); // Paramétrer l'email
                ResultSet resultSet = checkStmt.executeQuery();
                if (resultSet.next()) {
                    int emailCount = resultSet.getInt(1);
                    System.out.println("Nombre de comptes trouvés pour l'email '" + email + "': " + emailCount);
                    if (emailCount > 0) {
                        System.err.println("Erreur : L'email '" + email + "' existe déjà dans la base de données.");
                        return false;
                    }
                }
            }

            // Insérer un nouveau compte utilisateur
            String sql = "INSERT INTO utilisateurs (nom, email, mot_de_passe, statut, created_at) VALUES (?, ?, ?, ?, NOW())";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, email);
                stmt.setString(3, password);
                stmt.setString(4, statut);

                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("Utilisateur enregistré avec succès : " + username);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'inscription de l'utilisateur : " + username);
            e.printStackTrace();
        }
        return false;
    }
    public static boolean authenticateUser(String username, String password) {
        String sql = "SELECT mot_de_passe FROM utilisateurs WHERE nom = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("mot_de_passe");
                //return BCrypt.checkpw(password, storedPassword); // Vérifie le mot de passe
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'authentification.");
            e.printStackTrace();
        }
        return false;
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

                // Demande au client de choisir entre Connexion ou Inscription
                out.println("Bienvenue ! Choisissez une option : Connexion (1) ou Inscription (2)");
                String choice = in.readLine(); // Lire le choix du client

                if ("1".equals(choice)) { // Connexion
                    // Demande des informations de connexion
                    out.println("Nom d'utilisateur:");
                    String username = in.readLine();

                    out.println("Mot de passe:");
                    String password = in.readLine();

                    // Vérifier les informations du client (Vous devez implémenter `authenticateUser`)
                    boolean isAuthenticated = ChatServer.authenticateUser(username, password);

                    if (isAuthenticated) {
                        clientName = username; // Assigne le nom d'utilisateur après succès
                        out.println("Connexion réussie !");
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
                    } else {
                        out.println("Connexion échouée : nom d'utilisateur ou mot de passe incorrect.");
                        System.err.println("Connexion échouée pour : " + username);
                    }
                } else if ("2".equals(choice)) { // Inscription
                    // Demande des informations d'inscription
                    out.println("Nom d'utilisateur:");
                    String username = in.readLine();

                    out.println("Mot de passe:");
                    String password = in.readLine();

                    out.println("Email:");
                    String email = in.readLine();

                    // Enregistrer l'utilisateur dans la base de données
                    boolean isRegistered = ChatServer.registerUser(username, email, password);

                    if (isRegistered) {
                        out.println("Inscription réussie !");
                        System.out.println("Nouvel utilisateur inscrit : " + username);
                    } else {
                        out.println("Échec : L'inscription a échoué. Vérifiez vos informations ou essayez un autre email.");
                        System.err.println("Échec de l'inscription pour : " + username + ", email : " + email);
                    }
                } else {
                    out.println("Option invalide. Veuillez redémarrer et choisir 1 (Connexion) ou 2 (Inscription).");
                }
            } catch (IOException e) {
                System.err.println("Erreur de communication avec le client.");
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

                // Notifier les autres utilisateurs de la déconnexion s'il s'agissait d'une session connectée
                if (clientName != null) {
                    System.out.println(clientName + " s'est déconnecté.");
                    removeClient(this);
                    broadcast(clientName + " a quitté le chat.", this);
                }
            }
        }
        
        // Méthode pour envoyer un message à ce client
        public void sendMessage(String message) {
            out.println(message);
        }
    }
}