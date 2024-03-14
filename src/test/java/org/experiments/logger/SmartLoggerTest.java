package org.experiments.logger;

import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.assertTrue;

public class SmartLoggerTest {

    @Test
    public void testEnableFileLogging() throws IOException {
        // Test the SmartLogger
        String fileName = "test";
        // If the file is already present in the cache, remove it
        SmartLogger.removeFileFromCache(fileName);
        SmartLogger.enableFileLogging("test");
        SmartLogger.log("This is a test message.");
        SmartLogger.disableFileLogging();
        assertTrue(SmartLogger.isFileInCache(fileName));
    }
}
