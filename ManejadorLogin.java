import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ManejadorLogin implements Runnable {

    private Socket socket;

    public ManejadorLogin(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                DataInputStream entrada = new DataInputStream(socket.getInputStream());
                DataOutputStream salida = new DataOutputStream(socket.getOutputStream())
        ) {

            salida.writeUTF("Bienvenido. Escribe [1] para Iniciar Sesión o [2] para Registrarse");


            String credenciales = entrada.readUTF();
            String[] partes = credenciales.split(":", 3);

            String comando = partes[0];
            String usuario = (partes.length > 1) ? partes[1] : "";
            String contrasena = (partes.length > 2) ? partes[2] : ""; // Variable renombrada

            String respuesta = "";
            boolean loginExitoso = false;


            if ("LOGIN".equals(comando)) {


                if (ServidorHilos.clientes.containsKey(usuario)) {
                    respuesta = "ERROR: El usuario '" + usuario + "' ya está conectado.";
                }

                else if (db.loginUser(usuario, contrasena)) {
                    respuesta = "LOGIN_OK";
                    loginExitoso = true;
                } else {
                    respuesta = "ERROR: Usuario o contraseña incorrectos.";
                }

            } else if ("REGISTER".equals(comando)) {

                respuesta = db.registerUser(usuario, contrasena);

            } else {
                respuesta = "ERROR: Comando no reconocido.";
            }


            salida.writeUTF(respuesta);


            if (loginExitoso) {
                System.out.println("Se conectó el cliente: " + usuario);


                // Usamos el 'usuario' como idCliente
                DosClientes cliente = new DosClientes(socket, usuario);

                // Lo añadimos a la lista de clientes activos
                ServidorHilos.clientes.put(usuario, cliente);

                // Iniciamos el hilo del chat
                new Thread(cliente).start();



            } else {
                // Si no hubo login (fue registro o error), cerramos la conexión.
                socket.close();
            }

        } catch (IOException e) {
            System.out.println("Un cliente en proceso de login se desconectó: " + e.getMessage());
        }
    }
}