package org.analysis.exp2;

import org.analysis.common.Metrics;
import org.configurations.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.utility.YAMLConfigLoader;

import java.io.File;
import java.nio.file.FileSystems;

public class ResultAnalyzer {
    private OWLOntology predictedOntology;
    private OWLOntology expectedOntology;
    private final String fileSeparator = FileSystems.getDefault().getSeparator();
    private final Configuration config;

    public ResultAnalyzer(String[] args) {
        this.config = new YAMLConfigLoader().getConfig(args[0], Configuration.class);
    }

    public void run() {
        for (var model : config.getModels()) {
            for (var onto : config.getOntologies()) {
                readOntologies(model, onto);
                compareOntologies();
            }
        }
    }

    private void compareOntologies() {
        ///COMPARE T with H

        //CONFUSION MATRIX
        //      PREDICTED
        // E         T   F
        // X     T   TP  FN
        // P     F   FP  TN
        var expectedAxioms = expectedOntology.getLogicalAxioms();
        var predictedAxioms = predictedOntology.getLogicalAxioms();
        var expectedClasses = expectedOntology.getClassesInSignature();
        var predictedClasses = predictedOntology.getClassesInSignature();
        int[][] confusionMatrix = new int[3][3];
        var expectedReasoner = new Reasoner(expectedOntology);
        var predictedReasoner = new Reasoner(predictedOntology);

        for (var ax : predictedAxioms) {
            if (expectedReasoner.isEntailed(ax)) confusionMatrix[0][0]++;
            else confusionMatrix[0][1]++;
        }
        for (var cls : predictedClasses) {
            if (expectedClasses.contains(cls)) confusionMatrix[0][0]++;
            else confusionMatrix[0][1]++;
        }

        for (var ax : expectedAxioms) {
            if (predictedReasoner.isEntailed(ax)) confusionMatrix[0][0]++;
            else confusionMatrix[1][0]++;
        }
        for (var cls : expectedClasses) {
            if (predictedClasses.contains(cls)) confusionMatrix[0][0]++;
            else confusionMatrix[1][0]++;
        }
        System.out.println("Ontology "+ expectedOntology.getOntologyID().getOntologyIRI());
        System.out.println("RECALL:" + Metrics.calculateRecall(confusionMatrix));
        System.out.println("PRECISION:" + Metrics.calculatePrecision(confusionMatrix));
        System.out.println("##############################################################");
    }

    private void readOntologies(String model, String onto) {
        int lastSlashIndex = onto.lastIndexOf('/');
        int extensionIndex = onto.lastIndexOf(".owl");
        String name = "";

        if (lastSlashIndex != -1 && extensionIndex != -1) {
            name = onto.substring(lastSlashIndex + 1, extensionIndex) + ".owl";
        } else {
            System.out.println("Could not get ontology name. Exiting...");
            System.exit(1);
        }
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            expectedOntology = manager.loadOntologyFromOntologyDocument(new File("results" + fileSeparator + "ontologies" + fileSeparator + "target_" + name));
            predictedOntology = manager.loadOntologyFromOntologyDocument(new File("results" + fileSeparator + "ontologies" + fileSeparator + "learned_" + model + "_" + name));
        } catch (OWLOntologyCreationException e) {
            System.out.println("ERROR IN READING OWL FILE; CHECK IF LLM LEARNER THROWS SOME ERRORS!!!");
            throw new RuntimeException(e);
        }

    }
}
