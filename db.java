

import java.sql.*;

public class db {
    private static final String URL = "jdbc:mysql://localhost:3306/chatdb";
    private static final String USER = "root";
    private static final String PASS = "tu_contraseña_aquí"; // 🔹 cambia esto por tu contraseña

    private static Connection conn = null;

    // Obtener conexión
    public static synchronized Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(URL, USER, PASS);
        }
        return conn;
    }

    // Añadir usuario si no existe
    public static synchronized void addUser(String id) throws SQLException {
        String sql = "INSERT IGNORE INTO usuarios(id) VALUES(?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    // Verificar existencia
    public static synchronized boolean userExists(String id) throws SQLException {
        String sql = "SELECT 1 FROM usuarios WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Bloquear usuario
    public static synchronized String blockUser(String blocker, String blocked) {
        if (blocker.equals(blocked)) return "❌ No puedes bloquearte a ti mismo.";
        try {
            if (!userExists(blocked)) return "❌ El usuario " + blocked + " no existe.";
            String sql = "INSERT IGNORE INTO bloqueos(blocker, blocked) VALUES(?, ?)";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, blocker);
                ps.setString(2, blocked);
                int affected = ps.executeUpdate();
                return (affected == 0)
                        ? "⚠️ El usuario " + blocked + " ya estaba bloqueado."
                        : "✅ Has bloqueado al usuario " + blocked + ".";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "⚠️ Error al bloquear en la base de datos.";
        }
    }

    // Desbloquear usuario
    public static synchronized String unblockUser(String blocker, String blocked) {
        try {
            String sqlCheck = "SELECT 1 FROM bloqueos WHERE blocker = ? AND blocked = ?";
            try (PreparedStatement ps = getConnection().prepareStatement(sqlCheck)) {
                ps.setString(1, blocker);
                ps.setString(2, blocked);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return "⚠️ El usuario " + blocked + " no estaba bloqueado.";
                    }
                }
            }

            String sql = "DELETE FROM bloqueos WHERE blocker = ? AND blocked = ?";
            try (PreparedStatement ps2 = getConnection().prepareStatement(sql)) {
                ps2.setString(1, blocker);
                ps2.setString(2, blocked);
                ps2.executeUpdate();
                return "✅ Has desbloqueado al usuario " + blocked + ".";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "⚠️ Error al desbloquear en la base de datos.";
        }
    }

    // Verificar si alguien está bloqueado
    public static synchronized boolean isBlocked(String blocker, String blocked) {
        String sql = "SELECT 1 FROM bloqueos WHERE blocker = ? AND blocked = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, blocker);
            ps.setString(2, blocked);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
