package com.islandium.core.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for logging UI commands for debugging purposes.
 * Sends debug data to a local web viewer via HTTP.
 */
public class UIDebugLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("UIDebug");

    /** Set to true to enable UI debug logging on all pages */
    public static boolean DEBUG_UI = true;

    /** URL of the debug viewer API */
    private static final String DEBUG_API_URL = "http://172.252.236.20:3847/api/debug";

    /** Thread-local buffer to collect all UI code during a build */
    private static final ThreadLocal<StringBuilder> buffer = ThreadLocal.withInitial(StringBuilder::new);

    /** Thread-local to store current page name */
    private static final ThreadLocal<String> currentPage = new ThreadLocal<>();

    /** Date formatter for timestamps */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** Executor for async HTTP calls */
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UIDebug-Sender");
        t.setDaemon(true);
        return t;
    });

    /**
     * Start collecting UI code. Call at the beginning of build().
     */
    public static void startCollecting(String pageName) {
        if (!DEBUG_UI) return;
        currentPage.set(pageName);
        StringBuilder sb = buffer.get();
        sb.setLength(0);
    }

    /**
     * Add UI code to the buffer. Call before each appendInline().
     */
    public static void collect(String label, String uiCode) {
        if (!DEBUG_UI) return;
        StringBuilder sb = buffer.get();
        sb.append("--- ").append(label).append(" ---\n");
        sb.append(uiCode).append("\n\n");
    }

    /**
     * Flush and send all collected UI code to the viewer. Call at the end of build().
     */
    public static void flush(String pageName) {
        if (!DEBUG_UI) return;
        StringBuilder sb = buffer.get();

        if (sb.length() == 0) return;

        String content = sb.toString();
        sb.setLength(0);

        // Send async
        sendToViewer(pageName, content);
    }

    /**
     * Simple log method for UI code.
     * Collects in buffer (use flush() to send).
     */
    public static void log(String pageName, String uiCode) {
        if (!DEBUG_UI) return;
        collect(pageName, uiCode);
    }

    /**
     * Send data to the debug viewer.
     */
    private static void sendToViewer(String page, String content) {
        executor.submit(() -> {
            try {
                URL url = new URL(DEBUG_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);

                // Build JSON manually to avoid dependencies
                String json = String.format(
                    "{\"page\":\"%s\",\"time\":\"%s\",\"code\":%s}",
                    escapeJson(page),
                    LocalDateTime.now().format(DATE_FORMAT),
                    escapeJsonValue(content)
                );

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    LOGGER.warn("Debug viewer returned status {}", responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                // Silently ignore - viewer might not be running
                // LOGGER.debug("Could not send to debug viewer: {}", e.getMessage());
            }
        });
    }

    /**
     * Escape a string for JSON.
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Escape a string value for JSON (with quotes).
     */
    private static String escapeJsonValue(String str) {
        return "\"" + escapeJson(str) + "\"";
    }

    /**
     * Logs all commands from a UICommandBuilder.
     */
    public static void logCommands(String pageName, UICommandBuilder cmd) {
        if (!DEBUG_UI) return;

        CustomUICommand[] commands = cmd.getCommands();
        if (commands.length == 0) return;

        StringBuilder sb = new StringBuilder();
        sb.append("=== UI COMMANDS: ").append(pageName).append(" (").append(commands.length).append(" commands) ===\n\n");

        for (int i = 0; i < commands.length; i++) {
            CustomUICommand command = commands[i];
            sb.append("[").append(i).append("] ").append(command.type.name());

            if (command.selector != null && !command.selector.isEmpty()) {
                sb.append(" | selector: ").append(command.selector);
            }
            if (command.data != null && !command.data.isEmpty()) {
                sb.append(" | data: ").append(command.data);
            }
            if (command.text != null && !command.text.isEmpty()) {
                sb.append(" | text: ").append(command.text);
            }
            sb.append("\n");
        }

        sendToViewer(pageName + " (Commands)", sb.toString());
    }

    /**
     * Logs only AppendInline commands.
     */
    public static void logInlineCode(String pageName, UICommandBuilder cmd) {
        if (!DEBUG_UI) return;

        CustomUICommand[] commands = cmd.getCommands();

        StringBuilder sb = new StringBuilder();
        sb.append("=== UI INLINE CODE: ").append(pageName).append(" ===\n\n");

        for (CustomUICommand command : commands) {
            if (command.type.name().contains("Inline") && command.text != null) {
                sb.append("[").append(command.selector).append("]\n");
                sb.append(command.text).append("\n\n");
            }
        }

        sendToViewer(pageName + " (Inline)", sb.toString());
    }

    /**
     * Clear the debug viewer history.
     */
    public static void clearViewer() {
        executor.submit(() -> {
            try {
                URL url = new URL("http://172.252.236.20:3847/api/clear");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                // Silently ignore
            }
        });
    }
}
