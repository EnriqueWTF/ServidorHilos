import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;

import java.util.concurrent.ConcurrentHashMap;

public class ServidorHilos {
    
    static ConcurrentHashMap<String, DosClientes> clientes = new ConcurrentHashMap<>();
    

    static ConcurrentHashMap<Integer, ManejadorLogin> invitados = new ConcurrentHashMap<>();
 
    static ConcurrentHashMap<String, Gatito> juegosActivos = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, String> invitaciones = new ConcurrentHashMap<>();
  


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

             
                ManejadorLogin manejador = new ManejadorLogin(socket);
                
             
                invitados.put(socket.getPort(), manejador);
        
                new Thread(manejador).start();
               
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}