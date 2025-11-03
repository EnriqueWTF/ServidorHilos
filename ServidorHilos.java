import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;

public class ServidorHilos {
    static HashMap<String, DosClientes> clientes = new HashMap<>();
    static HashMap<String, Gatito> juegosActivos = new HashMap<>();
    static HashMap<String, String> invitaciones = new HashMap<>();

    public static synchronized String getKeyJuego(String id1, String id2) {
        if (id1.compareTo(id2) < 0) {
            return id1 + ":" + id2;
        } else {
            return id2 + ":" + id1;
        }
    }

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(8080)) {
            System.out.println("Servidor iniciado en puerto 8080...");

            while (true) {
                Socket socket = servidor.accept();



                new Thread(new ManejadorLogin(socket)).start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}