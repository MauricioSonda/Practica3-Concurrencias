import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {

    private static final Map<String, DataOutputStream> clientes = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        final int PORT = 8080;

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado en el puerto " + PORT);

            while (true) {
                Socket socket = server.accept();
                System.out.println("Cliente conectado: " + socket.getInetAddress());
                new Thread(new ClientHandler(socket)).start();
            }

        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String username;
        private DataInputStream in;
        private DataOutputStream out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                startConnection();

                String mensaje;
                while (true) {
                    mensaje = in.readUTF();

                    if (mensaje.startsWith("/name ")) {
                        changeUserName(mensaje.substring(6));
                    } else if (mensaje.startsWith("/msg ")) {
                        sendPrivateMessage(mensaje);
                    } else if (mensaje.startsWith("/global ")) {
                        sendGlobalMessage(mensaje.substring(8));
                    } else if (mensaje.equals("/clientes")) {
                        showConnectedClients();
                    } else {
                        out.writeUTF("Comando no reconocido. Usa /name, /msg, /global o /clientes.");
                    }
                }

            } catch (IOException e) {
                if (username != null) {
                    System.out.println("Usuario desconectado: " + username);
                    clientes.remove(username);
                    try {
                        sendGlobalMessage(username + " salió del chat");
                    } catch (IOException ignored) {}
                }
            }
        }

        // startConnection()
        private void startConnection() throws IOException {
            out.writeUTF("Bienvenido al chat. Ingresa tu nombre de usuario:");
            username = in.readUTF();

            synchronized (clientes) {
                while (clientes.containsKey(username)) {
                    out.writeUTF("El nombre ya está en uso, elige otro:");
                    username = in.readUTF();
                }
                clientes.put(username, out);
            }

            out.writeUTF("Conectado como: " + username);
            sendGlobalMessage(username + " se ha unido al chat.");
        }

        // changeUserName()
        private void changeUserName(String nuevo) throws IOException {
            synchronized (clientes) {
                if (clientes.containsKey(nuevo)) {
                    out.writeUTF("El nombre '" + nuevo + "' ya está en uso.");
                } else {
                    clientes.remove(username);
                    clientes.put(nuevo, out);
                    sendGlobalMessage("→" + username + " ahora se llama " + nuevo);
                    username = nuevo;
                }
            }
        }

        // sendPrivateMessage()
        private void sendPrivateMessage(String comando) throws IOException {
            String[] partes = comando.split(" ", 3);
            if (partes.length < 3) {
                out.writeUTF("Uso correcto: /msg <usuario> <mensaje>");
                return;
            }

            String destinatario = partes[1];
            String mensaje = partes[2];

            DataOutputStream destino = clientes.get(destinatario);
            if (destino != null) {
                destino.writeUTF("[Privado de " + username + "]: " + mensaje);
                out.writeUTF("(Privado a " + destinatario + "): " + mensaje);
            } else {
                out.writeUTF("Usuario '" + destinatario + "' no está conectado.");
            }
        }

        // sendGlobalMessage()
        private void sendGlobalMessage(String mensaje) throws IOException {
            synchronized (clientes) {
                for (Map.Entry<String, DataOutputStream> entry : clientes.entrySet()) {
                    if (!entry.getKey().equals(username)) {
                        entry.getValue().writeUTF("(Global) [" + username + "]: " + mensaje);
                    }
                }
            }
            System.out.println("(Global) " + username + ": " + mensaje);
        }

        // showConnectedClients()
        private void showConnectedClients() throws IOException {
            synchronized (clientes) {
                StringBuilder lista = new StringBuilder("Usuarios conectados (" + clientes.size() + "):\n");
                for (String user : clientes.keySet()) {
                    lista.append(" - ").append(user).append("\n");
                }
                out.writeUTF(lista.toString());
            }
        }
    }
}