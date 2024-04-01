package org.analysis;

import org.exactlearner.parser.OWLParser;
import org.exactlearner.parser.OWLParserImpl;
import org.experiments.Configuration;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.yaml.snakeyaml.Yaml;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.stream.Collectors;

public class AxiomsAnalyser{

    public static void main(String[] args) {
        // Read the configuration file passed by the user as an argument
        Yaml yaml = new Yaml();
        Configuration config;

        try {
            config = yaml.loadAs(new FileInputStream(args[0]), Configuration.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // For each model in the configuration file and for each ontology in the configuration file, run the experiment
        SmartLogger.checkCachedFiles();
        for (String model : config.getModels()) {
            for (String ontology : config.getOntologies()) {
                System.out.println("Analysing experiment for model: " + model + " and ontology: " + ontology);
                runAnalysis(model, ontology, config.getSystem(), config.getMaxTokens(), config.getType());
            }
        }
    }

    private static void runAnalysis(String model, String ontology, String system, int maxTokens, String type) {
        var parser = loadOntology(ontology);
        int trueCounter = 0;
        int falseCounter = 0;
        int unknownCounter = 0;

        if (type.equals("axiomsQuerying")) {
            var axioms = parser.getAxioms();
            var filteredManchesterSyntaxAxioms = parseAxioms(axioms);
            for (String axiom : filteredManchesterSyntaxAxioms) {
                // Remove carriage return and line feed characters
                axiom = axiom.replaceAll("\r", " ").replaceAll("\n", " ");
                // load result
                String fileName = new ExperimentTask("axiomsQuerying", model, ontology, axiom, system, () -> {}).getFileName();
                Result result = new Result(fileName);
                if (result.isTrue()) {
                    trueCounter++;
                } else if (result.isFalse()) {
                    falseCounter++;
                } else {
                    unknownCounter++;
                }
            }
        } else {
            throw new IllegalStateException("Invalid type of experiment.");
        }

        // Save results to file
        String separator = System.getProperty("file.separator");
        // Check if the results directory exists
        if (!new File("results").exists()) {
            new File("results").mkdir();
        }
        if (!new File("results" + separator + "axiomsQuerying").exists()) {
            new File("results" + separator + "axiomsQuerying").mkdir();
        }
        String shortOntology = ontology.substring(ontology.lastIndexOf(separator) + 1);
        shortOntology = shortOntology.substring(0, shortOntology.lastIndexOf('.'));
        String resultFileName = "results" + separator + "axiomsQuerying" + separator + model + '_' + shortOntology;
        SmartLogger.disableFileLogging();
        SmartLogger.enableFileLogging(resultFileName, false);
        SmartLogger.log("True, False, Unknown\n");
        SmartLogger.log(trueCounter + ", " + falseCounter + ", " + unknownCounter);
        SmartLogger.disableFileLogging();
    }

    private static Set<String> parseAxioms(Set<OWLAxiom> axioms) {
        return axioms.stream().filter(axiom -> !axiom.isOfType(AxiomType.DECLARATION))
                .filter(axiom -> !axiom.isOfType(AxiomType.FUNCTIONAL_OBJECT_PROPERTY))
                .filter(axiom -> !axiom.isOfType(AxiomType.SYMMETRIC_OBJECT_PROPERTY))
                .filter(axiom -> !axiom.isOfType(AxiomType.CLASS_ASSERTION)).collect(Collectors.toSet())
                .stream().map(new ManchesterOWLSyntaxOWLObjectRendererImpl()::render).collect(Collectors.toSet());
    }

    private static OWLParser loadOntology(String ontology) {
        OWLParser parser = null;
        try {
            parser = new OWLParserImpl(ontology);
        } catch (OWLOntologyCreationException e) {
            System.out.println(e.getMessage());
        }
        return parser;
    }
}
