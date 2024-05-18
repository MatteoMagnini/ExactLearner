package org.pac;

import org.exactlearner.parser.OWLParserImpl;
import org.configurations.Configuration;
import org.experiments.Environment;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.utility.YAMLConfigLoader;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;

import java.util.stream.Collectors;

public class AskStatement {

    public static void main(String[] args) {

        new AskStatement(args[0]);
    }

    public AskStatement(String YMLFilePath) {
        var config = new YAMLConfigLoader().getConfig(YMLFilePath, Configuration.class);

        SmartLogger.checkCachedFiles();
        for(int seed = 1; seed <= 30; seed++){
            for (String model : config.getModels()) {
                for (String ontology : config.getOntologies()) {
                    System.out.println("Seed: " + seed+". Asking statements for model: " + model + " and ontology: " + ontology);
                    askStatement(seed,model, ontology, config.getSystem(), config.getMaxTokens(), config.getType());
                }
            }
        }

    }

    private void askStatement(Integer seed, String model, String ontology, String system, int maxTokens, String type) {
        var parser = new OWLParserImpl(ontology, OWLManager.createOWLOntologyManager());
        var builder = new StatementBuilderImpl(seed,parser.getClassesNamesAsString(), parser.getObjectProperties().stream().map(Object::toString).map(s -> s.split("#")[1].replace(">", "")).collect(Collectors.toSet()));
        //epsilon and gamma = 0.01
        Pac pac = new Pac(builder.getNumberOfStatements(), 0.05, 0.1, 2, builder.getAllStatements(), seed);
        for (int i = 1; i <= pac.getNumberOfExamples(); i++) {
            System.out.println("Training samples done: " + i + "/" + pac.getNumberOfExamples());
            Runnable work;
            var s = builder.chooseRandomStatement();
            if (s.isEmpty()) {
                throw new IllegalStateException("No statement available.");
            }
            if (OllamaWorkload.supportedModels.contains(model)) {
                work = new OllamaWorkload(model, system, s.get(), maxTokens);
            } else if (OpenAIWorkload.supportedModels.contains(model)) {
                work = new OpenAIWorkload(model, system, s.get(), maxTokens);
            } else {
                throw new IllegalStateException("Invalid model.");
            }
            Task task = new ExperimentTask(type, model, ontology, s.get(), system, work);
            Environment.run(task);
        }
    }
}
