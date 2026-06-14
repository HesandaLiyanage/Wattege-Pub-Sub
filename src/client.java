import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class client{
    public static void main(String[] args) throws IOException {

        if (args.length < 3) {
            System.out.println("Usage: java client <SERVER_IP> <PORT> <PUBLISHER|SUBSCRIBER>");
            return;
        }

        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String role = args[2].toUpperCase();

        Socket socket = new Socket(serverIp, serverPort);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        System.out.println("Connected to server " + serverIp + ":" + serverPort + " as " + role);

        writer.println("REGISTER|" + role);

        Thread listener = new Thread(() -> {
            try {
                String incoming;
                while ((incoming = reader.readLine()) != null) {
                    System.out.println("[BROADCAST]"+incoming);
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
            }
        });
        listener.setDaemon(true);
        listener.start();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Type messages and press Enter. Type 'terminate' to exit.");

        while (true) {
            String message = scanner.nextLine();
            writer.println(message);

            if (message.equalsIgnoreCase("terminate")) {
                System.out.println("Disconnecting.....");
                break;
            }
        }

        socket.close();
        System.out.println("Client closed.");
    }
}
