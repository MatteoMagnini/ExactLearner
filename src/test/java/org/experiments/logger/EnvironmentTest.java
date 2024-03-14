package org.experiments.logger;
import org.exactlearner.connection.OllamaBridge;
import org.experiments.Environment;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.experiments.logger.SmartLogger.getFullFileName;
import static org.junit.Assert.assertTrue;

public class EnvironmentTest {

    final static String modelName = "mixtral";
    final static String query = "What is the temperature of the Sun?";

    @Test
    public void testSimpleTaskInEnvironment() throws IOException {
        String taskName = "Task1";
        Task task = new ExperimentTask(taskName, modelName, "Dummy", "", () -> {
            SmartLogger.log("This is a simple task.");
        });
        Environment.run(task);
        assertTrue(SmartLogger.isFileInCache(task.getFileName()));
        String fileContent = Files.readString(Path.of(getFullFileName(task.getFileName())));
        assertTrue(fileContent.contains("This is a simple task."));
    }

    @Test
    public void testRealTaskInEnvironment() {
        String taskName = "Task2";
        Task task = new ExperimentTask(taskName, modelName, "Dummy", query, () -> {
            OllamaBridge bridge = new OllamaBridge(modelName);
            String response = bridge.ask(query);
            SmartLogger.log(response);
        });
        Environment.run(task);
        assertTrue(SmartLogger.isFileInCache(task.getFileName()));
    }
}
