package org.experiments;

import org.analysis.OntologyManipulator;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.utility.SHA256Hash;
import org.experiments.utility.YAMLConfigLoader;
import org.experiments.workload.OllamaWorkload;
import org.experiments.workload.OpenAIWorkload;
import org.semanticweb.owlapi.model.OWLAxiom;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

public class Launch {

    public static void main(String[] args) {
        // Read the configuration file passed by the user as an argument
        var config = new YAMLConfigLoader().getConfig(args[0], Configuration.class);
        // For each model in the configuration file and for each ontology in the configuration file, run the experiment
        SmartLogger.checkCachedFiles();
        for (String model : config.getModels()) {
            for (String ontology : config.getOntologies()) {
                System.out.println("Running experiment for model: " + model + " and ontology: " + ontology);
                runExperiment(model, ontology, config.getSystem(), config.getMaxTokens(), config.getType());
            }
        }
    }

    private static void runExperiment(String model, String ontology, String system, int maxTokens, String type) {
        var parser = OntologyManipulator.getParser(ontology);
        var classesNames = parser.getClassesNamesAsString();
        var axioms = parser.getAxioms();
        var filteredManchesterSyntaxAxioms = parseAxioms(axioms);

        if (type.equals("classesQuerying")) {
            for (String className : classesNames) {
                for (String className2 : classesNames) {
                    String message = className + " SubClassOf " + className2;
                    //queries.put(new Pair<>(model, ontology), message);
                    runModel(model, ontology, system, maxTokens, type, message);
                }
            }
        } else if (type.equals("axiomsQuerying")) {
            for (String axiom : filteredManchesterSyntaxAxioms) {
                // Remove carriage return and line feed characters
                axiom = axiom.replaceAll("\r", " ").replaceAll("\n", " ");
                runModel(model, ontology, system, maxTokens, type, axiom);
            }
        } else {
            throw new IllegalStateException("Invalid type of experiment.");
        }
    }

    private static void runModel(String model, String ontology, String system, int maxTokens, String type, String message) {

        Runnable work = null;
        if (OllamaWorkload.supportedModels.contains(model)) {
            work = new OllamaWorkload(model, system, message, maxTokens);
        } else if (OpenAIWorkload.supportedModels.contains(model)) {
            work = new OpenAIWorkload(model, system, message, maxTokens);
        } else {
            throw new IllegalStateException("Invalid model.");
        }
        Task task = new ExperimentTask(type, model, ontology, message, system, work);
        Environment.run(task);
        //moveFile(type, model, ontology, message, system);
    }

    private static void moveFile(String type, String model, String ontology, String message, String system) {
        File f = new File("cache/" + SHA256Hash.sha256(type + model + ontology + message + system) + ".csv");
        var ontName = ontology.split("/")[5].replace(".owl", "");
        if (!new File("cache/" + type + "-" + model + "-" + ontName).exists())
            new File("cache/" + type + "-" + model + "-" + ontName).mkdir();
        f.renameTo(new File("cache/" + type + "-" + model + "-" + ontName + "/" + f.getName()));
    }

    private static Set<String> parseAxioms(Set<OWLAxiom> axioms) {
        return axioms.stream().map(new ManchesterOWLSyntaxOWLObjectRendererImpl()::render).collect(Collectors.toSet());
    }
}