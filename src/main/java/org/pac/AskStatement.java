package org.pac;

import org.analysis.OntologyManipulator;
import org.experiments.Configuration;
import org.experiments.Environment;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.utility.YAMLConfigLoader;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AskStatement {

    public static void main(String[] args) {
        AskStatement ask = new AskStatement("src/main/java/org/pac/statementsQueryingConf.yml");
    }

    public AskStatement(String YMLFilePath) {
        var config = new YAMLConfigLoader().getConfig(YMLFilePath, Configuration.class);
        SmartLogger.checkCachedFiles();
        var cpus = Runtime.getRuntime().availableProcessors();//THIS COUNT LOGIC CORES NOT PHYSICAL CORES
        ExecutorService executorService = Executors.newFixedThreadPool(cpus/2-1);
        for (String model : config.getModels()) {
            executorService.execute(() -> {
                for (String ontology : config.getOntologies()) {
                    executorService.execute(() -> {
                        System.out.println("Asking statements for model: " + model + " and ontology: " + ontology);
                        askStatement(model, ontology, config.getSystem(), config.getMaxTokens(), config.getType());
                    });
                }
            });
        }
    }

    private void askStatement(String model, String ontology, String system, int maxTokens, String type) {
        var parser = OntologyManipulator.getParser(ontology);
        var builder = new StatementBuilderImpl(parser.getClassesNamesAsString(), parser.getObjectProperties().stream().map(Object::toString).map(s -> s.split("#")[1].replace(">", "")).collect(Collectors.toSet()));

        builder.getAllStatements().forEach(s -> {
            Runnable work;
            if (OllamaWorkload.supportedModels.contains(model)) {
                work = new OllamaWorkload(model, system, s, maxTokens);
            } else if (OpenAIWorkload.supportedModels.contains(model)) {
                work = new OpenAIWorkload(model, system, s, maxTokens);
            } else {
                throw new IllegalStateException("Invalid model.");
            }
            Task task = new ExperimentTask(type, model, ontology, s, system, work);
            Environment.run(task);
        });

    }
}
