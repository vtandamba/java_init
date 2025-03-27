package com.example.client;

import java.io.*;
import java.net.Socket;


public class ChatClient {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static boolean connect(String username, String password) {
        try {
            socket = new Socket("localhost", 1000); // Assure-toi que le port est correct
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Serveur : " + in.readLine()); // Bienvenue !
            out.println("1"); // choix connexion

            System.out.println("Serveur : " + in.readLine()); // Nom d'utilisateur:
            out.println(username);

            System.out.println("Serveur : " + in.readLine()); // Mot de passe:
            out.println(password);

            String response = in.readLine();
            System.out.println("Réponse serveur connexion : " + response);

            return response != null && response.toLowerCase().contains("réussie");

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean register(String username, String password, String email) {
        try {
            socket = new Socket("localhost", 1000); // même port que le serveur
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Serveur : " + in.readLine()); // Bienvenue ! Connexion (1) ou Inscription (2)
            out.println("2"); // Choix inscription

            System.out.println("Serveur : " + in.readLine()); // Nom d'utilisateur:
            out.println(username);

            System.out.println("Serveur : " + in.readLine()); // Mot de passe:
            out.println(password);

            System.out.println("Serveur : " + in.readLine()); // Email:
            out.println(email);

            String response = in.readLine();
            System.out.println("Réponse serveur inscription : " + response);

            return response != null && response.toLowerCase().contains("réussie");

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void send(String msg) {
        if (out != null) out.println(msg);
    }

    public static String readMessage() throws IOException {
        return in.readLine();
    }
    private static MessageReceiver receiver;

public static void setReceiver(MessageReceiver r) {
    receiver = r;
}

public static void startListening() {
    new Thread(() -> {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (receiver != null) {
                    receiver.onMessageReceived(line);
                }
            }
        } catch (IOException e) {
            if (receiver != null) {
                receiver.onMessageReceived("[Erreur] Déconnecté du serveur.");
            }
        }
    }).start();
}

public interface MessageReceiver {
    void onMessageReceived(String message);
}

}