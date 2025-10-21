

import java.io.*;
import java.net.Socket;

public class DosClientes implements Runnable {

    final DataOutputStream salida;
    final DataInputStream entrada;
    public final String idCliente;

    public DosClientes(Socket s, String idCliente) throws IOException {
        this.idCliente = idCliente;
        this.salida = new DataOutputStream(s.getOutputStream());
        this.entrada = new DataInputStream(s.getInputStream());

        salida.writeUTF("Conectado como usuario " + idCliente);
        salida.writeUTF("Comandos: /block <id>, /unblock <id>, @<id> <mensaje>");
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

                } else if (mensaje.startsWith("@")) {
                    enviarDirecto(mensaje);

                } else {
                    enviarBroadcast(mensaje);
                }

            } catch (IOException e) {
                System.out.println("Cliente " + idCliente + " desconectado.");
                ServidorHilos.clientes.remove(idCliente);
                break;
            }
        }
    }

    
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

        receptor.salida.writeUTF("Mensaje privado de " + idCliente + ": " + texto);
        salida.writeUTF("Enviado a " + aQuien);
    }

    private void enviarBroadcast(String mensaje) throws IOException {
        for (DosClientes cliente : ServidorHilos.clientes.values()) {
            if (!cliente.idCliente.equals(idCliente) && !db.isBlocked(cliente.idCliente, idCliente)) {
                cliente.salida.writeUTF("💬 " + idCliente + ": " + mensaje);
            }
        }
        salida.writeUTF("Mensaje enviado a todos (menos los que te bloquearon).");
    }
}
