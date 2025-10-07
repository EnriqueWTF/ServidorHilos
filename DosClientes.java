
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


public class DosClientes implements Runnable{
    
    final DataOutputStream salida;
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataInputStream entrada;
    String idCliente;
    DosClientes(Socket s, String idCliente) throws IOException{
        this.idCliente = idCliente;
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
                
    }
    
    @Override
    public void run() {
        String mensaje;
        while (true) {            
            try {
                mensaje = entrada.readUTF();
                if (mensaje.startsWith("@")) {
                    String[] partes= mensaje.split(" ");
                    String aQuien = partes[0].substring(1);
                    DosClientes cliente = ServidorHilos.clientes.get(aQuien);
                    if (cliente != null) {
                        cliente.salida.writeUTF("Mensjae directo de " + idCliente + ": " + partes[1]);
                    }
                }else{
                    for (DosClientes cliente : ServidorHilos.clientes.values()) {
                        if (cliente.idCliente != idCliente) {
                        cliente.salida.writeUTF("Mensjae directo de " + idCliente + ": " + mensaje);
                        }
                    }
                    
                }
            } catch (Exception ex) {
            }                               
        }
    }
    
}