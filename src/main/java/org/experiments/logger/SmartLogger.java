package org.experiments.logger;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public class SmartLogger {

    private static final Logger logger = Logger.getLogger(SmartLogger.class.getName());;
    private static final String CACHE_DIR = "cache";
    private static final String FILE_EXTENSION = ".csv";

    static {
        // Remove all default handlers
        logger.setUseParentHandlers(false);

        // Create a new ConsoleHandler with a custom formatter
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter() {
            @Override
            public String format(java.util.logging.LogRecord record) {
                return record.getMessage() + "\n";
            }
        });
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.ALL); // Set the logger level as needed
    }

    private static String getFullFileName(String filename) {
        return CACHE_DIR + System.getProperty("file.separator") + filename + FILE_EXTENSION;
    }

    public static void log(String message) {
        logger.info(message);
    }

    public static void removeFileFromCache(String filename) {
        File file = new File(getFullFileName(filename));
        if (file.exists()) {
            file.delete();
        }
    }

    public static void enableFileLogging(String filename) throws IOException {
        // If cache directory does not exist, then create it
        if (!new File(CACHE_DIR).exists()) {
            new File(CACHE_DIR).mkdir();
        }
        logger.addHandler(new FileHandler(getFullFileName(filename)));
    }

    public static void disableFileLogging() {
        logger.getHandlers()[0].close();
        logger.removeHandler(logger.getHandlers()[0]);
    }

    /**
     * Check if the file is already present in the cache
     * @param filename
     * @return true if the file is present in the cache, false otherwise
     */
    public static boolean isFileInCache(String filename) {
        // If cache directory does not exist, then return false
        if (!new File(CACHE_DIR).exists()) {
            return false;
        } else {
            // If the file is present in the cache, then return true
            File file = new File(getFullFileName(filename));
            return file.exists();
        }
    }

}
