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
    private final String model;
    private final String onto;
    private String engine="";
    public ResultAnalyzer(String model, String onto, Integer i) {
        this.model = model;
        this.onto = onto;
        switch (i) {
            case 1:
                this.engine = "manchester_";
                break;
            case 2:
                this.engine = "enriched_manchester_";
                break;
            case 3:
                this.engine = "nlp_";
                break;
            default:
                System.out.println("Invalid engine. Exiting...");
                System.exit(1);
        }

    }

    public void run() {
        readOntologies(model, onto);
        compareOntologies();
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

        for (var ax : expectedAxioms) {
            if (predictedReasoner.isEntailed(ax)) confusionMatrix[0][0]++;
            else confusionMatrix[1][0]++;
        }

        System.out.println("Ontology predicted:"+ predictedOntology.toString());
        System.out.println("Model "+ model);
        System.out.println("RECALL:" + Metrics.calculateRecall(confusionMatrix));
        System.out.println("PRECISION:" + Metrics.calculatePrecision(confusionMatrix));
        System.out.println("F1-Score:" + Metrics.calculateF1Score(confusionMatrix));
        System.out.println("##############################################################");
    }

    private void readOntologies(String model, String onto) {
        int lastSlashIndex = onto.lastIndexOf('/');
        int extensionIndex = onto.lastIndexOf(".owl");
        String name = "";

        if (lastSlashIndex != -1 && extensionIndex != -1) {
            name = onto.substring(lastSlashIndex + 1, extensionIndex) + ".owl";
            name = name.replaceAll("\\(.*\\)", "");
        } else {
            System.out.println("Could not get ontology name. Exiting...");
            System.exit(1);
        }
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            expectedOntology = manager.loadOntologyFromOntologyDocument(new File("results" + fileSeparator + "ontologies" + fileSeparator + "target_" + name));
            predictedOntology = manager.loadOntologyFromOntologyDocument(new File("results" + fileSeparator + "ontologies" + fileSeparator + engine +"learned_" + model.replace(":","-") + "_" + name));
        } catch (OWLOntologyCreationException e) {
            System.out.println("ERROR IN READING OWL FILE; CHECK IF LLM LEARNER THROWS SOME ERRORS!!!");
            throw new RuntimeException(e);
        }

    }
}
