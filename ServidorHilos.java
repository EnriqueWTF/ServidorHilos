import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;

public class ServidorHilos {
    static HashMap<String, DosClientes> clientes = new HashMap<>();

    
    /** Almacena los juegos activos. La clave es "idMenor:idMayor" (ej: "0:1") */
    static HashMap<String, Gatito> juegosActivos = new HashMap<>();
    
    /** Almacena invitaciones. Key: id_retado, Value: id_retador */
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
            int idCliente = 0; // NOTA: Esto se reinicia. Considera usar IDs únicos de la DB a futuro.

            while (true) {
                Socket socket = servidor.accept();
                // (Usar un contador simple como ID es problemático si el servidor reinicia, 
                // pero lo mantenemos por simplicidad)
                String id = Integer.toString(idCliente);

                db.addUser(id);

                DosClientes cliente = new DosClientes(socket, id);
                clientes.put(id, cliente);
                new Thread(cliente).start();

                System.out.println("Se conectó el cliente: " + id);
                idCliente++;
            }

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}