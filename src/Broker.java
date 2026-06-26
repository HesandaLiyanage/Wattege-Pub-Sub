import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Broker {

    private static volatile Broker instance;

    // Concurrent structure mapping Topic -> Set of ClientHandlers
    private final ConcurrentHashMap<String, Set<ClientHandler>> subscribersByTopic;
    
    // Statistics counters
    private final AtomicLong totalMessages;

    private Broker() {
        this.subscribersByTopic = new ConcurrentHashMap<>();
        this.totalMessages = new AtomicLong(0);
    }

    /**
     * Singleton accessor for Broker.
     */
    public static Broker getInstance() {
        if (instance == null) {
            synchronized (Broker.class) {
                if (instance == null) {
                    instance = new Broker();
                }
            }
        }
        return instance;
    }

    /**
     * Subscribes a ClientHandler to a specific topic.
     * @param topic the topic to subscribe to
     * @param handler the subscriber
     */
    public void subscribe(String topic, ClientHandler handler) {
        Set<ClientHandler> topicSubscribers = subscribersByTopic.computeIfAbsent(
                topic, k -> ConcurrentHashMap.newKeySet()
        );
        topicSubscribers.add(handler);
        
        Logger.info("broker", String.format("%s subscribed to %s (%d subscribers)", 
                handler.getId(), topic, topicSubscribers.size()));
    }

    /**
     * Unsubscribes a ClientHandler from all topics.
     * @param handler the subscriber to remove
     */
    public void unsubscribe(ClientHandler handler) {
        for (Map.Entry<String, Set<ClientHandler>> entry : subscribersByTopic.entrySet()) {
            Set<ClientHandler> topicSubscribers = entry.getValue();
            if (topicSubscribers.remove(handler)) {
                Logger.info("broker", String.format("%s unsubscribed from %s (%d subscribers remaining)", 
                        handler.getId(), entry.getKey(), topicSubscribers.size()));
            }
        }
    }

    /**
     * Publishes a message to all subscribers of a specific topic.
     * @param topic the topic to publish to
     * @param payload the message content
     * @param pubId the publisher ID
     */
    public void publish(String topic, String payload, String pubId) {
        Set<ClientHandler> topicSubscribers = subscribersByTopic.get(topic);
        
        if (topicSubscribers == null || topicSubscribers.isEmpty()) {
            Logger.info("broker", String.format("no subscribers for %s; dropped", topic));
            return;
        }

        totalMessages.incrementAndGet();
        String formattedMessage = String.format("[%s@%s] %s", pubId, topic, payload);
        
        Iterator<ClientHandler> iterator = topicSubscribers.iterator();
        while (iterator.hasNext()) {
            ClientHandler handler = iterator.next();
            try {
                handler.send(formattedMessage);
            } catch (Exception e) {
                Logger.error("broker", String.format("failed to send to %s on %s, removing subscriber", 
                        handler.getId(), topic), e);
                iterator.remove();
            }
        }
    }

    /**
     * Admin statistics: Subscriber count per topic.
     */
    public Map<String, Integer> topicSubscriberCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, Set<ClientHandler>> entry : subscribersByTopic.entrySet()) {
            int size = entry.getValue().size();
            if (size > 0) {
                counts.put(entry.getKey(), size);
            }
        }
        return counts;
    }

    /**
     * Admin statistics: Total messages routed.
     */
    public long totalMessagesPublished() {
        return totalMessages.get();
    }
}
