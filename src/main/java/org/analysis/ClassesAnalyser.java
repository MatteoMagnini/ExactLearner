package org.analysis;

import org.apache.jena.atlas.lib.Pair;
import org.exactlearner.parser.OWLParser;
import org.exactlearner.parser.OWLParserImpl;
import org.experiments.Configuration;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
                runAnalysis(model, ontology, config.getSystem(), config.getMaxTokens(), config.getType());
            }
        }
    }

    private static void runAnalysis(String model, String ontology, String system, int maxTokens, String type) {

        Set<String> trueClassesQuerying = new HashSet<>();
        Set<String> falseClassesQuerying = new HashSet<>();
        Set<String> unknownClassesQuerying = new HashSet<>();
        var parser = loadOntology(ontology);
        int trueCounter = 0;
        int falseCounter = 0;
        int unknownCounter = 0;
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
