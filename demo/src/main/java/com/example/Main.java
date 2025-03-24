package com.example;

import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        Connection conn = Cl_Connection.getConnection();
        if (conn != null) {
            System.out.println("Connexion réussie à MySQL !");
        } else {
            System.out.println("Échec de la connexion !");
        }
    }
}
