// --------------------- ClientHandler.java ---------------------
package com.example.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

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

                if (ChatServer.authenticateUser(user, pass)) {
                    username = user;
                    out.println("Connexion réussie !");
                    ChatServer.addClient(this);
                    readMessages();
                } else {
                    out.println("Connexion échouée.");
                    socket.close();
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
            ChatServer.broadcast(username + ": " + msg, this);
        }
    }

    public void send(String msg) {
        out.println(msg);
    }
}
