
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;

public class ServidorHilos {
    static HashMap<String, DosClientes> clientes = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(8080)) {
            System.out.println("🟢 Servidor iniciado en puerto 8080...");
            int idCliente = 0;

            while (true) {
                Socket socket = servidor.accept();
                String id = Integer.toString(idCliente);

                db.addUser(id);

                DosClientes cliente = new DosClientes(socket, id);
                clientes.put(id, cliente);
                new Thread(cliente).start();

                System.out.println("👤 Se conectó el cliente: " + id);
                idCliente++;
            }

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
