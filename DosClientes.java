import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

public class DosClientes implements Runnable {

    final DataOutputStream salida;
    final DataInputStream entrada;
    public final String idCliente;

    public DosClientes(Socket s, String idCliente) throws IOException {
        this.idCliente = idCliente;
        this.salida = new DataOutputStream(s.getOutputStream());
        this.entrada = new DataInputStream(s.getInputStream());

        salida.writeUTF("Conectado como usuario " + idCliente);

        salida.writeUTF("Comandos Chat: /block <id>, /unblock <id>, @<id> <mensaje>");
        salida.writeUTF("Comandos Gato: /gato <id>, /aceptar, /rechazar, /mover <oponente> <1-9>, /salirgato <oponente>");
        salida.writeUTF("Comandos Ranking: /ranking, /ranking <id1> <id2>");

    }


    @Override
    public void run() {
        String mensaje;
        while (true) {

            try {

                mensaje = entrada.readUTF().trim();


                if (mensaje.isEmpty()) continue;

                if (mensaje.startsWith("/block ")) {
                    String objetivo = mensaje.substring(7).trim();
                    salida.writeUTF(db.blockUser(idCliente, objetivo));

                } else if (mensaje.startsWith("/unblock ")) {
                    String objetivo = mensaje.substring(9).trim();
                    salida.writeUTF(db.unblockUser(idCliente, objetivo));

                } else if (mensaje.startsWith("/gato ")) {
                    String oponente = mensaje.substring(6).trim();
                    manejarInvitacion(oponente);

                } else if (mensaje.equals("/aceptar")) {
                    manejarAceptacion();

                } else if (mensaje.equals("/rechazar")) {
                    manejarRechazo();

                } else if (mensaje.startsWith("/mover ")) {
                    String[] partes = mensaje.split(" ", 3);
                    if (partes.length < 3) {
                        salida.writeUTF("Formato incorrecto. Usa: /mover <oponente> <numero>");
                    } else {
                        String idOponente = partes[1];
                        String posicionComoTexto = partes[2];
                        manejarMovimiento(idOponente, posicionComoTexto);
                    }

                } else if (mensaje.startsWith("/salirgato ")) {
                    String oponente = mensaje.substring(11).trim();
                    manejarAbandono(oponente);

                    // --- Ranking ---

                } else if (mensaje.equals("/ranking")) {
                    salida.writeUTF(db.getRankingGeneral());

                } else if (mensaje.startsWith("/ranking ")) {
                    String[] palabras = mensaje.split(" ");

                    if (palabras.length < 3) {
                        salida.writeUTF("Formato incorrecto. Usa: /ranking <id1> <id2>");
                    } else {
                        String jugador1 = palabras[1];
                        String jugador2 = palabras[2];

                        if (!db.userExists(jugador1) || !db.userExists(jugador2)) {
                            salida.writeUTF("Uno o ambos usuarios no existen.");
                        } else {
                            salida.writeUTF(db.getStatsH2H(jugador1, jugador2));
                        }
                    }

                    // --- Chat ---

                } else if (mensaje.startsWith("@")) {
                    enviarDirecto(mensaje);

                } else {
                    enviarBroadcast(mensaje);
                }

            } catch (IOException e) {
                System.out.println("Cliente " + idCliente + " desconectado.");

                ServidorHilos.clientes.remove(idCliente);

                Iterator<Map.Entry<String, Gatito>> iter = ServidorHilos.juegosActivos.entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<String, Gatito> entry = iter.next();
                    Gatito juego = entry.getValue();

                    if (juego.idJugadorX.equals(idCliente) || juego.idJugadorO.equals(idCliente)) {
                        juego.forfeit(idCliente);
                        iter.remove();
                        System.out.println("Juego " + entry.getKey() + " terminado por desconexión.");
                    }
                }

                break;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Invita a otro jugador a una partida de Gato.
     */
    private void manejarInvitacion(String idOponente) throws IOException {
        if (idOponente.equals(idCliente)) {
            salida.writeUTF("No puedes jugar contra ti mismo.");
            return;
        }

        DosClientes oponente = ServidorHilos.clientes.get(idOponente);
        if (oponente == null) {
            salida.writeUTF("El usuario " + idOponente + " no está conectado.");
            return;
        }

        String claveJuego = ServidorHilos.getKeyJuego(idCliente, idOponente);
        if (ServidorHilos.juegosActivos.containsKey(claveJuego)) {
            salida.writeUTF("Ya estás en una partida activa con " + idOponente + ".");
            return;
        }

        if (ServidorHilos.invitaciones.containsKey(idOponente)) {
            salida.writeUTF("El usuario " + idOponente + " ya tiene una invitación pendiente. Intenta más tarde.");
            return;
        }

        ServidorHilos.invitaciones.put(idOponente, idCliente);

        salida.writeUTF("Invitación enviada a " + idOponente + ".");
        oponente.salida.writeUTF("¡" + idCliente + " te ha invitado a jugar Gato!");
        oponente.salida.writeUTF("Escribe '/aceptar' o '/rechazar'.");
    }

    /**
     * Acepta una invitación pendiente.
     */
    private void manejarAceptacion() throws IOException {
        String idRetador = ServidorHilos.invitaciones.remove(idCliente);

        if (idRetador == null) {
            salida.writeUTF("No tienes invitaciones pendientes.");
            return;
        }

        DosClientes retador = ServidorHilos.clientes.get(idRetador);
        if (retador == null) {
            salida.writeUTF("El usuario " + idRetador + " que te invitó ya no está conectado.");
            return;
        }

        String claveJuego = ServidorHilos.getKeyJuego(idCliente, idRetador);

        if (ServidorHilos.juegosActivos.containsKey(claveJuego)) {
            salida.writeUTF("Error: Ya existe un juego activo con " + idRetador + ".");
            retador.salida.writeUTF("Error: " + idCliente + " intentó aceptar, pero ya hay un juego activo.");
            return;
        }

        Gatito nuevoJuego = new Gatito(retador, this, claveJuego);
        ServidorHilos.juegosActivos.put(claveJuego, nuevoJuego);

        nuevoJuego.iniciarJuego();
    }

    /**
     * Rechaza una invitación pendiente.
     */
    private void manejarRechazo() throws IOException {
        String idRetador = ServidorHilos.invitaciones.remove(idCliente);

        if (idRetador == null) {
            salida.writeUTF("No tienes invitaciones pendientes que rechazar.");
            return;
        }

        salida.writeUTF("Has rechazado la invitación de " + idRetador + ".");

        DosClientes retador = ServidorHilos.clientes.get(idRetador);
        if (retador != null) {
            retador.salida.writeUTF("Tu invitación a " + idCliente + " fue rechazada.");
        }
    }

    /**
     * Envía un movimiento al juego correspondiente.
     */
    private void manejarMovimiento(String idOponente, String posicionComoTexto) throws IOException {
        String claveJuego = ServidorHilos.getKeyJuego(idCliente, idOponente);
        Gatito juego = ServidorHilos.juegosActivos.get(claveJuego);

        if (juego == null) {
            salida.writeUTF("No estás jugando una partida activa con " + idOponente + ".");
            return;
        }

        try {
            int posicionNumero = Integer.parseInt(posicionComoTexto);
            juego.realizarMovimiento(idCliente, posicionNumero);
        } catch (NumberFormatException e) {
            salida.writeUTF("Movimiento inválido. La posición debe ser un número (ej: /mover " + idOponente + " 5)");
        }
    }


    private void manejarAbandono(String idOponente) throws IOException {
        String claveJuego = ServidorHilos.getKeyJuego(idCliente, idOponente);
        Gatito juego = ServidorHilos.juegosActivos.remove(claveJuego);

        if (juego == null) {
            salida.writeUTF("No estabas jugando una partida activa con " + idOponente + ".");
            return;
        }

        salida.writeUTF("Has abandonado la partida contra " + idOponente + ".");
        juego.forfeit(idCliente);

    }


    /**
     * Métodos de Chat
     */
    private void enviarDirecto(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ", 2);
        if (partes.length < 2) {
            salida.writeUTF("Formato incorrecto. Usa: @id mensaje");
            return;
        }

        String aQuien = partes[0].substring(1);
        String texto = partes[1];

        DosClientes receptor = ServidorHilos.clientes.get(aQuien);
        if (receptor == null) {
            salida.writeUTF("El usuario " + aQuien + " no está conectado.");
            return;
        }

        if (db.isBlocked(aQuien, idCliente)) {
            salida.writeUTF("No puedes enviar mensaje, te tiene bloqueado.");
            return;
        }

        if (db.isBlocked(idCliente, aQuien)) {
            salida.writeUTF("Debes desbloquear a " + aQuien + " para enviarle mensajes.");
            return;
        }

        receptor.salida.writeUTF("Mensaje privado de " + idCliente + ": " + texto);
        salida.writeUTF("Enviado a " + aQuien);
    }

    private void enviarBroadcast(String mensaje) throws IOException {
        for (DosClientes cliente : ServidorHilos.clientes.values()) {

            if (cliente.idCliente.equals(idCliente)) {
                continue;
            }

            if (db.isBlocked(cliente.idCliente, idCliente)) {
                continue;
            }

            if (db.isBlocked(idCliente, cliente.idCliente)) {
                continue;
            }

            cliente.salida.writeUTF("💬 " + idCliente + ": " + mensaje);
        }
        salida.writeUTF("Mensaje enviado a todos (excepto bloqueados).");
    }
}