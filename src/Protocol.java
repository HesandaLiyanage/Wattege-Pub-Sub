/**
 * Protocol.java — Wire protocol constants and helper methods for the
 * IS3208 Assignment 01 Pub/Sub Middleware.
 *
 * Member 5: Ushitha
 *
 * All messages are line-based UTF-8, one message per line, '\n' terminated.
 *
 * Wire protocol summary:
 *   Client → Server (first line) : REGISTER <ROLE> <TOPIC>
 *   Publisher → Server           : MSG <payload>
 *   Client → Server              : TERMINATE
 *   Server → Subscriber          : [<pubId>@<TOPIC>] <payload>
 *   Server → Client              : ACK <message> | ERR <message>
 */
public class Protocol {

    // -----------------------------------------------------------------------
    // Command constants
    // -----------------------------------------------------------------------

    /** Client opens the session with this keyword: {@code REGISTER <ROLE> <TOPIC>} */
    public static final String REGISTER = "REGISTER";

    /** Publisher sends a message payload: {@code MSG <payload>} */
    public static final String MSG = "MSG";

    /** Either party may send this to initiate a clean disconnect. */
    public static final String TERMINATE = "TERMINATE";

    /** Server acknowledgement: {@code ACK <message>} */
    public static final String ACK = "ACK";

    /** Server error response: {@code ERR <message>} */
    public static final String ERR = "ERR";

    /** The fallback topic used when a client does not specify one. */
    public static final String DEFAULT_TOPIC = "GLOBAL";

    // -----------------------------------------------------------------------
    // Private constructor — utility class, not meant to be instantiated.
    // -----------------------------------------------------------------------

    private Protocol() {
        throw new UnsupportedOperationException("Protocol is a utility class.");
    }

    // -----------------------------------------------------------------------
    // Format helpers (client-side construction)
    // -----------------------------------------------------------------------

    /**
     * Builds the registration handshake line that a client sends as its very
     * first message after connecting.
     *
     * <pre>
     *   formatRegister("PUBLISHER", "TOPIC_A")  →  "REGISTER PUBLISHER TOPIC_A"
     *   formatRegister("SUBSCRIBER", "GLOBAL")  →  "REGISTER SUBSCRIBER GLOBAL"
     * </pre>
     *
     * @param role  {@code "PUBLISHER"} or {@code "SUBSCRIBER"} (case-sensitive).
     * @param topic The topic name; use {@link #DEFAULT_TOPIC} when none is given.
     * @return The formatted REGISTER line ready to be sent over the socket.
     */
    public static String formatRegister(String role, String topic) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role must not be null or blank");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be null or blank");
        }
        return REGISTER + " " + role.trim() + " " + topic.trim();
    }

    /**
     * Wraps a publisher's payload in the wire-protocol MSG envelope.
     *
     * <pre>
     *   formatMsg("hello world")  →  "MSG hello world"
     * </pre>
     *
     * @param payload The raw text to publish; may contain spaces.
     * @return The formatted MSG line ready to be sent over the socket.
     */
    public static String formatMsg(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        return MSG + " " + payload;
    }

    /**
     * Builds the broadcast line the server delivers to every subscriber of a topic.
     *
     * <pre>
     *   formatBroadcast("pub-3", "TOPIC_A", "hello world")
     *       →  "[pub-3@TOPIC_A] hello world"
     * </pre>
     *
     * @param pubId   The short publisher id assigned by the server (e.g. {@code "pub-3"}).
     * @param topic   The topic on which the message was published.
     * @param payload The original message payload.
     * @return The formatted broadcast line ready to be written to each subscriber's socket.
     */
    public static String formatBroadcast(String pubId, String topic, String payload) {
        if (pubId == null || pubId.isBlank()) {
            throw new IllegalArgumentException("pubId must not be null or blank");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be null or blank");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        return "[" + pubId.trim() + "@" + topic.trim() + "] " + payload;
    }

    // -----------------------------------------------------------------------
    // Parse helpers (server-side extraction)
    // -----------------------------------------------------------------------

    /**
     * Parses the REGISTER handshake line sent by the client as the very first
     * message after connecting.
     *
     * <p>Expected format: {@code REGISTER <ROLE> <TOPIC>}
     *
     * <pre>
     *   parseRegister("REGISTER PUBLISHER TOPIC_A")  →  {"PUBLISHER", "TOPIC_A"}
     *   parseRegister("REGISTER SUBSCRIBER GLOBAL")  →  {"SUBSCRIBER", "GLOBAL"}
     * </pre>
     *
     * @param line The raw line read from the client socket.
     * @return A two-element array {@code {role, topic}}.
     * @throws IllegalArgumentException if the line is {@code null}, does not
     *         start with the {@code REGISTER} keyword, or has fewer than three
     *         whitespace-separated tokens.  The server should catch this and
     *         reply with {@code ERR bad handshake}.
     */
    public static String[] parseRegister(String line) {
        if (line == null) {
            throw new IllegalArgumentException("REGISTER line must not be null");
        }

        // Normalise: collapse any run of whitespace to a single space, then split.
        String[] tokens = line.trim().split("\\s+");

        // Must be exactly 3 tokens: REGISTER <ROLE> <TOPIC>
        if (tokens.length < 3) {
            throw new IllegalArgumentException(
                    "Malformed REGISTER line (expected 'REGISTER <ROLE> <TOPIC>'): " + line);
        }

        if (!REGISTER.equalsIgnoreCase(tokens[0])) {
            throw new IllegalArgumentException(
                    "Line does not start with REGISTER keyword: " + line);
        }

        String role  = tokens[1].toUpperCase();
        String topic = tokens[2].toUpperCase();

        // Validate role value.
        if (!role.equals("PUBLISHER") && !role.equals("SUBSCRIBER")) {
            throw new IllegalArgumentException(
                    "Invalid role '" + role + "'; must be PUBLISHER or SUBSCRIBER");
        }

        return new String[]{role, topic};
    }

    /**
     * Extracts the payload from a {@code MSG <payload>} line.
     *
     * <pre>
     *   parseMsgPayload("MSG hello world")  →  "hello world"
     *   parseMsgPayload("MSG   spaces  ")   →  "  spaces  "   (leading space preserved)
     * </pre>
     *
     * @param line The raw MSG line read from the client socket.
     * @return Everything that follows the {@code "MSG "} prefix.
     * @throws IllegalArgumentException if the line is {@code null} or does not
     *         start with the {@code MSG} keyword followed by a space.
     */
    public static String parseMsgPayload(String line) {
        if (line == null) {
            throw new IllegalArgumentException("MSG line must not be null");
        }

        String prefix = MSG + " ";
        if (!line.startsWith(prefix)) {
            throw new IllegalArgumentException(
                    "Line does not start with '" + prefix + "': " + line);
        }

        // Return everything after "MSG " — preserves internal spacing.
        return line.substring(prefix.length());
    }
}
