package com.library.seatmgmt.util;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Standardized logging utility using java.util.logging.
 * Logs application actions, warnings, and errors to both console and a rotating log file.
 */
public final class LoggerUtil {

    private static final Logger LOGGER = Logger.getLogger("LibrarySeatMgmt");
    private static boolean initialized = false;

    private LoggerUtil() {
    }

    public static synchronized Logger getLogger() {
        if (!initialized) {
            initLogger();
        }
        return LOGGER;
    }

    private static void initLogger() {
        try {
            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.ALL);

            // Ensure logs directory exists
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }

            // Custom Clean Formatter
            Formatter customFormatter = new Formatter() {
                private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                @Override
                public String format(LogRecord record) {
                    return String.format("[%s] [%-7s] %s%n",
                            LocalDateTime.now().format(dtf),
                            record.getLevel().getName(),
                            record.getMessage());
                }
            };

            // Console Handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(customFormatter);
            LOGGER.addHandler(consoleHandler);

            // File Handler (rotating file, max 2MB, up to 3 files)
            FileHandler fileHandler = new FileHandler("logs/library_system.log", 2 * 1024 * 1024, 3, true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(customFormatter);
            LOGGER.addHandler(fileHandler);

            initialized = true;
            LOGGER.info("Logger initialized successfully. Outputting to logs/library_system.log");
        } catch (IOException e) {
            System.err.println("Failed to initialize FileHandler for Logger: " + e.getMessage());
        }
    }

    public static void info(String message) {
        getLogger().info(message);
    }

    public static void warning(String message) {
        getLogger().warning(message);
    }

    public static void severe(String message, Throwable throwable) {
        getLogger().log(Level.SEVERE, message, throwable);
    }
}
