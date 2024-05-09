package org.experiments.exp2;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.exactlearner.engine.BaseEngine;
import org.exactlearner.learner.Learner;
import org.exactlearner.oracle.Oracle;
import org.exactlearner.parser.OWLParserImpl;
import org.exactlearner.utils.Metrics;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class LaunchLearner {

    int conceptNumber;
    int roleNumber;
    File targetFile;
    String ontologyFolder = "";
    BaseEngine llmQueryEngineForT;
    //BaseEngine elQueryEngineForT;
    //BaseEngine llmQueryEngineForH;
    BaseEngine elQueryEngineForH;
    OWLClassExpression lastExpression;
    OWLSubClassOfAxiom counterExample;
    OWLOntology targetOntology;
    OWLOntology hypothesisOntology;
    Learner learner;
    File hypoFile;
    OWLParserImpl parser;
    String ontologyFolderH = "";
    Oracle oracle = null;
    OWLClass lastName = null;
    Set<OWLAxiom> axiomsT = new HashSet<>();

    final static OWLOntologyManager myManager = OWLManager.createOWLOntologyManager();
    final static OWLObjectRenderer myRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
    final static String fileSeparator = System.getProperty("file.separator");
    Metrics myMetrics = new Metrics(myRenderer);

    void validation() throws Exception {
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

    void cleaningUp() {
        elQueryEngineForH.disposeOfReasoner();
        llmQueryEngineForT.disposeOfReasoner();
        //llmQueryEngineForH.disposeOfReasoner();
        myManager.removeOntology(hypothesisOntology);
        myManager.removeOntology(targetOntology);
    }

    private void updateAxiomsT() {
        axiomsT = targetOntology.getAxioms().stream()
                .filter(axe -> (!axe.toString().contains("Thing") && (axe.isOfType(AxiomType.SUBCLASS_OF)
                        || axe.isOfType(AxiomType.EQUIVALENT_CLASSES))))
                .collect(Collectors.toSet());
    }

    void checkTransformations() throws Exception {
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
                lastExpression = llmQueryEngineForT.getOWLObjectIntersectionOf(mySet);
                counterExample = llmQueryEngineForT.getSubClassAxiom(lastName, lastExpression);
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

    void addHypothesis(OWLAxiom addedAxiom) {
        myManager.addAxiom(hypothesisOntology, addedAxiom);
    }

    private Boolean canTransformELrhs() {
        OWLSubClassOfAxiom counterexample = counterExample;
        OWLClassExpression left = counterexample.getSubClass();
        OWLClassExpression right = counterexample.getSuperClass();
        for (OWLClass cl1 : left.getClassesInSignature()) {
            if (oracle.isCounterExample(cl1, right)) {
                counterExample = llmQueryEngineForT.getSubClassAxiom(cl1, right);
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
                counterExample = llmQueryEngineForT.getSubClassAxiom(left, cl1);
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

    void precomputation(BaseEngine elQueryEngineForT) {
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

    void loadTargetOntology(String ontology) throws OWLOntologyCreationException, IOException {
        targetFile = new File(ontology);
        targetOntology = myManager.loadOntologyFromOntologyDocument(targetFile);
        parser = new OWLParserImpl(targetOntology);
    }

    void saveTargetOntology() throws OWLOntologyStorageException, IOException {
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

    void loadHypothesisOntology() throws OWLOntologyCreationException, IOException {
        hypoFile = new File(ontologyFolderH);
        if (hypoFile.exists()) {
            hypoFile.delete();
        }
        hypoFile.createNewFile();

        hypothesisOntology = myManager.loadOntologyFromOntologyDocument(hypoFile);
    }

    void setUpOntologyFolders(String model) {
        String ontologyID = targetOntology.getOntologyID().toString();
        int lastSlashIndex = ontologyID.lastIndexOf('/');
        int extensionIndex = ontologyID.lastIndexOf(".owl");
        String name = "";

        if (lastSlashIndex != -1 && extensionIndex != -1) {
            name = ontologyID.substring(lastSlashIndex + 1, extensionIndex) + ".owl";
        } else {
            System.out.println("Could not get ontology name. Exiting...");
            System.exit(1);
        }
        ontologyFolder = "results" + fileSeparator + "ontologies" + fileSeparator + "target_" + name;
        ontologyFolderH = "results" + fileSeparator + "ontologies" + fileSeparator + "learned_" + model + "_" + name;
    }

    void computeConceptAndRoleNumbers() throws IOException {
        ArrayList<String> concepts = myMetrics.getSuggestionNames("concept", new File(ontologyFolder));
        ArrayList<String> roles = myMetrics.getSuggestionNames("role", new File(ontologyFolder));

        this.conceptNumber = concepts.size();
        this.roleNumber = roles.size();
    }

    void saveOWLFile(OWLOntology ontology, File file) throws Exception {
        learner.minimiseHypothesis(elQueryEngineForH, hypothesisOntology);
        OWLOntologyFormat format = myManager.getOntologyFormat(ontology);
        ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
        if (format.isPrefixOWLOntologyFormat()) {
            // need to remove prefixes
            manSyntaxFormat.clearPrefixes();
        }
        myManager.saveOntology(ontology, manSyntaxFormat, IRI.create(file.toURI()));
    }
}