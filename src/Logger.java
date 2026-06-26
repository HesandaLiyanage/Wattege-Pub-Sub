import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger.java — Timestamped, level-tagged console logger for the
 * IS3208 Assignment 01 Pub/Sub Middleware.
 *
 * Member 5: Ushitha
 *
 * Log format (exact):
 *   YYYY-MM-DD HH:mm:ss [tag] msg
 *
 * Example output:
 *   2026-06-25 10:21:03 [server] INFO  Listening on port 5000
 *   2026-06-25 10:21:07 [broker] WARN  No subscribers for TOPIC_B; message dropped
 *   2026-06-25 10:21:09 [handler] ERROR Read failed: Connection reset by peer
 *
 * All methods synchronize on {@link System#out} so that log lines from
 * concurrent threads are never interleaved.
 */
public class Logger {

    // -----------------------------------------------------------------------
    // Formatter (package-private so tests can read it if needed)
    // -----------------------------------------------------------------------

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // -----------------------------------------------------------------------
    // Private constructor — utility class, not meant to be instantiated.
    // -----------------------------------------------------------------------

    private Logger() {
        throw new UnsupportedOperationException("Logger is a utility class.");
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Logs an informational message.
     *
     * <pre>
     *   Logger.info("server", "Listening on port 5000");
     *   // →  2026-06-25 10:21:03 [server] INFO  Listening on port 5000
     * </pre>
     *
     * @param tag Short label that identifies the component (e.g. {@code "server"},
     *            {@code "broker"}, {@code "handler"}).
     * @param msg The message to log.
     */
    public static void info(String tag, String msg) {
        log("INFO ", tag, msg, null);
    }

    /**
     * Logs a warning message.
     *
     * <pre>
     *   Logger.warn("broker", "No subscribers for TOPIC_B; message dropped");
     *   // →  2026-06-25 10:21:07 [broker] WARN  No subscribers for TOPIC_B; message dropped
     * </pre>
     *
     * @param tag Short label identifying the component.
     * @param msg The warning message.
     */
    public static void warn(String tag, String msg) {
        log("WARN ", tag, msg, null);
    }

    /**
     * Logs an error message, optionally with a {@link Throwable} stack trace.
     *
     * <pre>
     *   Logger.error("handler", "Read failed", e);
     *   // →  2026-06-25 10:21:09 [handler] ERROR Read failed
     *   //       java.io.IOException: Connection reset by peer
     *   //         at ...
     * </pre>
     *
     * @param tag Short label identifying the component.
     * @param msg The error description.
     * @param t   The throwable whose stack trace will be printed, or {@code null}
     *            if there is no associated exception.
     */
    public static void error(String tag, String msg, Throwable t) {
        log("ERROR", tag, msg, t);
    }

    // -----------------------------------------------------------------------
    // Internal implementation
    // -----------------------------------------------------------------------

    /**
     * Core logging method.  Synchronises on {@link System#out} so that
     * multi-line log entries (e.g. those with stack traces) are written
     * atomically without interleaving from other threads.
     *
     * @param level Padded level label: {@code "INFO "}, {@code "WARN "}, or {@code "ERROR"}.
     * @param tag   Component tag.
     * @param msg   Log message.
     * @param t     Optional throwable; its stack trace is printed immediately
     *              after the log line when non-{@code null}.
     */
    private static void log(String level, String tag, String msg, Throwable t) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String line = timestamp + " [" + tag + "] " + level + " " + msg;

        // Synchronise on System.out to prevent interleaving from concurrent threads.
        synchronized (System.out) {
            System.out.println(line);
            if (t != null) {
                // Print the full stack trace inside the same synchronized block so
                // the exception detail stays attached to its log line.
                t.printStackTrace(System.out);
            }
        }
    }
}
