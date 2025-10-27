import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class Gatito {

    // --- Estado del Juego ---
    private final char[][] tablero;
    private final String claveJuego; // Clave única (ej: "0:1")

    // --- Jugadores ---
    public final String idJugadorX;
    public final String idJugadorO;
    private final DosClientes handlerX;
    private final DosClientes handlerO;

    // --- Turno y Control ---
    private String idTurnoActual;
    private boolean juegoActivo;
    private int movimientos;

    /**
     * Constructor del juego.
     * Determina aleatoriamente quién es X y quién es O.
     */
    public Gatito(DosClientes retador, DosClientes retado, String claveJuego) {
        this.claveJuego = claveJuego;
        this.tablero = new char[3][3];
        this.movimientos = 0;
        this.juegoActivo = true;

        // Asignación aleatoria de X y O
        if (new Random().nextBoolean()) {
            this.idJugadorX = retador.idCliente;
            this.handlerX = retador;
            this.idJugadorO = retado.idCliente;
            this.handlerO = retado;
        } else {
            this.idJugadorX = retado.idCliente;
            this.handlerX = retado;
            this.idJugadorO = retador.idCliente;
            this.handlerO = retador;
        }

        // X siempre empieza
        this.idTurnoActual = this.idJugadorX;

        // Inicializa el tablero con números del 1 al 9
        char c = '1';
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tablero[i][j] = c++;
            }
        }
    }

    /**
     * Envía el estado inicial del juego a ambos jugadores.
     */
    public void iniciarJuego() {
        String msgX = "--- ¡Juego de Gato Iniciado! --- \n" +
                      "Tú juegas contra " + idJugadorO + ". Eres 'X'.\n" +
                      "Es tu turno. " + getInstrucciones();
        
        String msgO = "--- ¡Juego de Gato Iniciado! --- \n" +
                      "Tú juegas contra " + idJugadorX + ". Eres 'O'.\n" +
                      "Espera el turno de 'X'. " + getInstrucciones();

        enviarMensajeAmbos(dibujarTablero());
        enviarMensajeJuego(handlerX, msgX);
        enviarMensajeJuego(handlerO, msgO);
    }

    private String getInstrucciones() {
        return "Usa: /mover <oponente> <numero_casilla>";
    }

    /**
     * Genera un String del tablero actual.
     */
    private String dibujarTablero() {
        StringBuilder sb = new StringBuilder("\nTablero (vs " + (idTurnoActual.equals(idJugadorX) ? idJugadorO : idJugadorX) + "):\n");
        sb.append(" " + tablero[0][0] + " | " + tablero[0][1] + " | " + tablero[0][2] + " \n");
        sb.append("---+---+---\n");
        sb.append(" " + tablero[1][0] + " | " + tablero[1][1] + " | " + tablero[1][2] + " \n");
        sb.append("---+---+---\n");
        sb.append(" " + tablero[2][0] + " | " + tablero[2][1] + " | " + tablero[2][2] + " \n");
        return sb.toString();
    }

    /**
     * Procesa el movimiento de un jugador.
     */
    public synchronized void realizarMovimiento(String idJugador, int posicion) {
        if (!juegoActivo) {
            enviarMensajeJuego(getHandler(idJugador), "El juego ya ha terminado.");
            return;
        }

        if (!idJugador.equals(idTurnoActual)) {
            enviarMensajeJuego(getHandler(idJugador), "No es tu turno.");
            return;
        }

        if (posicion < 1 || posicion > 9) {
            enviarMensajeJuego(getHandler(idJugador), "Posición inválida (debe ser 1-9).");
            return;
        }

        // Convertir posición 1-9 a coordenadas [fila][col]
        int fila = (posicion - 1) / 3;
        int col = (posicion - 1) % 3;

        if (tablero[fila][col] == 'X' || tablero[fila][col] == 'O') {
            enviarMensajeJuego(getHandler(idJugador), "Esa casilla ya está ocupada.");
            return;
        }

        // Realizar movimiento
        char marca = (idJugador.equals(idJugadorX)) ? 'X' : 'O';
        tablero[fila][col] = marca;
        movimientos++;

        // Dibujar el tablero actualizado para ambos
        enviarMensajeAmbos(dibujarTablero());

        // Verificar estado del juego
        if (verificarGanador(marca)) {
            terminarJuego("¡El jugador " + idJugador + " (" + marca + ") ha ganado!");
        } else if (movimientos == 9) {
            terminarJuego("¡Es un empate!");
        } else {
            // Cambiar turno
            idTurnoActual = (idTurnoActual.equals(idJugadorX)) ? idJugadorO : idJugadorX;
            enviarMensajeAmbos("Es el turno de " + idTurnoActual + " (" + (idTurnoActual.equals(idJugadorX) ? 'X' : 'O') + ").");
        }
    }

    /**
     * Verifica si la última marca (X o O) ganó el juego.
     */
    private boolean verificarGanador(char marca) {
        // Filas y Columnas
        for (int i = 0; i < 3; i++) {
            if (tablero[i][0] == marca && tablero[i][1] == marca && tablero[i][2] == marca) return true;
            if (tablero[0][i] == marca && tablero[1][i] == marca && tablero[2][i] == marca) return true;
        }
        // Diagonales
        if (tablero[0][0] == marca && tablero[1][1] == marca && tablero[2][2] == marca) return true;
        if (tablero[0][2] == marca && tablero[1][1] == marca && tablero[2][0] == marca) return true;
        
        return false;
    }

    /**
     * Finaliza el juego, notifica y lo elimina del servidor.
     */
    private void terminarJuego(String mensaje) {
        enviarMensajeAmbos("--- Fin del Juego ---");
        enviarMensajeAmbos(mensaje);
        this.juegoActivo = false;
        // Se auto-elimina de la lista de juegos activos
        ServidorHilos.juegosActivos.remove(this.claveJuego);
    }

    /**
     * Maneja cuando un jugador abandona (por /salirgato o desconexión).
     */
    public synchronized void forfeit(String idPerdedor) {
        if (!juegoActivo) return; // El juego ya había terminado

        this.juegoActivo = false;
        String idGanador = idPerdedor.equals(idJugadorX) ? idJugadorO : idJugadorX;
        DosClientes handlerGanador = (idPerdedor.equals(idJugadorX)) ? handlerO : handlerX;

        String mensaje = "Tu oponente (" + idPerdedor + ") ha abandonado la partida. ¡Has ganado!";
        
        // No necesitamos notificar al perdedor (ya se fue o usó /salirgato)
        enviarMensajeJuego(handlerGanador, mensaje);
        
        // Quien llama a forfeit (DosClientes) se encarga de remover el juego del HashMap.
    }

  
    
    private DosClientes getHandler(String id) {
        return id.equals(idJugadorX) ? handlerX : handlerO;
    }

    private void enviarMensajeAmbos(String mensaje) {
        enviarMensajeJuego(handlerX, mensaje);
        enviarMensajeJuego(handlerO, mensaje);
    }

    private void enviarMensajeJuego(DosClientes handler, String mensaje) {
        try {
            if (handler != null && handler.salida != null) {
                handler.salida.writeUTF("[GATO] " + mensaje);
            }
        } catch (IOException e) {
          
            System.err.println("Error al enviar mensaje de juego: " + e.getMessage());
        }
    }
}