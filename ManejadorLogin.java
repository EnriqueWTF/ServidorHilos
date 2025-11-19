import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ManejadorLogin implements Runnable {

    private Socket socket;

 
    public DataOutputStream salida;
  


    public ManejadorLogin(Socket socket) {
        this.socket = socket;
    }

  
    public void enviarMensaje(String msg) {
        try {
            if (salida != null) {
                salida.writeUTF(msg);
            }
        } catch (IOException e) {
        
        }
    }

    @Override
    public void run() {

        DataInputStream entrada = null;
       

        boolean loginExitoso = false;
        int mensajesGratisEnviados = 0;
        final int MAX_MENSAJES_GRATIS = 3;
        String nombreInvitado = "Invitado-" + socket.getPort();

        try {

            entrada = new DataInputStream(socket.getInputStream());
            this.salida = new DataOutputStream(socket.getOutputStream());


            salida.writeUTF("Bienvenido. Tienes " + MAX_MENSAJES_GRATIS + " mensajes gratis.");
            salida.writeUTF("Escribe [1] para Iniciar Sesión o [2] para Registrarse en cualquier momento.");

         
            String mensajeRecibido = "";
            while (!loginExitoso) {

                mensajeRecibido = entrada.readUTF();
                String[] partes = mensajeRecibido.split(":", 3);

                String comando = partes[0];
                String usuario = (partes.length > 1) ? partes[1] : "";
                String contrasena = (partes.length > 2) ? partes[2] : "";

                String respuesta = "";

                if ("LOGIN".equals(comando)) {
                    if (ServidorHilos.clientes.containsKey(usuario)) {
                        respuesta = "ERROR: El usuario '" + usuario + "' ya está conectado.";
                    } else if (db.loginUser(usuario, contrasena)) {
                        respuesta = "LOGIN_OK";
                        loginExitoso = true; 
                    } else {
                        respuesta = "ERROR: Usuario o contraseña incorrectos.";
                    }
                    salida.writeUTF(respuesta);

                } else if ("REGISTER".equals(comando)) {
                    respuesta = db.registerUser(usuario, contrasena);
                    salida.writeUTF(respuesta);
             

                } else {
                 
                    if (mensajesGratisEnviados < MAX_MENSAJES_GRATIS) {
                        mensajesGratisEnviados++;
                        String msgFormateado = "💬 [" + nombreInvitado + "]: " + mensajeRecibido;

                     
                        for (DosClientes cliente : ServidorHilos.clientes.values()) {
                            try {
                                cliente.salida.writeUTF(msgFormateado);
                            } catch (IOException e) { /* Ignoramos si uno falla */ }
                        }
                        
                    
                        for (ManejadorLogin invitado : ServidorHilos.invitados.values()) {
                            
                            if (invitado != this) {
                                invitado.enviarMensaje(msgFormateado);
                            }
                        }

                     
                        salida.writeUTF("Mensaje gratis enviado (" + mensajesGratisEnviados + "/" + MAX_MENSAJES_GRATIS + ").");

                    } else {
                        respuesta = "Has agotado tus " + MAX_MENSAJES_GRATIS + " mensajes gratis. " +
                                "Escribe [1] Iniciar Sesión o [2] Registrarse.";
                        salida.writeUTF(respuesta);
                    }
                    
                }
            } 

           
            String usuario = mensajeRecibido.split(":", 3)[1];
            System.out.println("Se conectó el cliente: " + usuario);

            DosClientes cliente = new DosClientes(socket, usuario);
            ServidorHilos.clientes.put(usuario, cliente);
            new Thread(cliente).start();
          

        } catch (IOException e) {
            System.out.println("Un cliente en proceso de login/invitado se desconectó: " + e.getMessage());
        } finally {
          
            ServidorHilos.invitados.remove(socket.getPort());
            
     
            if (!loginExitoso) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ex) { /* Ignoramos error al cerrar */ }
            }
        }
    }
}