import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Scanner scanner;

    public static void main(String[] args) {
        Cliente cliente = new Cliente();
        cliente.startConnection();
    }

    public void startConnection() {
        scanner = new Scanner(System.in);

        try {
            System.out.print("🔹 Ingresa la IP del servidor: ");
            String address = scanner.nextLine();

            System.out.print("🔹 Ingresa el puerto: ");
            int port = Integer.parseInt(scanner.nextLine());

            socket = new Socket(address, port);
            System.out.println("Conectado a " + address + ":" + port);

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Thread listener = new Thread(() -> {
                try {
                    while (true) {
                        System.out.println("\n" + in.readUTF());
                    }
                } catch (IOException e) {
                    System.out.println("Desconectado del servidor.");
                }
            });
            listener.start();

            while (true) {
                String msg = scanner.nextLine();
                out.writeUTF(msg);
            }

        } catch (IOException e) {
            System.out.println("Error al conectar: " + e.getMessage());
        }
    }
}