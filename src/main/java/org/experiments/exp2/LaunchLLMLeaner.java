package org.experiments.exp2;
import org.analysis.exp2.ResultAnalyzer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.configurations.Configuration;
import org.exactlearner.engine.ELEngine;
import org.exactlearner.engine.LLMEngine;
import org.exactlearner.learner.Learner;
import org.exactlearner.oracle.Oracle;
import org.exactlearner.utils.Metrics;
import org.experiments.logger.SmartLogger;
import org.pac.Pac;
import org.pac.StatementBuilderImpl;
import org.semanticweb.owlapi.model.*;
import org.utility.OntologyManipulator;
import org.utility.YAMLConfigLoader;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.utility.StatsPrinter.printStats;

public class LaunchLLMLeaner extends LaunchLearner {

    private List<String> ontologies;
    private List<String> models;
    private String system;
    private Integer maxTokens;
    private List<Integer> hypothesisSizes;

    private double epsilon = 0.1;
    private double delta = 0.2;


    public static void main(String[] args) {
        Logger.getRootLogger().setLevel(Level.OFF);
        new LaunchLLMLeaner().run(args);
    }

    private void loadConfiguration(String fileName) {
        Configuration config = new YAMLConfigLoader().getConfig(fileName, Configuration.class);
        //choose configuration from file here:
        models = config.getModels();
        system = config.getSystem();
        ontologies = config.getOntologies();
        maxTokens = config.getMaxTokens();
        hypothesisSizes = ontologies.stream().map(OntologyManipulator::computeOntologySize).collect(Collectors.toList());
    }

    public void run(String[] args) {
        String configurationFile = args[0];
        if (args.length > 1) {
            epsilon = Double.parseDouble(args[1]);
        }
        if (args.length > 2) {
            delta = Double.parseDouble(args[2]);
        }
        SmartLogger.checkCachedFiles();
        loadConfiguration(configurationFile);
        try {
            for (String ontology : ontologies) {
                System.out.println("\nRunning experiment for " + ontology);
                for (String model : models) {
                    System.out.println("\nRunning experiment for " + model + "\n");
                    setup(ontology, model.replace(":", "-"));
                    elQueryEngineForT = new ELEngine(groundTruthOntology);
                    llmQueryEngineForH = new LLMEngine(hypothesisOntology, model, system, maxTokens, myManager);
                    llmQueryEngineForT = new LLMEngine(groundTruthOntology, model, system, maxTokens, myManager);
                    elQueryEngineForH = new ELEngine(hypothesisOntology);
                    learner = new Learner(llmQueryEngineForT, elQueryEngineForH, myMetrics);
                    oracle = new Oracle(llmQueryEngineForT, elQueryEngineForH);
                    runLearningExperiment(args, hypothesisSizes.get(ontologies.indexOf(ontology)));
                    cleaningUp();
                    launchResultAnalyzer(args);
                }
                System.out.println("\nFinished experiment for " + ontology + "\n");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("error" + e);
        }
    }

    private void launchResultAnalyzer(String[] args) {
        new ResultAnalyzer(args).run();

    }

    private void setup(String ontology, String model) {
        try {
            myMetrics = new Metrics(myRenderer);
            System.out.println("Trying to load groundTruthOntology");
            loadTargetOntology(ontology);
            setUpOntologyFolders(model);
            saveTargetOntology();
            loadHypothesisOntology();
            System.out.println(groundTruthOntology);
            System.out.println("Loaded successfully.");
            System.out.println();
            System.out.flush();
            computeConceptAndRoleNumbers();
        } catch (OWLOntologyCreationException e) {
            System.out.println("Could not load groundTruthOntology: " + e.getMessage());
        } catch (IOException | OWLException e) {
            e.printStackTrace();
        }
    }

    private void runLearningExperiment(String[] args, int hypothesisSize) throws Throwable {
        long timeStart = System.currentTimeMillis();
        runLearner(hypothesisSize);
        long timeEnd = System.currentTimeMillis();
        saveOWLFile(hypothesisOntology, hypoFile);
        validation();
        printStats(timeStart, timeEnd, args, true,
                targetFile, myMetrics, learner, oracle, conceptNumber, roleNumber, groundTruthOntology, hypothesisOntology);
    }

    private void runLearner(int hypothesisSize) throws Throwable {
        // Computes inclusions of the form A implies B
        precomputation();
        int i= 0;
        while (true) {
            myMetrics.setEquivCount(myMetrics.getEquivCount() + 1);
            counterExample = getCounterExample(hypothesisSize);
            i++;
            System.out.println("Counterexample number: " + i);
            if (counterExample == null) {
                System.out.println("No counterexample found, closing...");
                break;
            }
            // Add the last counterexample to axiomsT

            // Update size of the largest counterexample
            int size = myMetrics.getSizeOfCounterexample(counterExample);
            if (size > myMetrics.getSizeOfLargestCounterExample()) {
                myMetrics.setSizeOfLargestCounterExample(size);
            }

            // Decompose the last counterexample
            counterExample = learner.decompose(counterExample.getSubClass(), counterExample.getSuperClass());

            // Check if transformation can be applied
            checkTransformations();
        }

    }


    private OWLSubClassOfAxiom getCounterExample(int hypothesisSize) throws Exception {

        // Seed for random number generator can be generated randomly
        int seed = 1;
        // Initialize the statement builder
        this.builder = new StatementBuilderImpl(seed, parser.getClassesNamesAsString(), parser.getObjectPropertiesAsString());
        // Initialize PAC with epsilon and gamma values
        Pac pac = new Pac(builder.getNumberOfStatements(), epsilon, delta, hypothesisSize, builder.getAllStatements());
        int iterations = 0;
        // Iterate over PAC training samples
        while (pac.getPacStatementsSize() > 0) {
            System.out.println("PAC Training sample: " + ++iterations + " out of " + pac.getNumberOfExamples());
            // Get the last counterexample
            String statement = pac.getRandomStatement();
            if (statement == null) {
                System.out.println("PAC Algorithm completed");
                return null;
            }
            var selectedAxiom = OntologyManipulator.createAxiomFromString(statement, groundTruthOntology);
            if (selectedAxiom.isOfType(AxiomType.SUBCLASS_OF)) {
                if (!elQueryEngineForH.entailed(selectedAxiom) && llmQueryEngineForT.entailed(selectedAxiom)) {
                    OWLSubClassOfAxiom counterexample = (OWLSubClassOfAxiom) selectedAxiom;
                    return getCounterExampleSubClassOf(counterexample);
                }
            } else if (selectedAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                OWLEquivalentClassesAxiom equivCounterexample = (OWLEquivalentClassesAxiom) selectedAxiom;
                Set<OWLSubClassOfAxiom> eqSubClassAxioms = equivCounterexample.asOWLSubClassOfAxioms();
                for (OWLSubClassOfAxiom subClassAxiom : eqSubClassAxioms) {
                    if (!elQueryEngineForH.entailed(subClassAxiom) && llmQueryEngineForT.entailed(selectedAxiom)) {
                        return getCounterExampleSubClassOf(subClassAxiom);
                    }
                }
            } else {
                throw new Exception("Unknown axiom type: " + selectedAxiom.getAxiomType() + "You must delete unknown axioms FIRST!");
            }
        }
        return null;
    }

    private OWLSubClassOfAxiom getCounterExampleSubClassOf(OWLSubClassOfAxiom counterexample) throws Exception {
        OWLSubClassOfAxiom newCounterexampleAxiom;
        OWLClassExpression left = counterexample.getSubClass();
        OWLClassExpression right = counterexample.getSuperClass();
        double p = 0;

        newCounterexampleAxiom = oracle.mergeLeft(left, right, p);
        left = newCounterexampleAxiom.getSubClass();
        right = newCounterexampleAxiom.getSuperClass();

        newCounterexampleAxiom = oracle.saturateLeft(left, right, p);
        left = newCounterexampleAxiom.getSubClass();
        right = newCounterexampleAxiom.getSuperClass();

        newCounterexampleAxiom = oracle.branchRight(left, right, p);
        left = newCounterexampleAxiom.getSubClass();
        right = newCounterexampleAxiom.getSuperClass();

        newCounterexampleAxiom = oracle.composeLeft(left, right, p);
        left = newCounterexampleAxiom.getSubClass();
        right = newCounterexampleAxiom.getSuperClass();

        newCounterexampleAxiom = oracle.composeRight(left, right, p);
        left = newCounterexampleAxiom.getSubClass();
        right = newCounterexampleAxiom.getSuperClass();

        newCounterexampleAxiom = oracle.unsaturateRight(left, right, p);

        return newCounterexampleAxiom;
    }
}
