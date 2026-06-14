import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class server {

    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("Usage: java server <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);
            System.out.println("Waiting for clients on port " + port);

            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);

                Thread thread = new Thread(handler);
                thread.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {

        private final Socket clientSocket;
        private PrintWriter writer;
        private String role; // This can be Publisher ot Subscriber

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()))){

                writer = new PrintWriter(clientSocket.getOutputStream(), true);

                String registerLine = reader.readLine();

                if (registerLine != null && registerLine.startsWith("REGISTER")) {
                    role = registerLine.split("\\|")[1];
                    System.out.println("Client registered as: " + role);
                }

                String line;

                while ((line = reader.readLine()) != null) {

                    if (line.equalsIgnoreCase("terminate")) {
                        System.out.println(role + "disconnected.");
                        break;
                    }

                    System.out.println(role + "says: " + line);

                    if ("PUBLISHER".equalsIgnoreCase(role)) {
                        broadcastToSubscribers(line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection error: " + e.getMessage());
            } finally {
                clients.remove(this);
                try {
                    clientSocket.close();
                } catch (IOException ignored) {}
            }
        }

        public void send(String message){
            writer.println(message);
        }

        public String getRole(){
            return role;
        }

        private void broadcastToSubscribers(String message){
            for (ClientHandler client : clients) {
                if ("SUBSCRIBER".equalsIgnoreCase(client.getRole())) {
                    client.send(message);
                }
            }
        }
    }
}
