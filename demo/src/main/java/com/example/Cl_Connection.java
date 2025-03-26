// package com.example;
// /**
//  *
//  * @author tanda
//  */
// public class Cl_Connection {
//     public static String url = "//localhost";
//     public static String login ="tandambav";
//     public static String password ="MeeJeediexo3";
// }

package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Cl_Connection {
    private static final String URL = "jdbc:mysql://localhost:3306/chat_app";
    private static final String USER = "root"; // Change si nécessaire
    private static final String PASSWORD = "MeeJeediexo3*";

    public static Connection getConnection() {
        Connection connection = null;
        try {
            // Charger le driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Établir la connexion
            return connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.out.println("Erreur : Driver JDBC non trouvé !");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Erreur : Impossible de se connecter à la base de données !");
            e.printStackTrace();
        }
        return connection;
    }
}
