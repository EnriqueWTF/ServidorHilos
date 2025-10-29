import java.sql.*;

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
            // Si esto falla, no continuamos actualizando puntos.
            return;
        }

        // Actualizar puntos

        try {
            if ("EMPATE".equals(resultado)) {
                // Empate: 1 punto para cada uno
                String sqlEmpate = "UPDATE usuarios SET puntos = puntos + 1 WHERE id = ? OR id = ?";
                try (PreparedStatement psPuntos = getConnection().prepareStatement(sqlEmpate)) {
                    psPuntos.setString(1, j1);
                    psPuntos.setString(2, j2);
                    psPuntos.executeUpdate();
                }
            } else {
                // Victoria: 2 puntos para el ganador (el 'resultado' es el ID del ganador)
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

            if (!rs.isBeforeFirst()) { // Verifica si el ResultSet está vacío
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

            // Buscamos partidas donde los jugadores sean (j1, j2) O (j2, j1)
            // Y donde el resultado sea el ID del jugador 1.
            String sqlJ1 = "SELECT COUNT(*) FROM partidas " +
                    "WHERE ((id_jugador1 = ? AND id_jugador2 = ?) OR (id_jugador1 = ? AND id_jugador2 = ?)) " +
                    "AND resultado = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sqlJ1)) {
                ps.setString(1, jugador1);
                ps.setString(2, jugador2);
                ps.setString(3, jugador2);
                ps.setString(4, jugador1);
                ps.setString(5, jugador1); // El resultado que buscamos es el ID del jugador 1

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        victoriasJugador1 = rs.getInt(1); // Obtenemos el conteo
                    }
                }
            }


            // Es la misma consulta, pero ahora buscamos el ID del jugador 2 en el resultado.
            String sqlJ2 = "SELECT COUNT(*) FROM partidas " +
                    "WHERE ((id_jugador1 = ? AND id_jugador2 = ?) OR (id_jugador1 = ? AND id_jugador2 = ?)) " +
                    "AND resultado = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sqlJ2)) {
                ps.setString(1, jugador1);
                ps.setString(2, jugador2);
                ps.setString(3, jugador2);
                ps.setString(4, jugador1);
                ps.setString(5, jugador2); // El resultado que buscamos es el ID del jugador 2

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        victoriasJugador2 = rs.getInt(1);
                    }
                }
            }


            // Misma consulta, pero buscamos la palabra "EMPATE".
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

            // Calculamos los porcentajes
            double porc_j1 = (victoriasJugador1 * 100.0) / totalPartidas;
            double porc_j2 = (victoriasJugador2 * 100.0) / totalPartidas;
            double porc_emp = (empates * 100.0) / totalPartidas;

            // Devolvemos el texto formateado
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


}