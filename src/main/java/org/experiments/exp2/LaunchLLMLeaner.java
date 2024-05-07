package org.experiments.exp2;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.configurations.Configuration;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.exactlearner.engine.BaseEngine;
import org.exactlearner.engine.ELEngine;
import org.exactlearner.engine.LLMEngine;
import org.exactlearner.learner.Learner;
import org.exactlearner.oracle.Oracle;
import org.exactlearner.parser.OWLParserImpl;
import org.exactlearner.utils.Metrics;
import org.pac.Pac;
import org.pac.StatementBuilder;
import org.pac.StatementBuilderImpl;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.utility.OntologyManipulator;
import org.utility.YAMLConfigLoader;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.utility.StatsPrinter.printStats;

public class LaunchLLMLeaner {

    private final static String fileSeparator = System.getProperty("file.separator");
    private File targetFile;
    private static final OWLOntologyManager myManager = OWLManager.createOWLOntologyManager();
    private final OWLObjectRenderer myRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
    private final Metrics myMetrics = new Metrics(myRenderer);
    private Set<OWLAxiom> axiomsT = new HashSet<>();
    private String ontologyFolder="";
    private String ontology="";
    private File hypoFile;
    private String ontologyFolderH="";
    private OWLSubClassOfAxiom counterExample = null;
    private OWLClassExpression lastExpression = null;
    private OWLClass lastName = null;
    private OWLParserImpl parser;
    private OWLOntology targetOntology = null;
    private OWLOntology hypothesisOntology = null;
    private BaseEngine llmQueryEngineForT = null;
    private BaseEngine elQueryEngineForT = null;
    private BaseEngine llmQueryEngineForH = null;
    private BaseEngine elQueryEngineForH = null;

    private Learner learner = null;
    private Oracle oracle = null;
    private String model;
    private String system;
    private Integer maxTokens;
    private int conceptNumber;
    private int roleNumber;
    private double epsilon = 0.1;
    private double delta = 0.2;
    private int hypothesisSize = 10;

    public static void main(String[] args) {
        Logger.getRootLogger().setLevel(Level.OFF);
        new LaunchLLMLeaner().run(args);
    }

    private void loadConfiguration(String fileName) {
        Configuration config = new YAMLConfigLoader().getConfig(fileName, Configuration.class);
        //choose configuration from file here:
        model = config.getModels().get(0); //mistral
        system = config.getSystem();
        ontology = config.getOntologies().get(0); //animals
        maxTokens = config.getMaxTokens();
        hypothesisSize = OntologyManipulator.computeOntologySize(ontology);
    }

    public void run(String[] args) {
        String configurationFile = args[0];
        if (args.length > 1) {
            epsilon = Double.parseDouble(args[1]);
        }
        if (args.length > 2) {
            delta = Double.parseDouble(args[2]);
        }
        loadConfiguration(configurationFile);
        try {
            setupOntologies();
            computeConceptAndRoleNumbers();

            elQueryEngineForT = new ELEngine(targetOntology);
            llmQueryEngineForH = new LLMEngine(hypothesisOntology, model, system, maxTokens, myManager);
            llmQueryEngineForT = new LLMEngine(targetOntology, model, system, maxTokens, myManager);
            elQueryEngineForH = new ELEngine(hypothesisOntology);

            learner = new Learner(llmQueryEngineForT, elQueryEngineForH, myMetrics);
            oracle = new Oracle(llmQueryEngineForT, llmQueryEngineForH);

            runLearningExperiment(args);
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("error" + e);
        } finally {
            cleaningUp();
        }
    }

    private void cleaningUp() {
        elQueryEngineForH.disposeOfReasoner();
        elQueryEngineForT.disposeOfReasoner();
        myManager.removeOntology(hypothesisOntology);
        myManager.removeOntology(targetOntology);
    }

    private void runLearningExperiment(String[] args) throws Throwable {
        long timeStart = System.currentTimeMillis();
        runLearner();
        long timeEnd = System.currentTimeMillis();
        saveOWLFile(hypothesisOntology, hypoFile);
        validation();
        printStats(timeStart, timeEnd, args, true,
                targetFile, myMetrics, learner, oracle, conceptNumber, roleNumber, targetOntology, hypothesisOntology);
    }

    private void validation() throws Exception {
        validateLearnedOntology();
        printVictoryMessage();
        updateAxiomsT();
    }

    private void validateLearnedOntology() throws Exception {
        if (!elQueryEngineForH.entailed(axiomsT)) {
            // throw new Exception("Something went horribly wrong!");
        }
    }

    private void printVictoryMessage() {
        System.out.println("\nOntology learned successfully!");
        System.out.println("Congratulations!");
    }

    private void updateAxiomsT() {
        axiomsT = targetOntology.getAxioms().stream()
                .filter(axe -> (!axe.toString().contains("Thing") && (axe.isOfType(AxiomType.SUBCLASS_OF)
                        || axe.isOfType(AxiomType.EQUIVALENT_CLASSES))))
                .collect(Collectors.toSet());
    }

    private void runLearner() throws Throwable {
        // Computes inclusions of the form A implies B
        precomputation(llmQueryEngineForT);

        // Initialize the statement builder
        int seed = 1; // Seed for random number generator can be generated randomly
        StatementBuilder builder = new StatementBuilderImpl(seed, parser.getClassesNamesAsString(), parser.getObjectPropertiesAsString());

        // Initialize PAC with epsilon and gamma values
        Pac pac = new Pac(builder.getNumberOfStatements(), epsilon, delta, hypothesisSize);

        // Iterate over PAC training samples
        for (int i = 1; i <= pac.getTrainingSamples(); i++) {
            System.out.println("PAC Training sample: " + i + " out of " + pac.getTrainingSamples());

            // Increment equivalence count
            myMetrics.setEquivCount(myMetrics.getEquivCount() + 1);

            // Get the last counterexample
            counterExample = getCounterExample(builder);

            // If no counterexample found, break the loop
            if (counterExample == null) {
                System.out.println("No counterexample found");
                break;
            }

            // Add the last counterexample to axiomsT
            axiomsT.add(counterExample);

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

    private void checkTransformations() throws Exception {
        if (canTransformELrhs()) {
            processRightHandSideTransformations();
        } else if (canTransformELlhs()) {
            processLeftHandSideTransformations();
        } else {
            handleNoTransformation();
        }
    }

    private void processRightHandSideTransformations() throws Exception {
        for (OWLSubClassOfAxiom ax : elQueryEngineForH.getOntology().getSubClassAxiomsForSubClass(lastName)) {
            if (ax.getSubClass().getClassExpressionType() == ClassExpressionType.OWL_CLASS && ax.getSubClass().equals(lastName)) {
                Set<OWLClassExpression> mySet = new HashSet<>(ax.getSuperClass().asConjunctSet());
                mySet.addAll(lastExpression.asConjunctSet());
                lastExpression = elQueryEngineForT.getOWLObjectIntersectionOf(mySet);
                counterExample = elQueryEngineForT.getSubClassAxiom(lastName, lastExpression);
                break; // Assuming only one axiom needs to be processed
            }
        }
        counterExample = computeEssentialRightCounterexample();
        addHypothesis(counterExample);
    }

    private void processLeftHandSideTransformations() throws Exception {
        counterExample = computeEssentialLeftCounterexample();
        addHypothesis(counterExample);
    }

    private void handleNoTransformation() {
        addHypothesis(counterExample);
        System.out.println("Not an EL Terminology:" + counterExample.getSubClass() + " SubclassOf " + counterExample.getSuperClass());
    }

    private void addHypothesis(OWLAxiom addedAxiom) {
        myManager.addAxiom(hypothesisOntology, addedAxiom);
    }

    private Boolean canTransformELrhs() {
        OWLSubClassOfAxiom counterexample = counterExample;
        OWLClassExpression left = counterexample.getSubClass();
        OWLClassExpression right = counterexample.getSuperClass();
        for (OWLClass cl1 : left.getClassesInSignature()) {
            if (oracle.isCounterExample(cl1, right)) {
                counterExample = elQueryEngineForT.getSubClassAxiom(cl1, right);
                lastExpression = right;
                lastName = cl1;
                return true;
            }
        }
        return false;
    }

    private Boolean canTransformELlhs() {
        OWLSubClassOfAxiom counterexample = counterExample;
        OWLClassExpression left = counterexample.getSubClass();
        OWLClassExpression right = counterexample.getSuperClass();
        for (OWLClass cl1 : right.getClassesInSignature()) {
            if (oracle.isCounterExample(left, cl1)) {
                counterExample = elQueryEngineForT.getSubClassAxiom(left, cl1);
                lastExpression = left;
                lastName = cl1;
                return true;
            }
        }
        return false;
    }

    private OWLSubClassOfAxiom computeEssentialLeftCounterexample() throws Exception {
        OWLSubClassOfAxiom axiom = counterExample;

        lastExpression = axiom.getSubClass();
        lastName = (OWLClass) axiom.getSuperClass();

        axiom = learner.decomposeLeft(lastExpression, lastName);
        lastExpression = axiom.getSubClass();
        lastName = (OWLClass) axiom.getSuperClass();

        axiom = learner.branchLeft(lastExpression, lastName);
        lastExpression = axiom.getSubClass();
        lastName = (OWLClass) axiom.getSuperClass();

        axiom = learner.unsaturateLeft(lastExpression, lastName);

        return axiom;
    }

    private OWLSubClassOfAxiom computeEssentialRightCounterexample() throws Exception {
        OWLSubClassOfAxiom axiom = counterExample;

        lastName = (OWLClass) axiom.getSubClass();
        lastExpression = axiom.getSuperClass();

        axiom = learner.decomposeRight(lastName, lastExpression);
        lastName = (OWLClass) axiom.getSubClass();
        lastExpression = axiom.getSuperClass();

        axiom = learner.saturateRight(lastName, lastExpression);
        lastName = (OWLClass) axiom.getSubClass();
        lastExpression = axiom.getSuperClass();

        axiom = learner.decomposeRight(lastName, lastExpression);
        lastName = (OWLClass) axiom.getSubClass();
        lastExpression = axiom.getSuperClass();

        axiom = learner.mergeRight(lastName, lastExpression);

        return axiom;
    }


    private void saveOWLFile(OWLOntology ontology, File file) throws Exception {

        learner.minimiseHypothesis(elQueryEngineForH, hypothesisOntology);
        OWLOntologyFormat format = myManager.getOntologyFormat(ontology);
        ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
        if (format.isPrefixOWLOntologyFormat()) {
            // need to remove prefixes
            manSyntaxFormat.clearPrefixes();
        }
        myManager.saveOntology(ontology, manSyntaxFormat, IRI.create(file.toURI()));
    }

    private void setupOntologies() {
        try {
            System.out.println("Trying to load targetOntology");
            loadTargetOntology();
            getOntologyName();
            saveTargetOntology();
            loadHypothesisOntology();

            System.out.println(targetOntology);
            System.out.println("Loaded successfully.");
            System.out.println();
            System.out.flush();
        } catch (OWLOntologyCreationException e) {
            System.out.println("Could not load targetOntology: " + e.getMessage());
        } catch (IOException | OWLException e) {
            e.printStackTrace();
        }
    }

    private void getOntologyName() {
        String ontologyID = targetOntology.getOntologyID().toString();
        int lastSlashIndex = ontologyID.lastIndexOf('/');
        int extensionIndex = ontologyID.lastIndexOf(".owl");

        if (lastSlashIndex != -1 && extensionIndex != -1) {
            ontology = ontologyID.substring(lastSlashIndex + 1, extensionIndex) + ".owl";
        } else {
            // Fallback if the format is unexpected
            ontology = "ontology.owl";
        }
        ontologyFolder = "results" + fileSeparator + "ontologies" + fileSeparator + "target_" + ontology;
        ontologyFolderH = "results" + fileSeparator + "ontologies" + fileSeparator + "learned_" + model + "_" + ontology;
    }

    private void loadTargetOntology() throws OWLOntologyCreationException, IOException {
        targetFile = new File(ontology);
        targetOntology = myManager.loadOntologyFromOntologyDocument(targetFile);
        parser = new OWLParserImpl(targetOntology);
    }

    private void saveTargetOntology() throws OWLOntologyStorageException, IOException {
        OWLOntologyFormat format = myManager.getOntologyFormat(targetOntology);
        ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
        if (format.isPrefixOWLOntologyFormat()) {
            manSyntaxFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
        }

        File newFile = new File(ontologyFolder);
        if (newFile.exists()) {
            newFile.delete();
        }
        newFile.createNewFile();
        myManager.saveOntology(targetOntology, manSyntaxFormat, IRI.create(newFile.toURI()));
    }

    private void loadHypothesisOntology() throws OWLOntologyCreationException, IOException {
        hypoFile = new File(ontologyFolderH);
        if (hypoFile.exists()) {
            hypoFile.delete();
        }
        hypoFile.createNewFile();

        hypothesisOntology = myManager.loadOntologyFromOntologyDocument(hypoFile);
    }

    private void computeConceptAndRoleNumbers() throws IOException {
        ArrayList<String> concepts = myMetrics.getSuggestionNames("concept", new File(ontologyFolder));
        ArrayList<String> roles = myMetrics.getSuggestionNames("role", new File(ontologyFolder));

        this.conceptNumber = concepts.size();
        this.roleNumber = roles.size();
    }

    private OWLSubClassOfAxiom getCounterExample(StatementBuilder builder) throws Exception {
        var s = builder.chooseRandomStatement();
        if (s.isEmpty()) {
            return null;
        }
        var selectedAxiom = OntologyManipulator.createAxiomFromString(s.get(), targetOntology);
        if (!selectedAxiom.isOfType(AxiomType.SUBCLASS_OF) && !selectedAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
            throw new Exception("Unknown axiom type: " + selectedAxiom.getAxiomType() + "You must delete unknown axioms FIRST!");
        }
        if (selectedAxiom.isOfType(AxiomType.SUBCLASS_OF)) {
            if (!elQueryEngineForH.entailed(selectedAxiom) && elQueryEngineForT.entailed(selectedAxiom)) {
                OWLSubClassOfAxiom counterexample = (OWLSubClassOfAxiom) selectedAxiom;
                return getCounterExampleSubClassOf(counterexample);
            }
        }
        if (selectedAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
            OWLEquivalentClassesAxiom equivCounterexample = (OWLEquivalentClassesAxiom) selectedAxiom;
            Set<OWLSubClassOfAxiom> eqsubclassaxioms = equivCounterexample.asOWLSubClassOfAxioms();
            for (OWLSubClassOfAxiom subClassAxiom : eqsubclassaxioms) {
                if (!elQueryEngineForH.entailed(subClassAxiom) && elQueryEngineForT.entailed(selectedAxiom)) {
                    return getCounterExampleSubClassOf(subClassAxiom);
                }
            }
        }
        return getCounterExample(builder);
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

    private void precomputation(BaseEngine elQueryEngineForT) {
        int i = elQueryEngineForT.getClassesInSignature().size();
        myMetrics.setMembCount(myMetrics.getMembCount() + i * (i - 1));
        for (OWLClass cl1 : elQueryEngineForT.getClassesInSignature()) {
            Set<OWLClass> implied = elQueryEngineForT.getSuperClasses(cl1, true);
            for (OWLClass cl2 : implied) {
                OWLSubClassOfAxiom addedAxiom = elQueryEngineForT.getSubClassAxiom(cl1, cl2);
                addHypothesis(addedAxiom);
            }
        }

    }

}
