import java.net.Socket;

/**
 * Minimal stub for ClientHandler.
 * Created to satisfy type declarations for Broker.java until 
 * Hesanda's actual implementation is merged.
 */
public class ClientHandler implements Runnable {
    
    public ClientHandler(Socket socket) { }
    
    public String getId() { 
        return "stub-id"; 
    }
    
    public String getRole() { 
        return "SUBSCRIBER"; 
    }
    
    public String getTopic() { 
        return "GLOBAL"; 
    }
    
    public void send(String line) {
        // Stub for sending message to the socket
    }
    
    @Override
    public void run() { 
        // Thread execution stub
    }
}
