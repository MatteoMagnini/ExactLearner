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
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.yaml.snakeyaml.Yaml;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
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
                //runAnalysis(model, ontology, config.getSystem(), config.getMaxTokens(), config.getType());
            }
        }
    }

    private static void runExperiment3(String model, String ontology, String system) {
        var parser = loadOntology(ontology);
        var classesNames = parser.getClassesNamesAsString();
        var confusionMatrixTFU = createConfusionMatrix(classesNames, model, ontology, system);
        System.out.println(Arrays.deepToString(confusionMatrixTFU));

    }

    private static void runAnalysis(String model, String ontology, String system, int maxTokens, String type) {

        Set<String> trueClassesQuerying = new HashSet<>();
        Set<String> falseClassesQuerying = new HashSet<>();
        Set<String> unknownClassesQuerying = new HashSet<>();

        int trueCounter = 0;
        int falseCounter = 0;
        int unknownCounter = 0;
        var parser = loadOntology(ontology);
        var classesNames = parser.getClassesNamesAsString();

        if (type.equals("classesQuerying")) {
            for (String className : classesNames) {
                for (String className2 : classesNames) {
                    if (!className.equals(className2)) {
                        String message = className + " SubClassOf " + className2;
                        //queries.put(new Pair<>(model, ontology), message);
                        String fileName = new ExperimentTask("classesQuerying", model, ontology, message, system, () -> {
                        }).getFileName();
                        Result result = new Result(fileName);
                        if (result.isTrue()) {
                            trueClassesQuerying.add(message);
                        } else if (result.isFalse()) {
                            falseClassesQuerying.add(message);
                        } else {
                            unknownClassesQuerying.add(message);
                        }
                    }
                }
            }
        } else {
            throw new IllegalStateException("Invalid type of experiment.");
        }

        if (!falseClassesQuerying.isEmpty() && !unknownClassesQuerying.isEmpty()) {
            falseClassesQuerying.addAll(unknownClassesQuerying);

            trueClassesQuerying.addAll(checkClosure(trueClassesQuerying, falseClassesQuerying));

            falseClassesQuerying.removeAll(trueClassesQuerying);
            falseClassesQuerying.removeAll(unknownClassesQuerying);
            unknownClassesQuerying.removeAll(trueClassesQuerying);
        }

        trueCounter = trueClassesQuerying.size();
        falseCounter = falseClassesQuerying.size();
        unknownCounter = unknownClassesQuerying.size();
        // Save results to file
        String separator = System.getProperty("file.separator");
        // Check if the results directory exists
        if (!new File("results").exists()) {
            new File("results").mkdir();
        }
        if (!new File("results" + separator + "classesQuerying").exists()) {
            new File("results" + separator + "classesQuerying").mkdir();
        }
        String shortOntology = ontology.substring(ontology.lastIndexOf(separator) + 1);
        shortOntology = shortOntology.substring(0, shortOntology.lastIndexOf('.'));
        String resultFileName = "results" + separator + "classesQuerying" + separator + model + '_' + shortOntology;
        SmartLogger.disableFileLogging();
        SmartLogger.enableFileLogging(resultFileName, false);
        SmartLogger.log("True, False, Unknown\n");
        SmartLogger.log(trueCounter + ", " + falseCounter + ", " + unknownCounter);
        SmartLogger.disableFileLogging();
    }

    private static int[][] createConfusionMatrix(Set<String> classesNames, String model, String ontology, String system) {
        var manager = OWLManager.createOWLOntologyManager();
        var matrixCFU = new int[3][3];
        // Populate the confusion matrix with zeros
        for (int[] i : matrixCFU) {
            Arrays.fill(i, 0);
        }
        //      T   F   U
        //  T   TT
        //  F       FF
        //  U           UU
        ELEngine engine;
        OWLOntology owl;
        try {
            owl = manager.loadOntologyFromOntologyDocument(new File(ontology));
            engine = new ELEngine(owl);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        var classesArray = classesNames.stream().filter(s -> !s.contains("owl:Thin")).toList();
        for(String className1 : classesArray){
            for (String className2 : classesArray) {
                if (className1.equals(className2)) {
                    matrixCFU[1][1]++;
                } else {
                    String message = className1 + " SubClassOf " + className2;
                    //queries.put(new Pair<>(model, ontology), message);
                    String fileName = new ExperimentTask("classesQuerying", model, ontology, message, system, () -> {
                    }).getFileName();
                    Result result=result = new Result(fileName);
                    if (result.isTrue()) {
                        if (engine.entailed(createAxiomFromString(message, owl))) {
                            matrixCFU[0][0]++;
                        } else {
                            matrixCFU[0][1]++;
                        }
                    } else if (result.isFalse()) {
                        if (engine.entailed(createAxiomFromString(message, owl))) {
                            matrixCFU[1][0]++;
                        } else {
                            matrixCFU[1][1]++;
                        }
                    } else {
                        if (engine.entailed(createAxiomFromString(message, owl))) {
                            matrixCFU[2][0]++;
                        } else {
                            matrixCFU[2][1]++;
                        }
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
