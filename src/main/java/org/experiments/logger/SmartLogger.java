package org.experiments.logger;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SmartLogger {

    private static final Logger logger = Logger.getLogger(SmartLogger.class.getName());
    ;
    private static final String CACHE_DIR = "cache";
    private static final String FILE_EXTENSION = ".csv";

    public static String getFullFileName(String filename) {
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

    public static void enableFileLogging(String filename) {
        enableFileLogging(filename, "");
    }

    public static void enableFileLogging(String filename, String lineTerminator) {
        // If cache directory does not exist, then create it
        if (!new File(CACHE_DIR).exists()) {
            new File(CACHE_DIR).mkdir();
        }
        try {
            // Create a new FileHandler with a custom formatter
            Handler fileHandler = new FileHandler(getFullFileName(filename));
            fileHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(java.util.logging.LogRecord record) {
                    return record.getMessage() + lineTerminator;
                }
            });
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Error setting up FileHandler: " + e.getMessage());
        }
    }

    public static void disableFileLogging() {
        if (logger.getHandlers().length > 0) {
            logger.getHandlers()[0].close();
            logger.removeHandler(logger.getHandlers()[0]);
        }
    }

    /**
     * Check if the file is already present in the cache
     *
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

    /**
     * Check cache's files integrity
     * If a file is present in the cache but does not contain the answer, then remove it from the cache
     */
    public static void checkCachedFiles() {
        File cacheDir = new File(CACHE_DIR);
        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    try {
                        String content = java.nio.file.Files.readString(file.toPath());
                        if (content.contains("True") || content.contains("False")) {
                            System.out.println("File" + file.getName() + " Integrity check ... OK");
                        } else {
                            System.out.println("File " + file.getName() + " Integrity check ... FAILED, removing.");
                            file.delete();
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + e.getMessage());
                    }

                }
            }
        }
    }
}
