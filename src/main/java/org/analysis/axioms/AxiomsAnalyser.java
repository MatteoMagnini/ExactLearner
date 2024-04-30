package org.analysis.axioms;

import org.experiments.Result;
import org.exactlearner.engine.ELEngine;
import org.exactlearner.parser.OWLParserImpl;
import org.configurations.Configuration;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.utility.YAMLConfigLoader;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.util.HashSet;
import java.util.Set;

import static org.utility.OntologyManipulator.filterUnusedAxioms;
import static org.utility.OntologyManipulator.getOntologyShortName;

public class AxiomsAnalyser {

    public static void main(String[] args) {
        // Read the configuration file passed by the user as an argument
        var config = new YAMLConfigLoader().getConfig(args[0], Configuration.class);

        // For each model in the configuration file and for each ontology in the configuration file, run the experiment
        SmartLogger.checkCachedFiles();
        for (String model : config.getModels()) {
            for (String ontology : config.getOntologies()) {
                System.out.println("Analysing experiment for model: " + model + " and ontology: " + ontology);
                runAnalysis(model, ontology, config.getSystem(), config.getType());
            }
        }
    }

    private static void runAnalysis(String model, String ontology, String system, String type) {
        Set<OWLAxiom> trueAxioms = new HashSet<>();
        Set<OWLAxiom> falseAxioms = new HashSet<>();
        Set<OWLAxiom> unknownAxioms = new HashSet<>();
        Set<OWLAxiom> logicInconsistentAxioms = new HashSet<>();
        var parser = new OWLParserImpl(ontology);
        int trueCounter;
        int falseCounter;
        int unknownCounter;
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
        if (ontology.contains("inferred")) {
            //using true axiom based ontology to compute false and unknown axioms, if there are entailed, add them to true axioms
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology resultedOntology;
            try {
                resultedOntology = manager.createOntology(trueAxioms);
            } catch (OWLOntologyCreationException e) {
                throw new RuntimeException(e);
            }
            ELEngine engine = new ELEngine(resultedOntology);
            unknownAxioms.forEach(axiom -> {
                if (engine.entailed(axiom)) {
                    System.out.println(new ManchesterOWLSyntaxOWLObjectRendererImpl().render(axiom) + ", was unknown but is entailed, adding to true axioms");
                    logicInconsistentAxioms.add(axiom);
                }
            });
            falseAxioms.forEach(axiom -> {
                if (engine.entailed(axiom)) {
                    System.out.println(new ManchesterOWLSyntaxOWLObjectRendererImpl().render(axiom) + ", was false but is entailed, adding to true axioms");
                    logicInconsistentAxioms.add(axiom);
                }
            });
        }

        trueCounter = trueAxioms.size();
        falseCounter = falseAxioms.size();
        unknownCounter = unknownAxioms.size();
        // Save results to file
        String resultFileName = getOntologyShortName(model, ontology);

        SmartLogger.disableFileLogging();
        SmartLogger.enableFileLogging(resultFileName, false);
        if (ontology.contains("inferred")) {
            SmartLogger.log("True, False, Unknown, Logic Inconsistent Axioms\n");
            SmartLogger.log(trueCounter + ", " + falseCounter + ", " + unknownCounter + ", " + logicInconsistentAxioms.size());
        } else {
            SmartLogger.log("True, False, Unknown\n");
            SmartLogger.log(trueCounter + ", " + falseCounter + ", " + unknownCounter);
        }
        SmartLogger.disableFileLogging();
    }
}
