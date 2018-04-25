package org.zhaowl.console;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.zhaowl.engine.ELEngine;
import org.zhaowl.learner.ELLearner;
import org.zhaowl.oracle.ELOracle;
import org.zhaowl.utils.Metrics;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

//import javax.swing.JOptionPane;

public class consoleLearner {

    private String filePath;

    // ############# Game variables Start ######################

    // #########################################################

    // ############# OWL variables Start ######################

    private static final OWLOntologyManager myManager = OWLManager.createOWLOntologyManager();
    private final OWLObjectRenderer myRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
    private final Metrics myMetrics = new Metrics(myRenderer);


    private Set<OWLAxiom> axiomsT = null;

    private String ontologyFolder = null;
    private String ontologyName = null;
    private File hypoFile = null;

    private ArrayList<String> roles = new ArrayList<>();

    private String ontologyFolderH = null;

    private OWLSubClassOfAxiom lastCE = null;
    private OWLClassExpression lastExpression = null;
    private OWLClass lastName = null;
    private OWLOntology targetOntology = null;
    private OWLOntology hypothesisOntology = null;

    private ELEngine elQueryEngineForT = null;
    private ELEngine elQueryEngineForH = null;

    private ELLearner elLearner = null;
    private ELOracle elOracle = null;

    // ############# OWL variables Start ######################

    // #########################################################

    // ############# Oracle and Learner skills Start ######################

    private boolean oracleSaturate;
    private boolean oracleMerge;
    private boolean oracleBranch;
    private boolean oracleUnsaturate;

    private boolean learnerSat;
    private boolean learnerMerge;
    private boolean learnerDecompL;
    private boolean learnerUnsat;
    private boolean learnerBranch;
    private boolean learnerDecompR;

    // ############# Oracle and Learner skills END ######################

    public static void main(String[] args) {
        Logger.getRootLogger().setLevel(Level.OFF);
        consoleLearner maker = new consoleLearner();
        maker.doIt(args);

    }

    public void doIt(String[] args) {

        try {
            // targetOntology from parameters
            filePath = args[0];


            // setLearnerSkills
            setLearnerSkills(args);

            // setLearnerSkills
            setOracleSkills(args);
            // load targetOntology

            try {
                setupOntologies();

                elQueryEngineForT = new ELEngine(targetOntology);
                elQueryEngineForH = new ELEngine(hypothesisOntology);

                elLearner = new ELLearner(elQueryEngineForT, elQueryEngineForH, myMetrics);
                elOracle = new ELOracle(elQueryEngineForT, elQueryEngineForH, myMetrics);


                long timeStart = System.currentTimeMillis();
                runLearner(elQueryEngineForT, elQueryEngineForH);
                long timeEnd = System.currentTimeMillis();
                System.out.println("Total time (ms): " + (timeEnd - timeStart));
                System.out.println("Total membership queries: " + myMetrics.getMembCount());
                System.out.println("Total equivalence queries: " + myMetrics.getEquivCount());
                System.out.println("Target TBox logical axioms: " + axiomsT.size());
                //////////////////////////////////////////////////////////////////////
                System.out.println("Total left decompositions: " + elLearner.getNumberLeftDecomposition());
                System.out.println("Total right decompositions: " + elLearner.getNumberRightDecomposition());
                System.out.println("Total mergings: " + elLearner.getNumberMerging());
                System.out.println("Total branchings: " + elLearner.getNumberBranching());
                System.out.println("Total saturations: " + elLearner.getNumberSaturations());
                System.out.println("Total unsaturations: " + elLearner.getNumberUnsaturations());
                saveOWLFile(hypothesisOntology, hypoFile);


                myMetrics.showCIT(axiomsT, true);

                System.out.println("Hypothesis TBox logical axioms: " + hypothesisOntology.getAxioms().size());
                myMetrics.showCIT(hypothesisOntology.getAxioms(), false);
                elQueryEngineForH.disposeOfReasoner();
                elQueryEngineForT.disposeOfReasoner();
                myManager.removeOntology(hypothesisOntology);
                myManager.removeOntology(targetOntology);

            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println("error in runLearner call ----- " + e);
            }

        } catch (Throwable e) {
            // TODO Auto-generated catch block
            System.out.println("error in doIt --- " + e);
        } finally {
            elQueryEngineForH.disposeOfReasoner();
            elQueryEngineForT.disposeOfReasoner();
        }

    }

    private void setOracleSkills(String[] args) {
        oracleMerge = args[7].equals("t");

        oracleSaturate = args[8].equals("t");

        oracleBranch = args[9].equals("t");

        oracleUnsaturate = args[10].equals("t");
    }

    private void setLearnerSkills(String[] args) {

        learnerDecompL = args[1].equals("t");

        learnerBranch = args[2].equals("t");

        learnerUnsat = args[3].equals("t");

        learnerDecompR = args[4].equals("t");

        learnerMerge = args[5].equals("t");

        learnerSat = args[6].equals("t");

    }


    private void runLearner(ELEngine elQueryEngineForT, ELEngine elQueryEngineForH) throws Throwable {
        while (!equivalenceQuery()) {
            myMetrics.setEquivCount(myMetrics.getMembCount() + 1);
            lastCE = getCounterExample(elQueryEngineForT, elQueryEngineForH);

            OWLSubClassOfAxiom counterexample = lastCE;
            OWLClassExpression left = counterexample.getSubClass();
            OWLClassExpression right = counterexample.getSuperClass();
            lastCE = elLearner.decompose(left, right);
            if (canTransformELrhs()) {
                lastCE = computeEssentialRightCounterexample();
                //TODO
                //if there is a concept name in H ...
                //if()
                //siblingmerge
            } else if (canTransformELlhs()) {
                lastCE = computeEssentialLeftCounterexample();
            }
            addHypothesis(lastCE);
        }
        victory();
        lastCE = null;
    }


    private OWLSubClassOfAxiom computeEssentialLeftCounterexample() throws Exception {
        OWLSubClassOfAxiom axiom = lastCE;
        OWLSubClassOfAxiom counterexample = axiom;
        OWLClassExpression left = counterexample.getSubClass();
        OWLClass right = (OWLClass) counterexample.getSuperClass();


        if (learnerDecompL) {
            axiom = elLearner.decomposeLeft(lastExpression, lastName);
            counterexample = axiom;
            left = counterexample.getSubClass();
            right = (OWLClass) counterexample.getSuperClass();
        }

        if (learnerBranch) {
            axiom = elLearner.branchLeft(left, right);
            counterexample = axiom;
            left = counterexample.getSubClass();
            right = (OWLClass) counterexample.getSuperClass();
        }

        if (learnerUnsat) {
            axiom = elLearner.unsaturateLeft(left, right);
        }

        return axiom;
    }

    private OWLSubClassOfAxiom computeEssentialRightCounterexample() throws Exception {
        OWLSubClassOfAxiom axiom = lastCE;
        OWLSubClassOfAxiom counterexample = axiom;
        OWLClass left = (OWLClass) counterexample.getSubClass();
        OWLClassExpression right = counterexample.getSuperClass();


        if (learnerDecompR) {
            axiom = elLearner.decomposeRight(lastName, lastExpression);
            counterexample = axiom;
            left = (OWLClass) counterexample.getSubClass();
            right = counterexample.getSuperClass();
        }

        if (learnerMerge) {
            axiom = elLearner.mergeRight(left, right);
            counterexample = axiom;
            left = (OWLClass) counterexample.getSubClass();
            right = counterexample.getSuperClass();
        }

        if (learnerSat) {
            axiom = elLearner.saturateRight(left, right);
        }
        return axiom;
    }


    private void victory() {
        //win = true;
        System.out.println("Ontology learned successfully!");
        System.out.println("You dun did it!!!");

        axiomsT = new HashSet<>();
        for (OWLAxiom axe : targetOntology.getAxioms())
            if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
                    || axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
                axiomsT.add(axe);
    }

    private void setupOntologies() {
        //win = false;

        try {

            System.out.println("Trying to load targetOntology");
            targetOntology = myManager.loadOntologyFromOntologyDocument(new File(filePath));


            axiomsT = new HashSet<>();
            for (OWLAxiom axe : targetOntology.getAxioms())
                if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
                        || axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
                    axiomsT.add(axe);
//            Set<OWLAxiom> axiomsTCheck = new HashSet<>();
//			for (OWLAxiom axe : targetOntology.getAxioms())
//				if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
//						|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
//					axiomsTCheck.add(axe);

            lastCE = null;


            // transfer Origin targetOntology to ManchesterOWLSyntaxOntologyFormat
            OWLOntologyFormat format = myManager.getOntologyFormat(targetOntology);
            ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
            if (format.isPrefixOWLOntologyFormat()) {
                manSyntaxFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
            }
            // format = null;

            // create personalized names for targetOntology
            ontologyFolderH = "src/main/resources/tmp/";
            ontologyFolder = "src/main/resources/tmp/";
            ontologyName = "";
            getOntologyName();

            // save ontologies
            File newFile = new File(ontologyFolder);
            hypoFile = new File(ontologyFolderH);
            // save owl file as a new file in different location
            if (newFile.exists()) {
                newFile.delete();
            }
            newFile.createNewFile();
            myManager.saveOntology(targetOntology, manSyntaxFormat, IRI.create(newFile.toURI()));

            // Create OWL Ontology Manager for hypothesis and load hypothesis file
            if (hypoFile.exists()) {
                hypoFile.delete();
            }
            hypoFile.createNewFile();

            hypothesisOntology = myManager.loadOntologyFromOntologyDocument(hypoFile);


//			axiomsH = hypothesisOntology.getAxioms();
            //public boolean win;
            // boolean wePlayin = true;


            System.out.println(targetOntology);
            System.out.println("Loaded successfully.");
            System.out.println();

            ArrayList<String> concepts = myMetrics.getSuggestionNames("concept", newFile);
            roles = myMetrics.getSuggestionNames("role", newFile);

            System.out.println("Total number of concepts is: " + concepts.size());


            System.out.flush();
        } catch (OWLOntologyCreationException e) {
            System.out.println("Could not load targetOntology: " + e.getMessage());
        } catch (OWLException | IOException e) {
            e.printStackTrace();
        }

    }


    private void getOntologyName() {

        int con = 0;
        for (int i = 0; i < targetOntology.getOntologyID().toString().length(); i++)
            if (targetOntology.getOntologyID().toString().charAt(i) == '/')
                con = i;
        ontologyName = targetOntology.getOntologyID().toString().substring(con + 1,
                targetOntology.getOntologyID().toString().length());
        ontologyName = ontologyName.substring(0, ontologyName.length() - 3);
        if (!ontologyName.contains(".owl"))
            ontologyName = ontologyName + ".owl";
        ontologyFolder += ontologyName;
        ontologyFolderH += "hypo_" + ontologyName;
    }

    private Boolean equivalenceQuery() {

        return elQueryEngineForH.entailed(axiomsT);
    }


    private OWLSubClassOfAxiom getCounterExample(ELEngine elQueryEngineForT, ELEngine elQueryEngineForH) throws Exception {
        for (OWLAxiom selectedAxiom : axiomsT) {
            selectedAxiom.getAxiomType();

            // first get CounterExample from an axiom with the type SUBCLASS_OF
            if (selectedAxiom.isOfType(AxiomType.SUBCLASS_OF)) {
                Boolean queryAns = elQueryEngineForH.entailed(selectedAxiom);
                // if hypothesis does NOT entail the CI
                if (!queryAns) {
                    // System.out.println("Chosen CE:" + myRenderer.render(selectedAxiom));
                    OWLSubClassOfAxiom counterexample = (OWLSubClassOfAxiom) selectedAxiom;
                    OWLClassExpression subclass = counterexample.getSubClass();
                    OWLClassExpression superclass = counterexample.getSuperClass();

                    // create new counter example from the subclass and superclass
                    // of axiom NOT entailed by H

                    OWLSubClassOfAxiom newCounterexampleAxiom = getCounterExamplefromSubClassAxiom(subclass, superclass, elQueryEngineForT, elQueryEngineForH);
                    if (newCounterexampleAxiom != null) {
                        // if we actually got something, we use it as new counter example

                        // System.out.println("subclass 1");
                        // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                        // ADD SATURATION FOR newCounterexampleAxiom HERE
                        if (checkLeft(newCounterexampleAxiom)) {
                            if (oracleMerge) {
                                // ex = null;
                                // System.out.println(newCounterexampleAxiom);
                                // if (checkLeft(newCounterexampleAxiom)) {
                                OWLClassExpression ex = elOracle.oracleSiblingMerge(
                                        newCounterexampleAxiom.getSubClass()
                                );
                                newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(ex, superclass);
                                // ex = null;
                            }
                            if (oracleSaturate)
                                newCounterexampleAxiom = elOracle
                                        .saturateWithTreeLeft(newCounterexampleAxiom);
                        } else {

                            if (oracleBranch) {
                                // ex = null;
                                OWLSubClassOfAxiom auxAx = newCounterexampleAxiom;
                                OWLClassExpression ex = elOracle.branchRight(auxAx.getSuperClass());
                                newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(auxAx.getSubClass(), ex);
                            }
                            if (oracleUnsaturate) {
                                // ex = null;
                                OWLClassExpression ex = elOracle.unsaturateRight(newCounterexampleAxiom);
                                newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(subclass, ex);
                            }
                        }
                        /*
                         * } else { ex = siblingMerge(((OWLSubClassOfAxiom)
                         * newCounterexampleAxiom).getSuperClass()); newCounterexampleAxiom =
                         * saturateWithTreeRight( elQueryEngineForT.getSubClassAxiom(subclass, ex)); }
                         */

                        // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                        lastCE = newCounterexampleAxiom;
                        return newCounterexampleAxiom;
                    }
                }
            }

            // get CounterExample from an axiom with the type EQUIVALENT_CLASSES
            if (selectedAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {

                OWLEquivalentClassesAxiom counterexample = (OWLEquivalentClassesAxiom) selectedAxiom;
                Set<OWLSubClassOfAxiom> eqsubclassaxioms = counterexample.asOWLSubClassOfAxioms();

                for (OWLSubClassOfAxiom subClassAxiom : eqsubclassaxioms) {
                    OWLClassExpression subclass = subClassAxiom.getSubClass();

                    Set<OWLClass> superclasses = elQueryEngineForT.getSuperClasses(subclass, true);
                    if (!superclasses.isEmpty()) {
                        for (OWLClass SuperclassInSet : superclasses) {
                            OWLSubClassOfAxiom newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(subclass,
                                    SuperclassInSet);

                            Boolean querySubClass = elQueryEngineForH.entailed(newCounterexampleAxiom);
                            Boolean querySubClassforT = elQueryEngineForT.entailed(newCounterexampleAxiom);
                            if (!querySubClass && querySubClassforT) {

                                // System.out.println("eq 1");
                                // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                                // ADD SATURATION FOR newCounterexampleAxiom HERE
//                                OWLClassExpression ex = null;
                                if (checkLeft(newCounterexampleAxiom)) {
                                    if (oracleMerge) {

                                        // ex = null;
                                        // System.out.println(newCounterexampleAxiom);
                                        // if (checkLeft(newCounterexampleAxiom)) {
                                        OWLClassExpression ex = elOracle.oracleSiblingMerge(
                                                newCounterexampleAxiom.getSubClass()
                                        );
                                        newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(ex,
                                                newCounterexampleAxiom.getSuperClass());
                                        // ex = null;
                                    }
                                    if (oracleSaturate)
                                        newCounterexampleAxiom = elOracle
                                                .saturateWithTreeLeft(newCounterexampleAxiom);
                                } else {
                                    if (oracleBranch) {
                                        // ex = null;
                                        OWLSubClassOfAxiom auxAx = newCounterexampleAxiom;
                                        OWLClassExpression ex = elOracle.branchRight(auxAx.getSuperClass());
                                        newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(auxAx.getSubClass(),
                                                ex);
                                        // auxAx = null;
                                        // ex = null;
                                    }
                                    if (oracleUnsaturate) {
                                        // ex = null;
                                        OWLClassExpression ex = elOracle.unsaturateRight(newCounterexampleAxiom);
                                        newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(
                                                newCounterexampleAxiom.getSubClass(), ex);
                                        // ex = null;
                                    }
                                }
                                // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                                lastCE = newCounterexampleAxiom;
//								subclass = null;
//								SuperclassInSet = null;
//								superclasses = null;
//								counterexample = null;
//								subClassAxiom = null;
//								selectedAxiom = null;
//								iteratorT = null;
                                System.out.flush();
                                return newCounterexampleAxiom;
                            }
                        }
                    }

                }
            }
        }

        for (OWLAxiom Axiom : axiomsT) {
            Axiom.getAxiomType();
            if ((Axiom.isOfType(AxiomType.SUBCLASS_OF)) || (Axiom.isOfType(AxiomType.EQUIVALENT_CLASSES))) {

                Axiom.getAxiomType();
                if (Axiom.isOfType(AxiomType.SUBCLASS_OF)) {
                    OWLSubClassOfAxiom selectedAxiom = (OWLSubClassOfAxiom) Axiom;
                    Boolean queryAns = elQueryEngineForH.entailed(selectedAxiom);
                    if (!queryAns) {
                        selectedAxiom = processAxiom(selectedAxiom);
                        lastCE = selectedAxiom;
                        return selectedAxiom;
                    }
                }

                if (Axiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                    OWLEquivalentClassesAxiom counterexample = (OWLEquivalentClassesAxiom) Axiom;

                    Set<OWLSubClassOfAxiom> eqsubclassaxioms = counterexample.asOWLSubClassOfAxioms();
                    for (OWLSubClassOfAxiom subClassAxiom : eqsubclassaxioms) {
                        Boolean queryAns = elQueryEngineForH.entailed(subClassAxiom);
                        if (!queryAns) {
                            subClassAxiom = processAxiom(subClassAxiom);
                            // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*?*-*-*-*-*-*-*-*
                            lastCE = subClassAxiom;
                            return subClassAxiom;
                        }
                    }
                }
            }
        }
        System.out.println("no more CIs");
//		iterator = null;
//		iteratorT = null;
//		System.out.flush();
        return null;
    }

    private OWLSubClassOfAxiom processAxiom(OWLSubClassOfAxiom owlSubClassOfAxiom) throws Exception {



        if (checkLeft(owlSubClassOfAxiom)) {
            if (oracleMerge) {
                OWLClassExpression ex = elOracle.oracleSiblingMerge(owlSubClassOfAxiom.getSubClass()
                );
                owlSubClassOfAxiom = elQueryEngineForT.getSubClassAxiom(ex,
                        owlSubClassOfAxiom.getSuperClass());
                // ex = null;
            }
            if (oracleSaturate)
                owlSubClassOfAxiom = elOracle
                        .saturateWithTreeLeft(owlSubClassOfAxiom);
        } else {
            if (oracleBranch) {
                OWLSubClassOfAxiom auxAx = owlSubClassOfAxiom;
                OWLClassExpression ex = elOracle.branchRight(auxAx.getSuperClass());
                owlSubClassOfAxiom = elQueryEngineForT
                        .getSubClassAxiom(auxAx.getSubClass(), ex);
//								auxAx = null;
//								ex = null;
            }
            if (oracleUnsaturate) {
                OWLClassExpression ex = elOracle.unsaturateRight(owlSubClassOfAxiom);
                owlSubClassOfAxiom = elQueryEngineForT
                        .getSubClassAxiom(owlSubClassOfAxiom.getSubClass(), ex);
                // ex = null;
            }
        }
        // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
        return owlSubClassOfAxiom;
    }

    private boolean checkLeft(OWLAxiom axiom) {

        String left = myRenderer.render(((OWLSubClassOfAxiom) axiom).getSubClass());
        String right = myRenderer.render(((OWLSubClassOfAxiom) axiom).getSuperClass());
        for (String rol : roles) {
            if (left.contains(rol)) {
                return true;
            } else if (right.contains(rol)) {
                return false;
            }
        }
        return true;
    }

    private OWLSubClassOfAxiom getCounterExamplefromSubClassAxiom(OWLClassExpression subclass, OWLClassExpression superclass,
                                                                  ELEngine elQueryEngineForT, ELEngine elQueryEngineForH) {
        Set<OWLClass> superclasses = elQueryEngineForT.getSuperClasses(superclass, false);
        Set<OWLClass> subclasses = elQueryEngineForT.getSubClasses(subclass, false);

        if (!subclasses.isEmpty()) {

            for (OWLClass SubclassInSet : subclasses) {
                OWLSubClassOfAxiom newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(SubclassInSet, superclass);
                Boolean querySubClass = elQueryEngineForH.entailed(newCounterexampleAxiom);
                Boolean querySubClassforT = elQueryEngineForT.entailed(newCounterexampleAxiom);
                if (!querySubClass && querySubClassforT) {
//					SubclassInSet = null;
//					superclass = null;
//					iteratorSubClass = null;
//					elQueryEngineForH = null;

                    return newCounterexampleAxiom;
                }
            }
        }
        if (!superclasses.isEmpty()) {

            for (OWLClass SuperclassInSet : superclasses) {
                OWLSubClassOfAxiom newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(subclass, SuperclassInSet);
                Boolean querySubClass = elQueryEngineForH.entailed(newCounterexampleAxiom);
                Boolean querySubClassforT = elQueryEngineForT.entailed(newCounterexampleAxiom);
                if (!querySubClass && querySubClassforT) {

//					SuperclassInSet = null;
//					superclass = null;
//					subclass = null;
//					iteratorSuperClass = null;
//					elQueryEngineForH = null;

                    return newCounterexampleAxiom;
                }
            }
        }

//		elQueryEngineForH = null;
//		superclass = null;
//		subclass = null;
        return null;
    }

    private void addHypothesis(OWLAxiom addedAxiom) {
        //String StringAxiom = myRenderer.render(addedAxiom);

        //AddAxiom newAxiomInH = new AddAxiom(hypothesisOntology, addedAxiom);
        //myManager.applyChange(newAxiomInH);
        //System.out.println(addedAxiom.toString());
        myManager.addAxiom(hypothesisOntology, addedAxiom);
        minimiseHypothesis();
        // saveOWLFile(hypothesisOntology, hypoFile);

    }

    private void saveOWLFile(OWLOntology ontology, File file) throws Exception {

        OWLOntologyFormat format = myManager.getOntologyFormat(ontology);
        ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
        if (format.isPrefixOWLOntologyFormat()) {
            // need to remove prefixes
            manSyntaxFormat.clearPrefixes();
        }
        // format = null;
        myManager.saveOntology(ontology, manSyntaxFormat, IRI.create(file.toURI()));
    }

    private void minimiseHypothesis() {


        Set<OWLAxiom> tmpaxiomsH = elQueryEngineForH.getOntology().getAxioms();
        Iterator<OWLAxiom> ineratorMinH = tmpaxiomsH.iterator();
        Set<OWLAxiom> checkedAxiomsSet = new HashSet<>();


        if (tmpaxiomsH.size() > 1) {
            while (ineratorMinH.hasNext()) {
                OWLAxiom checkedAxiom = ineratorMinH.next();
                if (!checkedAxiomsSet.contains(checkedAxiom)) {
                    checkedAxiomsSet.add(checkedAxiom);

                    RemoveAxiom removedAxiom = new RemoveAxiom(elQueryEngineForH.getOntology(), checkedAxiom);
                    elQueryEngineForH.applyChange(removedAxiom);

                    Boolean queryAns = elQueryEngineForH.entailed(checkedAxiom);

                    if (!queryAns) {
                        //put it back
                        AddAxiom addAxiomtoH = new AddAxiom(hypothesisOntology, checkedAxiom);
                        elQueryEngineForH.applyChange(addAxiomtoH);
                    }
                }
            }
        }

    }

    private Boolean canTransformELrhs() {

        OWLSubClassOfAxiom counterexample = lastCE;
        OWLClassExpression left = counterexample.getSuperClass();
        OWLClassExpression right = counterexample.getSubClass();
        for (OWLClass cl1 : left.getClassesInSignature()) {
            if (elOracle.isCounterExample(cl1, right)) {
                lastCE = elQueryEngineForT.getSubClassAxiom(cl1, right);
                lastExpression = right;
                lastName = cl1;
                return true;
            }
        }
        return false;
    }

    private Boolean canTransformELlhs() {
        OWLSubClassOfAxiom counterexample = lastCE;
        OWLClassExpression left = counterexample.getSuperClass();
        OWLClassExpression right = counterexample.getSubClass();
        for (OWLClass cl1 : right.getClassesInSignature()) {
            if (elOracle.isCounterExample(left, cl1)) {
                lastCE = elQueryEngineForT.getSubClassAxiom(left, cl1);
                lastExpression = left;
                lastName = cl1;
                return true;
            }
        }
        return false;
    }
}
