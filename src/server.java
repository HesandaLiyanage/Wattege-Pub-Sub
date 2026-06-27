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

    private static final String DEFAULT_TOPIC = "GLOBAL";

    static class ClientHandler implements Runnable {

        private final Socket clientSocket;
        private PrintWriter writer;
        private String role;   // PUBLISHER or SUBSCRIBER
        private String topic;  // topic this client is registered on

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
                    String[] parts = registerLine.split("\\|");
                    role  = parts.length > 1 ? parts[1] : null;
                    topic = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : DEFAULT_TOPIC;
                    System.out.println("Client registered as: " + role + " on topic " + topic);
                }

                String line;

                while ((line = reader.readLine()) != null) {

                    if (line.equalsIgnoreCase("terminate")) {
                        System.out.println(role + " disconnected.");
                        break;
                    }

                    System.out.println(role + " says: " + line);

                    if ("PUBLISHER".equalsIgnoreCase(role)) {
                        broadcastToSubscribers(line, topic);
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

        public String getTopic(){
            return topic;
        }

        private void broadcastToSubscribers(String message, String fromTopic){
            String formatted = "[" + fromTopic + "] " + message;
            for (ClientHandler client : clients) {
                if ("SUBSCRIBER".equalsIgnoreCase(client.getRole())
                        && fromTopic.equals(client.getTopic())) {
                    client.send(formatted);
                }
            }
        }
    }
}
