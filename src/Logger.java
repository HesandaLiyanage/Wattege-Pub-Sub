/**
 * Minimal stub for Logger.
 * Created to satisfy type declarations for Broker.java until 
 * Ushitha's actual implementation is merged.
 */
public class Logger {
    public static void info(String tag, String msg) {
        System.out.println("[" + tag + "] " + msg);
    }
    public static void warn(String tag, String msg) {
        System.out.println("[" + tag + "] WARN: " + msg);
    }
    public static void error(String tag, String msg, Throwable t) {
        System.err.println("[" + tag + "] ERROR: " + msg);
        if (t != null) {
            t.printStackTrace();
        }
    }
}
