package org.analysis.classes;

import org.experiments.Result;
import org.exactlearner.engine.ELEngine;
import org.exactlearner.parser.OWLParserImpl;
import org.configurations.Configuration;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.utility.YAMLConfigLoader;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Set;

import static org.analysis.common.Metrics.*;
import static org.utility.OntologyManipulator.*;

public class ClassesAnalyser {

    public static void main(String[] args) {
        // Read the configuration file passed by the user as an argument
        var config = new YAMLConfigLoader().getConfig(args[0], Configuration.class);
        // For each model in the configuration file and for each ontology in the configuration file, run the experiment
        SmartLogger.checkCachedFiles();
        for (String model : config.getModels()) {
            for (String ontology : config.getOntologies()) {
                System.out.println("Analysing experiment for model: " + model + " and ontology: " + ontology);
                runExperiment3(model, ontology, config.getSystem());
                //runAnalysis(model, ontology, config.getSystem(), config.getType());
            }
        }
    }

    private static void runExperiment3(String model, String ontology, String system) {
        var parser = new OWLParserImpl(ontology, OWLManager.createOWLOntologyManager());
        var classesNames = parser.getClassesNamesAsString();
        var confusionMatrix = createConfusionMatrix(classesNames, model, ontology, system);
        // Calculate metrics
        double accuracy = calculateAccuracy(confusionMatrix);
        double f1Score = calculateF1Score(confusionMatrix);
        double precision = calculatePrecision(confusionMatrix);
        double recall = calculateRecall(confusionMatrix);
        double logLoss = calculateLogLoss(confusionMatrix);
        double matthewsCorrelationCoefficient = calculateMatthewsCorrelationCoefficient(confusionMatrix);

        // Print results
        System.out.println("Accuracy: " + accuracy);
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F1 Score: " + f1Score);
        System.out.println("Log Loss: " + logLoss);
        System.out.println("Matthews MCC: " + matthewsCorrelationCoefficient);
        for (int[] matrix : confusionMatrix) {
            for (int i : matrix) {
                System.out.print(i + " ");
            }
            System.out.println();
        }

        // Save results to file
        String separator = FileSystems.getDefault().getSeparator();
        // Check if the results directory exists
        if (!new File("results").exists()) {
            new File("results").mkdir();
        }
        if (!new File("results" + separator + "classesQuerying").exists()) {
            new File("results" + separator + "classesQuerying").mkdir();
        }
        String shortOntology = ontology.substring(ontology.lastIndexOf(separator) + 1);
        shortOntology = shortOntology.substring(0, shortOntology.lastIndexOf('.'));
        String resultFileName = "results" + separator + "classesQuerying" + separator + model.replace(":", "-") + '_' + shortOntology;
        SmartLogger.disableFileLogging();
        SmartLogger.enableFileLogging(resultFileName, false);
        SmartLogger.log("Accuracy; F1 Score; Precision; Recall; Log Loss; Matthews MCC\n");
        // Approximate the values to 2 decimal places
        SmartLogger.log(String.format("%.2f; %.2f; %.2f; %.2f; %.2f; %.2f\n", accuracy, f1Score, precision, recall, logLoss, matthewsCorrelationCoefficient));
        SmartLogger.disableFileLogging();
    }

    private static int[][] createConfusionMatrix(Set<String> classesNames, String model, String ontology, String system) {
        var manager = OWLManager.createOWLOntologyManager();
        var matrixCFU = new int[2][3];
        // Populate the confusion matrix with zeros
        for (int[] i : matrixCFU) {
            Arrays.fill(i, 0);
        }
        //      T   F   U
        //  T   TP  FN  FN
        //  F   FP  TN  TN
        ELEngine engine;
        OWLOntology owl;
        try {
            owl = manager.loadOntologyFromOntologyDocument(new File(ontology));
            engine = new ELEngine(owl);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        var classesArray = classesNames.stream().filter(s -> !s.contains("Thin")).toList();
        for (String className1 : classesArray) {
            for (String className2 : classesArray) {
                String message = className1 + " SubClassOf " + className2;
                //queries.put(new Pair<>(model, ontology), message);
                String fileName = new ExperimentTask("classesQuerying", model, ontology, message, system, () -> {
                }).getFileName();
                Result result = null;
                result = new Result(fileName);
                if (result.isTrue()) {
                    if (engine.entailed(createAxiomFromString(message, owl))) {
                        matrixCFU[0][0]++;
                    } else {
                        matrixCFU[1][0]++;
                    }
                } else if (result.isFalse()) {
                    if (engine.entailed(createAxiomFromString(message, owl))) {
                        matrixCFU[0][1]++;
                    } else {
                        matrixCFU[1][1]++;
                    }
                } else {
                    if (engine.entailed(createAxiomFromString(message, owl))) {
                        matrixCFU[0][2]++;
                    } else {
                        matrixCFU[1][2]++;
                    }
                }
            }
        }
        return matrixCFU;
    }
}