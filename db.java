import java.sql.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class db {
    private static final String URL = "jdbc:mysql://localhost:3306/chatdb";
    private static final String USER = "root";
    private static final String PASS = "Kikinwtf123$";

    private static Connection conn = null;

    // Obtener conexión
    public static synchronized Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(URL, USER, PASS);
        }
        return conn;
    }


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
        if (blocker.equals(blocked)) return " No puedes bloquearte a ti mismo.";
        try {
            if (!userExists(blocked)) return "El usuario " + blocked + " no existe.";
            String sql = "INSERT IGNORE INTO bloqueos(blocker, blocked) VALUES(?, ?)";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, blocker);
                ps.setString(2, blocked);
                int affected = ps.executeUpdate();
                return (affected == 0)
                        ? "El usuario " + blocked + " ya estaba bloqueado."
                        : "Has bloqueado al usuario " + blocked + ".";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error al bloquear en la base de datos.";
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
                        return "El usuario " + blocked + " no estaba bloqueado.";
                    }
                }
            }

            String sql = "DELETE FROM bloqueos WHERE blocker = ? AND blocked = ?";
            try (PreparedStatement ps2 = getConnection().prepareStatement(sql)) {
                ps2.setString(1, blocker);
                ps2.setString(2, blocked);
                ps2.executeUpdate();
                return "Has desbloqueado al usuario " + blocked + ".";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error al desbloquear en la base de datos.";
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


    // --- MÉTODOS DE RANKING ---

    public static synchronized void registrarResultado(String j1, String j2, String resultado) {
        // 1. Guardar la partida
        String sqlInsert = "INSERT INTO partidas (id_jugador1, id_jugador2, resultado) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sqlInsert)) {
            ps.setString(1, j1);
            ps.setString(2, j2);
            ps.setString(3, resultado);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error al registrar la partida.");
            return;
        }


        // 2. Actualizar puntos
        try {
            if ("EMPATE".equals(resultado)) {
                String sqlEmpate = "UPDATE usuarios SET puntos = puntos + 1 WHERE id = ? OR id = ?";
                try (PreparedStatement psPuntos = getConnection().prepareStatement(sqlEmpate)) {
                    psPuntos.setString(1, j1);
                    psPuntos.setString(2, j2);
                    psPuntos.executeUpdate();
                }
            } else {
                String sqlVictoria = "UPDATE usuarios SET puntos = puntos + 2 WHERE id = ?";
                try (PreparedStatement psPuntos = getConnection().prepareStatement(sqlVictoria)) {
                    psPuntos.setString(1, resultado);
                    psPuntos.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error al actualizar puntos.");
        }
    }


    public static synchronized String getRankingGeneral() {
        String sql = "SELECT id, puntos FROM usuarios ORDER BY puntos DESC LIMIT 10";
        StringBuilder ranking = new StringBuilder("---  Ranking General (Top 10)  ---\n");
        int pos = 1;

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.isBeforeFirst()) {
                return "Aún no hay nadie en el ranking. ¡Jueguen una partida!";
            }

            while (rs.next()) {
                ranking.append(pos)
                        .append(". Usuario: ")
                        .append(rs.getString("id"))
                        .append(" - Puntos: ")
                        .append(rs.getInt("puntos"))
                        .append("\n");
                pos++;
            }
            return ranking.toString();

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error al consultar el ranking.";
        }
    }


    public static synchronized String getStatsH2H(String jugador1, String jugador2) {
        int victoriasJugador1 = 0;
        int victoriasJugador2 = 0;
        int empates = 0;

        try {
            String sqlJ1 = "SELECT COUNT(*) FROM partidas " +
                    "WHERE ((id_jugador1 = ? AND id_jugador2 = ?) OR (id_jugador1 = ? AND id_jugador2 = ?)) " +
                    "AND resultado = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sqlJ1)) {
                ps.setString(1, jugador1);
                ps.setString(2, jugador2);
                ps.setString(3, jugador2);
                ps.setString(4, jugador1);
                ps.setString(5, jugador1);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        victoriasJugador1 = rs.getInt(1);
                    }
                }
            }

            String sqlJ2 = "SELECT COUNT(*) FROM partidas " +
                    "WHERE ((id_jugador1 = ? AND id_jugador2 = ?) OR (id_jugador1 = ? AND id_jugador2 = ?)) " +
                    "AND resultado = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sqlJ2)) {
                ps.setString(1, jugador1);
                ps.setString(2, jugador2);
                ps.setString(3, jugador2);
                ps.setString(4, jugador1);
                ps.setString(5, jugador2);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        victoriasJugador2 = rs.getInt(1);
                    }
                }
            }

            String sqlEmpate = "SELECT COUNT(*) FROM partidas " +
                    "WHERE ((id_jugador1 = ? AND id_jugador2 = ?) OR (id_jugador1 = ? AND id_jugador2 = ?)) " +
                    "AND resultado = 'EMPATE'";

            try (PreparedStatement ps = getConnection().prepareStatement(sqlEmpate)) {
                ps.setString(1, jugador1);
                ps.setString(2, jugador2);
                ps.setString(3, jugador2);
                ps.setString(4, jugador1);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        empates = rs.getInt(1);
                    }
                }
            }

            int totalPartidas = victoriasJugador1 + victoriasJugador2 + empates;
            if (totalPartidas == 0) {
                return "Los usuarios " + jugador1 + " y " + jugador2 + " nunca han jugado.";
            }

            double porc_j1 = (victoriasJugador1 * 100.0) / totalPartidas;
            double porc_j2 = (victoriasJugador2 * 100.0) / totalPartidas;
            double porc_emp = (empates * 100.0) / totalPartidas;

            return String.format(
                    "---  Estadísticas: %s vs %s ---\n" +
                            "Partidas Totales: %d\n" +
                            "Victorias %s: %d (%.1f%%)\n" +
                            "Victorias %s: %d (%.1f%%)\n" +
                            "Empates: %d (%.1f%%)\n",
                    jugador1, jugador2, totalPartidas,
                    jugador1, victoriasJugador1, porc_j1,
                    jugador2, victoriasJugador2, porc_j2,
                    empates, porc_emp
            );

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error al consultar estadísticas.";
        }
    }


    // MÉTODOS DE LOGIN/REGISTRO

    public static synchronized String registerUser(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            return "ERROR: El usuario y la contraseña no pueden estar vacíos.";
        }

        try {
            if (userExists(username)) {
                return "ERROR: El nombre de usuario '" + username + "' ya existe.";
            }

            String sql = "INSERT INTO usuarios(id, password, puntos) VALUES(?, ?, 0)";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.executeUpdate();
                return "OK: Usuario '" + username + "' registrado exitosamente.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR: Ocurrió un error en la base de datos.";
        }
    }

    public static synchronized boolean loginUser(String username, String password) {
        try {
            String sql = "SELECT 1 FROM usuarios WHERE id = ? AND password = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //  GRUPOS

    public static synchronized String crearGrupo(String nombreGrupo, String idCreador) {
        try {
            // 1. crear el grupo
            String sqlGrupo = "INSERT INTO grupos (nombre, id_creador) VALUES (?, ?)";
            try (PreparedStatement ps = getConnection().prepareStatement(sqlGrupo)) {
                ps.setString(1, nombreGrupo);
                ps.setString(2, idCreador);
                ps.executeUpdate();
            } catch (SQLException e) {
                return "ERROR: El grupo '" + nombreGrupo + "' ya existe o el nombre es inválido.";
            }

            // 2. Añadir al creador como miembro
            String sqlMiembro = "INSERT INTO miembros_grupo (nombre_grupo, id_usuario) VALUES (?, ?)";
            try (PreparedStatement ps = getConnection().prepareStatement(sqlMiembro)) {
                ps.setString(1, nombreGrupo);
                ps.setString(2, idCreador);
                ps.executeUpdate();
            }

            return "OK: Grupo '" + nombreGrupo + "' creado. Tú eres el primer miembro.";

        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR: Error de base de datos al crear grupo.";
        }
    }

    public static synchronized boolean esMiembro(String idUsuario, String nombreGrupo) {
        String sql = "SELECT 1 FROM miembros_grupo WHERE nombre_grupo = ? AND id_usuario = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, nombreGrupo);
            ps.setString(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized String invitarAGrupo(String idInvitador, String idInvitado, String nombreGrupo) {
        try {
            // 1. Validaciones
            if (!userExists(idInvitado)) {
                return "ERROR: El usuario '" + idInvitado + "' no existe.";
            }
            if (!esMiembro(idInvitador, nombreGrupo)) {
                return "ERROR: No puedes invitar. No eres miembro del grupo '" + nombreGrupo + "'.";
            }
            if (esMiembro(idInvitado, nombreGrupo)) {
                return "ERROR: El usuario '" + idInvitado + "' ya es miembro de este grupo.";
            }

            // 2. Añadir al miembro
            String sql = "INSERT INTO miembros_grupo (nombre_grupo, id_usuario) VALUES (?, ?)";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, nombreGrupo);
                ps.setString(2, idInvitado);
                ps.executeUpdate();
                return "OK: Has añadido a '" + idInvitado + "' al grupo '" + nombreGrupo + "'.";
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("FOREIGN KEY")) {
                return "ERROR: El grupo '" + nombreGrupo + "' no existe.";
            }
            e.printStackTrace();
            return "ERROR: Error de base de datos al invitar.";
        }
    }

    public static synchronized List<String> getMiembrosGrupo(String nombreGrupo) {
        List<String> miembros = new ArrayList<>();
        String sql = "SELECT id_usuario FROM miembros_grupo WHERE nombre_grupo = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, nombreGrupo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    miembros.add(rs.getString("id_usuario"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return miembros;
    }
}