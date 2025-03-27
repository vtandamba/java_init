// --------------------- ClientHandler.java ---------------------
package com.example.server;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private int userId;
    private MessageDAO messageDAO = new MessageDAO(ChatServer.getDB());

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Bienvenue ! Connexion (1) ou Inscription (2)");
            String choice = in.readLine();

            if ("1".equals(choice)) {
                out.println("Nom d'utilisateur:");
                String user = in.readLine();
                out.println("Mot de passe:");
                String pass = in.readLine();
                try {
                    if (ChatServer.authenticateUser(user, pass)) {
                        username = user;
                        this.userId = messageDAO.getUserIdByName(user);
                        int unread;

                        unread = messageDAO.getUnreadNotificationCount(userId);

                        if (unread > 0) {
                            send("🔔 Vous avez " + unread + " message(s) non lu(s).");
                        }
                        out.println("Connexion réussie !");
                        ChatServer.addClient(this);
                        readMessages();
                    } else {
                        out.println("Connexion échouée.");
                        socket.close();
                    }
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if ("2".equals(choice)) {
                out.println("Nom d'utilisateur:");
                String user = in.readLine();
                out.println("Mot de passe:");
                String pass = in.readLine();
                out.println("Email:");
                String email = in.readLine();

                if (ChatServer.registerUser(user, email, pass)) {
                    out.println("Inscription réussie !");
                } else {
                    out.println("Inscription échouée.");
                }
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (username != null) {
                ChatServer.removeClient(this);
            }
        }
    }

    private void readMessages() throws IOException {
        String msg;
        while ((msg = in.readLine()) != null) {
            if (msg.startsWith("/private")) {
                // Format : /private destinataire contenu
                String[] parts = msg.split(" ", 3);
                if (parts.length < 3)
                    continue;

                String targetUser = parts[1];
                String content = parts[2];

                try {
                    int targetId = messageDAO.getUserIdByName(targetUser);
                    int messageId = messageDAO.saveMessage(userId, targetId, null, content);

                    boolean delivered = false;
                    for (ClientHandler client : ChatServer.getClients()) {
                        if (client.username != null && client.username.equals(targetUser)) {
                            client.send("[" + username + "] (" + now() + ") : " + content);
                            delivered = true;
                            break;
                        }
                    }

                    if (!delivered) {
                        messageDAO.createNotification(targetId, messageId);
                    }

                    send("[" + targetUser + "] Moi (" + now() + ") : " + content);

                } catch (Exception e) {
                    e.printStackTrace();
                    send("[Erreur] Impossible d’envoyer le message.");
                }

            } else if (msg.equals("/repertoire")) {
                try {
                    List<String> history = messageDAO.getAllReceivedMessages(userId);
                    for (String line : history) {
                        send("[Répertoire] " + line);
                    }
                } catch (SQLException e) {
                    send("[Erreur] Chargement du répertoire échoué.");
                }
            } else {
                // Message public (à tous)
                int messageId = -1;
                try {
                    messageId = messageDAO.saveMessage(userId, null, null, msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ChatServer.broadcast("[" + username + "] (" + now() + ") : " + msg, this);
                send("[Public] Moi (" + now() + ") : " + msg);
            }
        }
    }

    private String now() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public void send(String msg) {
        out.println(msg);
    }
}
