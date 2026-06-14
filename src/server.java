import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class server {
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("Usage: java server <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);
            System.out.println("Waiting for client on port " + port);

            try (Socket clientSocket = serverSocket.accept()) {

                System.out.println("Connected to client " + clientSocket.getInetAddress());

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream())
                );

                String line;

                while ((line = reader.readLine()) != null) {
                    System.out.println("Client Says: " + line);

                    if (line.equalsIgnoreCase("terminate")) {
                        System.out.println("Client requested to close the connection.");
                        break;
                    }
                }
            }
        }

        System.out.println("Server shutting down.");
    }
}
