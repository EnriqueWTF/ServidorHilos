<<<<<<< HEAD
=======
import java.io.*;
import java.net.Socket;

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
                        manejarMovimiento(partes[1], partes[2]);
                    }
                
                } else if (mensaje.startsWith("/salirgato ")) {
                    String oponente = mensaje.substring(11).trim();
                    manejarForfeit(oponente);
                
         

                } else if (mensaje.startsWith("@")) {
                    enviarDirecto(mensaje);

                } else {
                    enviarBroadcast(mensaje);
                }

            } catch (IOException e) {
                System.out.println("Cliente " + idCliente + " desconectado.");
           
                
                // Remover cliente de la lista de conectados
                ServidorHilos.clientes.remove(idCliente);

                // Iterar sobre TODOS los juegos activos y forzar forfeit si este cliente participaba
                // Usamos un iterador para poder eliminar de forma segura mientras iteramos
                Iterator<Map.Entry<String, Gatito>> iter = ServidorHilos.juegosActivos.entrySet().iterator();
                
                while (iter.hasNext()) {
                    Map.Entry<String, Gatito> entry = iter.next();
                    Gatito juego = entry.getValue();

                    // Si el cliente desconectado era X o O en este juego
                    if (juego.idJugadorX.equals(idCliente) || juego.idJugadorO.equals(idCliente)) {
                        // Notificar al oponente que ganó
                        juego.forfeit(idCliente); 
                        // Remover el juego de la lista activa
                        iter.remove(); 
                        System.out.println("Juego " + entry.getKey() + " terminado por desconexión.");
                    }
                }
                
            
                break; // Salir del bucle run()
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

        // Requisito: No más de un juego con alguien en particular
        String claveJuego = ServidorHilos.getKeyJuego(idCliente, idOponente);
        if (ServidorHilos.juegosActivos.containsKey(claveJuego)) {
            salida.writeUTF("Ya estás en una partida activa con " + idOponente + ".");
            return;
        }

        // Verificar si el oponente ya tiene otra invitación
        if (ServidorHilos.invitaciones.containsKey(idOponente)) {
            salida.writeUTF("El usuario " + idOponente + " ya tiene una invitación pendiente. Intenta más tarde.");
            return;
        }
        
        // Registrar la invitación
        ServidorHilos.invitaciones.put(idOponente, idCliente);
        
        // Notificar a ambos
        salida.writeUTF("Invitación enviada a " + idOponente + ".");
        oponente.salida.writeUTF("¡" + idCliente + " te ha invitado a jugar Gato!");
        oponente.salida.writeUTF("Escribe '/aceptar' o '/rechazar'.");
    }

    /**
     * Acepta una invitación pendiente.
     */
    private void manejarAceptacion() throws IOException {
        // Verifica si este cliente (idCliente) tiene una invitación
        // Remueve la invitación para que no pueda ser aceptada de nuevo
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

        // Crear la clave del juego
        String claveJuego = ServidorHilos.getKeyJuego(idCliente, idRetador);

        // Doble chequeo (Requisito: no mas de uno)
        if (ServidorHilos.juegosActivos.containsKey(claveJuego)) {
            salida.writeUTF("Error: Ya existe un juego activo con " + idRetador + ".");
            retador.salida.writeUTF("Error: " + idCliente + " intentó aceptar, pero ya hay un juego activo.");
            return;
        }

        // Crear e iniciar el juego
        Gatito nuevoJuego = new Gatito(retador, this, claveJuego);
        ServidorHilos.juegosActivos.put(claveJuego, nuevoJuego);
        
        nuevoJuego.iniciarJuego(); // El juego notificará a ambos jugadores
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
    private void manejarMovimiento(String idOponente, String posStr) throws IOException {
        String claveJuego = ServidorHilos.getKeyJuego(idCliente, idOponente);
        Gatito juego = ServidorHilos.juegosActivos.get(claveJuego);

        if (juego == null) {
            salida.writeUTF("No estás jugando una partida activa con " + idOponente + ".");
            return;
        }

        try {
            int posicion = Integer.parseInt(posStr);
            juego.realizarMovimiento(idCliente, posicion);
        } catch (NumberFormatException e) {
            salida.writeUTF("Movimiento inválido. La posición debe ser un número (ej: /mover " + idOponente + " 5)");
        }
    }
    
    /**
     * Abandona (forfeit) una partida en curso.
     */
    private void manejarForfeit(String idOponente) throws IOException {
        String claveJuego = ServidorHilos.getKeyJuego(idCliente, idOponente);
        // IMPORTANTE: remove() devuelve el juego y lo elimina del map
        Gatito juego = ServidorHilos.juegosActivos.remove(claveJuego);

        if (juego == null) {
            salida.writeUTF("No estabas jugando una partida activa con " + idOponente + ".");
            return;
        }
        
        salida.writeUTF("Has abandonado la partida contra " + idOponente + ".");
        // Notificar al oponente que ganó
        juego.forfeit(idCliente);
    }



    // (Los métodos enviarDirecto y enviarBroadcast permanecen sin cambios)
    private void enviarDirecto(String mensaje) throws IOException {

    }

    private void enviarBroadcast(String mensaje) throws IOException {

    }
}
>>>>>>> dev2
