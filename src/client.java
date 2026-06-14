import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class client{
    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Usage: java client <server-ip> <port>");
            return;
        }

        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(serverIp, serverPort);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server " + serverIp + ":" + serverPort);
            System.out.println("Type messages to send to the server. Type 'terminate' to end the connection.");

            while (true) {
                String message = scanner.nextLine();

                writer.println(message);

                if (message.equals("terminate")) {
                    System.out.println("Closing connection.");
                    break;
                }
            }
        }

        System.out.println("Client Closed.");
    }
}
