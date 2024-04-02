package org.analysis;

import org.exactlearner.engine.ELEngine;
import org.exactlearner.parser.OWLParser;
import org.exactlearner.parser.OWLParserImpl;
import org.experiments.Configuration;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.yaml.snakeyaml.Yaml;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AxiomsAnalyser {

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

        Set<OWLAxiom> trueAxioms = new HashSet<>();
        Set<OWLAxiom> falseAxioms = new HashSet<>();
        Set<OWLAxiom> unknownAxioms = new HashSet<>();
        var parser = loadOntology(ontology);
        int trueCounter = 0;
        int falseCounter = 0;
        int unknownCounter = 0;
        if (type.equals("axiomsQuerying")) {
            var axioms = parser.getAxioms();
            for (OWLAxiom axiom : filterUnusedAxioms(axioms)) {
                // Remove carriage return and line feed characters
                var stringAxiom = new ManchesterOWLSyntaxOWLObjectRendererImpl().render(axiom).replaceAll("\r", " ").replaceAll("\n", " ");
                // load result
                String fileName = new ExperimentTask("axiomsQuerying", model, ontology, stringAxiom, system, () -> {
                }).getFileName();
                Result result = new Result(fileName);
                if (result.isTrue()) {
                    trueAxioms.add(axiom);
                } else if (result.isFalse()) {
                    falseAxioms.add(axiom);
                } else {
                    unknownAxioms.add(axiom);
                }
            }
        } else {
            throw new IllegalStateException("Invalid type of experiment.");
        }

        //using true axiom based ontology to compute false and unknown axioms, if there are entailed, add them to true axioms
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology resultedOntology = null;
        try {
            resultedOntology = manager.createOntology(trueAxioms);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        ELEngine engine = new ELEngine(resultedOntology);
        unknownAxioms.forEach(axiom -> {
            if (engine.entailed(axiom)) {
                System.out.println(new ManchesterOWLSyntaxOWLObjectRendererImpl().render(axiom) + ", was unknown but is entailed, adding to true axioms");
                trueAxioms.add(axiom);
            }
        });
        falseAxioms.forEach(axiom -> {
            if (engine.entailed(axiom)) {
                System.out.println(new ManchesterOWLSyntaxOWLObjectRendererImpl().render(axiom) + ", was false but is entailed, adding to true axioms");
                trueAxioms.add(axiom);
            }
        });
        unknownAxioms.removeAll(trueAxioms);
        falseAxioms.removeAll(trueAxioms);

        trueCounter = trueAxioms.size();
        falseCounter = falseAxioms.size();
        unknownCounter = unknownAxioms.size();
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

    private static Set<OWLAxiom> filterUnusedAxioms(Set<OWLAxiom> axioms) {
        return axioms.stream().filter(axiom -> axiom.isOfType(AxiomType.SUBCLASS_OF)
                || axiom.isOfType(AxiomType.EQUIVALENT_CLASSES)
                || axiom.isOfType(AxiomType.SUB_OBJECT_PROPERTY)
                || axiom.isOfType(AxiomType.EQUIVALENT_OBJECT_PROPERTIES)
                || axiom.isOfType(AxiomType.OBJECT_PROPERTY_DOMAIN)
                || axiom.isOfType(AxiomType.DISJOINT_CLASSES))
                .collect(Collectors.toSet());
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
