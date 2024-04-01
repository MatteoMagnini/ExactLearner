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

    final static String ollamaModelName = "mixtral";
    final static String openAIModelName = "gpt-3.5-turbo";
    final static String query = "Give me a number between 1 and 10. Reply with just the number!";

    @Test
    public void testSimpleTaskInEnvironment() throws IOException {
        String taskName = "Task1";
        Task task = new ExperimentTask(taskName, ollamaModelName, "Dummy", "", "",() -> {
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
        var workload = new OllamaWorkload(ollamaModelName, "", query, 10);
        Task task = new ExperimentTask(taskName, ollamaModelName, "Dummy", query, "",workload);
        Environment.run(task);
        assertTrue(SmartLogger.isFileInCache(task.getFileName()));
        SmartLogger.removeFileFromCache(task.getFileName());
    }

    @Test
    public void testOpenAIRealTaskInEnvironment() {
        String taskName = "Task2";
        var workload = new OpenAIWorkload(openAIModelName, "user", query, 30);
        Task task = new ExperimentTask(taskName, openAIModelName, "Dummy", query, "user", workload);
        Environment.run(task);
        assertTrue(SmartLogger.isFileInCache(task.getFileName()));
        SmartLogger.removeFileFromCache(task.getFileName());
    }

    @Test
    public void testMultipleTaskInEnvironment() {
        List<String> taskNames = List.of("Task3", "Task4", "Task5");
        for (String taskName : taskNames) {
            Task task = new ExperimentTask(taskName, ollamaModelName, "Dummy", query, "",() -> {
                OllamaBridge bridge = new OllamaBridge(ollamaModelName);
                String response = bridge.ask(query,"");
                SmartLogger.log(query + ", " + response);
            });
            Environment.run(task);
            assertTrue(SmartLogger.isFileInCache(task.getFileName()));
            SmartLogger.removeFileFromCache(task.getFileName());
        }
    }
}
