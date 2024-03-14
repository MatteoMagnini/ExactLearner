package org.experiments;

import org.experiments.logger.SmartLogger;
import org.experiments.task.Task;
import static org.experiments.logger.SmartLogger.isFileInCache;

public class Environment {

    public static void run(Task task) {
        // Setup logging
        String filename = task.getFileName();
        // If filename is already present in the cache, then skip the task
        if (isFileInCache(filename)) {
            System.out.println("Task " + task.getTaskName() + " is already present in the cache.");
        } else {
            // Enable file logging
            try {
                SmartLogger.enableFileLogging(filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Run the task
            task.run();
            // Disable file logging
            SmartLogger.disableFileLogging();
            System.out.println("Task " + task.getTaskName() + " is completed.");
        }
    }
}
