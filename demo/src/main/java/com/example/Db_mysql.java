package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe d'accès à une base MySql
 * 
 * @author tanda
 * @version 4 
 */

public class Db_mysql {
  /**
   * Driver à utiliser
   */
  private String driverName = "com.mysql.cj.jdbc.Driver"; // "com.mysql.jdbc.Driver" est obsolète;
  /**
   * Driver mysql
   */
  private String jdbc = "jdbc:mysql:";
  /**
   * Chemin vers le fichier sqlite
   */
  private String dbName;
  /**
   * Chaine complète de désignation de la base de données
   */
  private String dbUrl;
  /**
   * Objet base de données
   */
  private Connection conn;
  /**
   * Objet de dialogue avec la base de données
   */
  private Statement stmt;

  /**
   * Construit l'objet avec une base existante ou un base vide
   * 
   * @param p_db_name Chemin vers le fichier mysql
   * @throws Exception Exception levée si il y a une erreur (base non trouvée par
   *                   exemple)
   */
  public Db_mysql(String p_db_name, String login, String password) throws Exception {
    try {
      createBase(p_db_name + "?useTimezone=true&serverTimezone=UTC", login, password);
    } catch (Exception e) {
      System.out.println("Erreur d'accès à la base de données");
    }
  }

  /**
   * Construit l'objet avec l'exécution d'un script de création des tables
   * 
   * @param p_db_name        Chemin vers le fichier sqlite
   * @param p_sqlFile_create Chemin vers le script SQL de création des tables
   * @throws Exception Exception levée si il y a une erreur (base non trouvée,
   *                   erreur SQL)
   */
  public Db_mysql(String p_db_name, String login, String password, String p_sqlFile_create) throws Exception {
    try {
      createBase(p_db_name + "?useTimezone=true&serverTimezone=UTC", login, password);
      try {
        sqlExecFile(p_sqlFile_create);
      } catch (Exception e) {
        System.out.println("Erreur de création des tables");
      }
    } catch (Exception e) {
      System.out.println("Erreur d'accès à la base de données");
    }
  }

  /**
   * Construit l'objet avec l'exécution d'un script de création des tables
   * et l'exécution d'un script d'initialisation
   * 
   * @param p_db_name        Chemin vers le fichier sqlite
   * @param p_sqlFile_create Chemin vers le script SQL de création des tables
   * @param p_sqlFile_init   Chemin vers le script SQL d'initialisation des tables
   * @throws Exception Exception levée si il y a une erreur (base non trouvée,
   *                   erreur SQL)
   */
  public Db_mysql(String p_db_name, String login, String password, String p_sqlFile_create, String p_sqlFile_init)
      throws Exception {
    try {
      createBase(p_db_name + "?useTimezone=true&serverTimezone=UTC", login, password);
      try {
        sqlExecFile(p_sqlFile_create);
        try {
          sqlExecFile(p_sqlFile_init);
        } catch (Exception e) {
          System.out.println("Erreur d'initialisation des tables");
        }
      } catch (Exception e) {
        System.out.println("Erreur de création des tables");
      }
    } catch (Exception e) {
      System.out.println("Erreur d'accès à la base de données");
    }
  }

  /**
   * Création de la connexion
   * 
   * @param p_db_name Chemin vers la base MySql
   */
  private void createBase(String p_db_name, String login, String password) throws Exception {
    Class.forName(driverName);
    dbName = p_db_name;
    dbUrl = jdbc + dbName;
    System.out.println(dbUrl);
    conn = DriverManager.getConnection(dbUrl, login, password);
  }

  /**
   * Exécution d'un script SQL
   * 
   * @param p_SQLfilename Chemin vers le fichier SQL
   * @throws Exception Exception levée si il y a une erreur
   */
  private void sqlExecFile(String p_SQLfilename) throws Exception {
    String s = new String();
    StringBuffer sb = new StringBuffer();
    // Lecture du fichier SQL dans une variable StringBuffer
    FileReader fr = new FileReader(p_SQLfilename);
    // be sure to not have line starting with "--" or "/*" or any other non
    // alphabetical character
    BufferedReader br = new BufferedReader(fr);
    while ((s = br.readLine()) != null) {
      sb.append(s);
    }
    br.close();
    // Decomposition du StringBuffer en tableau de chaines
    // here is our splitter ! We use ";" as a delimiter for each request
    // then we are sure to have well formed statements
    String[] inst = sb.toString().split(";");
    for (int i = 0; i < inst.length; i++) {
      // we ensure that there is no spaces before or after the request string
      // in order to not execute empty statements
      if (!inst[i].trim().equals("")) {
        stmt.executeUpdate(inst[i]);
      }
    }
  }

  /**
   * Exécute une requete SELECT et retourne un jeu d'enregistrements
   * 
   * @param p_requete Requête SQL à exécuter
   * @return Jeu d'enregistrements retourné
   * @throws SQLException
   */
  public ResultSet sqlSelect(String p_requete) throws SQLException {
    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_UPDATABLE);
    return stmt.executeQuery(p_requete);
  }

  /**
   * Exécute une requete SQL qui ne retourne rien (INSERT, UPDATE...)
   * 
   * @param p_requete Requête SQL à exécuter
   * @throws SQLException
   */
  public void sqlExec(String p_requete) throws SQLException {
    stmt = conn.createStatement();
    stmt.executeUpdate(p_requete);
  }

  /**
   * Récupère le dernier id autoincrement utilisé dans la table indiquée
   * 
   * @param p_table Nom de la table pour laquelle on veut connaître le dernier
   *                identifiant
   * @return retourne la valeur de l'identifiant
   * @throws SQLException
   */
  public ResultSet sqlLastId() throws SQLException {
    return sqlSelect("select last_insert_id() as id");
  }

}
