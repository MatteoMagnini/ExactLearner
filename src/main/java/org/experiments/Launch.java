package org.experiments;

import org.exactlearner.parser.OWLParser;
import org.exactlearner.parser.OWLParserImpl;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.experiments.workload.OllamaWorkload;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.yaml.snakeyaml.Yaml;
import java.io.FileNotFoundException;
import java.util.Set;

class Launch {

    public static void main(String[] args) throws FileNotFoundException {
        // Read the configuration file
        Yaml yaml = new Yaml();
        Configuration config = yaml.load(Launch.class.getClassLoader().getResourceAsStream("config.yaml"));

        // For each model in the configuration file and for each ontology in the configuration file, run the experiment
        for (String model : config.getModels()) {
            for (String ontology : config.getOntologies()) {
                runExperiment(model, ontology, config.getSystem(), config.getMaxTokens());
            }
        }
    }

    private static void runExperiment(String model, String ontology, String system, int maxTokens) {
        var parser = loadOntology(ontology);
        var classesNames = parser.getClassesNamesAsString();
        var axiom = parser.getAxioms();
        for (String className : classesNames) {
            for (String className2 : classesNames) {
                if (!className.equals(className2)) {
                    String query = "Is " + className + " a subclass of " + className2 + "?";
                    Task task = new ExperimentTask("Experiment", model, ontology, query, system, new OllamaWorkload(model, system, query, maxTokens));
                    task.run();
                }
            }
        }
    }


    private static OWLParser loadOntology(String familyOntology) {
        OWLParser parser = null;
        try {
            parser = new OWLParserImpl(familyOntology);
        } catch (OWLOntologyCreationException e) {
            System.out.println(e.getMessage());
        }
        return parser;
    }
}