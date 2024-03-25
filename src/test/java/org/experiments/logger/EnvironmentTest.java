package org.experiments.logger;
import org.exactlearner.connection.OllamaBridge;
import org.experiments.Environment;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.experiments.logger.SmartLogger.getFullFileName;
import static org.junit.Assert.assertTrue;

public class EnvironmentTest {

    final static String modelName = "mixtral";
    final static String query = "Give me a number between 1 and 10. Reply with just the number!";

    @Test
    public void testSimpleTaskInEnvironment() throws IOException {
        String taskName = "Task1";
        Task task = new ExperimentTask(taskName, modelName, "Dummy", "", "",() -> {
            SmartLogger.log("This is a simple task.");
        });
        Environment.run(task);
        assertTrue(SmartLogger.isFileInCache(task.getFileName()));
        String fileContent = Files.readString(Path.of(getFullFileName(task.getFileName())));
        assertTrue(fileContent.contains("This is a simple task."));
        SmartLogger.removeFileFromCache(task.getFileName());
    }

    @Test
    public void testOllamaRealTaskInEnvironment() {
        String taskName = "Task2";
        var workload = new OllamaWorkload(modelName, "", query, 10);
        Task task = new ExperimentTask(taskName, modelName, "Dummy", query, "",workload);
        Environment.run(task);
        assertTrue(SmartLogger.isFileInCache(task.getFileName()));
        SmartLogger.removeFileFromCache(task.getFileName());
    }

    @Test
    public void testOpenAIRealTaskInEnvironment() {
        String taskName = "Task2";
        var workload = new OpenAIWorkload();
        workload.setUp(query,"");
        Task task = new ExperimentTask(taskName, modelName, "Dummy", query, "", workload);
        Environment.run(task);
        assertTrue(SmartLogger.isFileInCache(task.getFileName()));
        SmartLogger.removeFileFromCache(task.getFileName());
    }

    @Test
    public void testMultipleTaskInEnvironment() {
        List<String> taskNames = List.of("Task3", "Task4", "Task5");
        for (String taskName : taskNames) {
            Task task = new ExperimentTask(taskName, modelName, "Dummy", query, "",() -> {
                OllamaBridge bridge = new OllamaBridge(modelName);
                String response = bridge.ask(query,"");
                SmartLogger.log(query + ", " + response);
            });
            Environment.run(task);
            assertTrue(SmartLogger.isFileInCache(task.getFileName()));
            SmartLogger.removeFileFromCache(task.getFileName());
        }
    }
}
