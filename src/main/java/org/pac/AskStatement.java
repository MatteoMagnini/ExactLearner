package org.pac;

import org.experiments.Configuration;
import org.experiments.Environment;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.utility.OntologyLoader;
import org.experiments.utility.YAMLConfigLoader;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;

import java.util.stream.Collectors;

public class AskStatement {

    public static void main(String[] args) {
        AskStatement ask = new AskStatement("src/main/java/org/pac/statementsQueryingConf.yml");
    }

    public AskStatement(String YMLFilePath) {
        var config = new YAMLConfigLoader().getConfig(YMLFilePath, Configuration.class);

        for (String model : config.getModels()) {
            for (String ontology : config.getOntologies()) {
                System.out.println("Asking statements for model: " + model + " and ontology: " + ontology);
                askStatement(model, ontology, config.getSystem(), config.getMaxTokens(), config.getType());
            }
        }
    }

    private void askStatement(String model, String ontology, String system, int maxTokens, String type) {
        var parser = new OntologyLoader().getParser(ontology);
        var builder = new StatementBuilderImpl(parser.getClassesNamesAsString(), parser.getObjectProperties().stream().map(Object::toString).map(s -> s.split("#")[1].replace(">", "")).collect(Collectors.toSet()));

        builder.getAllStatements().forEach(s -> {
            Runnable work = null;
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
