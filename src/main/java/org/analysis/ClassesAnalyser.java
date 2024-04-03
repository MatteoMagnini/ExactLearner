package org.analysis;

import org.apache.jena.atlas.lib.Pair;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.exactlearner.engine.ELEngine;
import org.exactlearner.parser.OWLParser;
import org.exactlearner.parser.OWLParserImpl;
import org.experiments.Configuration;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.yaml.snakeyaml.Yaml;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassesAnalyser {

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
                runExperiment3(model, ontology, config.getSystem());
                //runAnalysis(model, ontology, config.getSystem(), config.getType());
            }
        }
    }

    private static void runExperiment3(String model, String ontology, String system) {
        var parser = loadOntology(ontology);
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
        for (int i = 0; i < confusionMatrix.length; i++) {
            for (int j = 0; j < confusionMatrix[i].length; j++) {
                System.out.print(confusionMatrix[i][j] + " ");
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
        String resultFileName = "results" + separator + "classesQuerying" + separator + model.replace(":","-") + '_' + shortOntology;
        SmartLogger.disableFileLogging();
        SmartLogger.enableFileLogging(resultFileName, false);
        SmartLogger.log("Accuracy; F1 Score; Precision; Recall; Log Loss; Matthews MCC\n");
        // Approximate the values to 2 decimal places
        SmartLogger.log(String.format("%.2f; %.2f; %.2f; %.2f; %.2f; %.2f\n", accuracy, f1Score, precision, recall, logLoss, matthewsCorrelationCoefficient));
        SmartLogger.disableFileLogging();
    }

    public static double calculateAccuracy(int[][] confusionMatrix) {
        int total = Arrays.stream(confusionMatrix).flatMapToInt(Arrays::stream).sum();
        int correct = 0;
        for (int i = 0; i < confusionMatrix.length; i++) {
            correct += confusionMatrix[i][i];
        }
        return (double) correct / total;
    }

    public static double calculatePrecision(int[][] confusionMatrix) {
        int tp = confusionMatrix[0][0];
        int fp = confusionMatrix[1][0];
        return (double) tp / (tp + fp);
    }

    public static double calculateRecall(int[][] confusionMatrix) {
        int tp = confusionMatrix[0][0];
        int fn = confusionMatrix[0][1] + confusionMatrix[0][2];
        return (double) tp / (tp + fn);
    }

    public static double calculateF1Score(int[][] confusionMatrix) {
        double precision = calculatePrecision(confusionMatrix);
        double recall = calculateRecall(confusionMatrix);
        if (precision + recall == 0) {
            return 0.0; // Avoid division by zero
        } else {
            return 2.0 * (precision * recall) / (precision + recall);
        }
    }

    public static double calculateLogLoss(int[][] confusionMatrix) {
        double sum = 0;
        int total = Arrays.stream(confusionMatrix).flatMapToInt(Arrays::stream).sum();
        for (int[] matrix : confusionMatrix) {
            for (int i : matrix) {
                double prob = (double) i / total;
                sum += i == 0 ? 0 : i * Math.log(prob);
            }
        }
        return -sum / total;
    }

    public static double calculateMatthewsCorrelationCoefficient(int[][] confusionMatrix) {
        int tp = confusionMatrix[0][0];
        int tn = confusionMatrix[1][1] + confusionMatrix[1][2];
        int fp = confusionMatrix[1][0];
        int fn = confusionMatrix[0][1] + confusionMatrix[0][2];
        //      T   F   U
        //  T   TP  FN  FN
        //  F   FP  TN  TN

        double numerator = (tp * tn) - (fp * fn);
        double denominator = Math.sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn));

        if (denominator == 0) {
            return 0.0; // Avoid division by zero
        } else {
            return numerator / denominator;
        }
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
        var classesArray = classesNames.stream().filter(s -> !s.contains("owl:Thin")).toList();
        for (String className1 : classesArray) {
            for (String className2 : classesArray) {
                String message = className1 + " SubClassOf " + className2;
                //queries.put(new Pair<>(model, ontology), message);
                String fileName = new ExperimentTask("classesQuerying", model, ontology, message, system, () -> {
                }).getFileName();
                Result result = new Result(fileName);
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

    private static OWLAxiom createAxiomFromString(String query, OWLOntology ontology) {
        query = query.replace("SubClassOf", "SubClassOf:");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ManchesterOWLSyntaxEditorParser parser = getManchesterOWLSyntaxEditorParser(ontology, manager, query);
        OWLAxiom axiom = null;
        try {
            axiom = parser.parseAxiom();
        } catch (ParserException e) {
            System.err.println("Error parsing axiom: " + e.getMessage());
        }
        return axiom;
    }

    private static ManchesterOWLSyntaxEditorParser getManchesterOWLSyntaxEditorParser(OWLOntology rootOntology, OWLOntologyManager manager, String axiomResult) {
        Set<OWLOntology> importsClosure = rootOntology.getImportsClosure();
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(
                new BidirectionalShortFormProviderAdapter(manager, importsClosure,
                        new SimpleShortFormProvider()));

        ManchesterOWLSyntaxEditorParser parser = new ManchesterOWLSyntaxEditorParser(new OWLDataFactoryImpl(), axiomResult);
        parser.setDefaultOntology(rootOntology);
        parser.setOWLEntityChecker(entityChecker);
        return parser;
    }

    private static Set<String> checkClosure(Set<String> trueAnswers, Set<String> notTrueAnswers) {
        Set<Pair<String, String>> truePairs = getPair(trueAnswers);
        Set<Pair<String, String>> notTruePairs = getPair(notTrueAnswers);

        var result = notTruePairs.stream().filter(pair ->
                        truePairs.stream()
                                .anyMatch(first -> first.getLeft().equals(pair.getLeft()) &&
                                        truePairs.stream()
                                                .anyMatch(second -> second.getRight().equals(pair.getRight()) &&
                                                        first.getRight().equals(second.getLeft()))))
                .map(pair -> pair.getLeft() + " SubClassOf " + pair.getRight())
                .collect(Collectors.toSet());
        System.out.println(result);
        return result;
    }

    private static Set<Pair<String, String>> getPair(Set<String> answers) {
        try {
            return answers.stream().map(a -> {
                String[] split = a.split(" SubClassOf ");
                return new Pair<>(split[0].trim(), split[1].trim());
            }).collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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