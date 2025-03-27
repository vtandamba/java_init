package com.example.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    private final Connection connection;

    public MessageDAO(Connection connection) {
        this.connection = connection;
    }

    public int getUserIdByName(String username) throws SQLException {
        String sql = "SELECT id FROM utilisateurs WHERE nom = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
    }

    public List<String> getUserGroups(int userId) throws SQLException {
        List<String> groupes = new ArrayList<>();
        String sql = "SELECT g.nom FROM groupes g " +
                "JOIN utilisateurs_groupes ug ON g.id = ug.groupe_id " +
                "WHERE ug.utilisateur_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                groupes.add(rs.getString("nom"));
            }
        }
        return groupes;
    }

    public int saveMessage(int expediteurId, Integer destinataireId, Integer groupeId, String contenu)
            throws SQLException {
        String sql = "INSERT INTO messages (expediteur_id, destinataire_id, groupe_id, contenu, timestamp) " +
                "VALUES (?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, expediteurId);
            if (destinataireId != null) {
                stmt.setInt(2, destinataireId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            if (groupeId != null) {
                stmt.setInt(3, groupeId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, contenu);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1); // return inserted message ID
            }
        }
        return -1;
    }

    public void createNotification(int userId, int messageId) throws SQLException {
        String sql = "INSERT INTO notifications (utilisateur_id, message_id, lu, created_at) VALUES (?, ?, 0, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, messageId);
            stmt.executeUpdate();
        }
    }

    public int getUnreadNotificationCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE utilisateur_id = ? AND lu = 0";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<String> getAllReceivedMessages(int userId) throws SQLException {
        List<String> messages = new ArrayList<>();
    
        String sql = "SELECT u.nom AS expediteur, m.contenu, m.timestamp " +
                     "FROM messages m JOIN utilisateurs u ON m.expediteur_id = u.id " +
                     "WHERE m.destinataire_id = ? OR m.groupe_id IN (" +
                     "SELECT groupe_id FROM utilisateurs_groupes WHERE utilisateur_id = ?) " +
                     "ORDER BY m.timestamp ASC";
    
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
    
            while (rs.next()) {
                String exp = rs.getString("expediteur");
                String contenu = rs.getString("contenu");
                Timestamp time = rs.getTimestamp("timestamp");
                messages.add("[De " + exp + " à " + time.toLocalDateTime().toLocalTime() + "] : " + contenu);
            }
        }
    
        return messages;
    }
    
}
