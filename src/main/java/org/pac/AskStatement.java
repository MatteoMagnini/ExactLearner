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

import java.util.stream.Collectors;

public class AskStatement {

    public static void main(String[] args) {
        AskStatement ask = new AskStatement("src/main/java/org/pac/statementsQueryingConf.yml");
    }

    public AskStatement(String YMLFilePath) {
        var config = new YAMLConfigLoader().getConfig(YMLFilePath, Configuration.class);

        SmartLogger.checkCachedFiles();
        for (String model : config.getModels()) {
            for (String ontology : config.getOntologies()) {
                System.out.println("Asking statements for model: " + model + " and ontology: " + ontology);
                askStatement(model, ontology, config.getSystem(), config.getMaxTokens(), config.getType());
            }
        }
    }

    private void askStatement(String model, String ontology, String system, int maxTokens, String type) {

        var parser = OntologyManipulator.getParser(ontology);
        var builder = new StatementBuilderImpl(parser.getClassesNamesAsString(), parser.getObjectProperties().stream().map(Object::toString).map(s -> s.split("#")[1].replace(">", "")).collect(Collectors.toSet()));
        //epsilon and gamma = 0.01
        Pac pac = new Pac(builder.getNumberOfStatements(), 0.01, 0.01);
        for (int i = 1; i <= pac.getTrainingSamples(); i++) {
            //System.out.println("Training samples done: " + i + "/" + pac.getTrainingSamples());
            Runnable work;
            var s = builder.chooseRandomStatement();
            if (OllamaWorkload.supportedModels.contains(model)) {
                work = new OllamaWorkload(model, system, s, maxTokens);
            } else if (OpenAIWorkload.supportedModels.contains(model)) {
                work = new OpenAIWorkload(model, system, s, maxTokens);
            } else {
                throw new IllegalStateException("Invalid model.");
            }
            Task task = new ExperimentTask(type, model, ontology, s, system, work);
            Environment.run(task);
        }
    }
}
