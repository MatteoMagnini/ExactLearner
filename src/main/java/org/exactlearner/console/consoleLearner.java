package org.exactlearner.console;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.exactlearner.engine.ELEngine;
import org.exactlearner.learner.ELLearner;
import org.exactlearner.oracle.ELOracle;
import org.exactlearner.utils.Metrics;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class consoleLearner {

	private static double SATURATION_BOUND = 0d;
	private static double MERGE_BOUND = 0d;
	private static double BRANCH_BOUND = 0d;
	private static double UNSATURATE_BOUND = 0d;
	private static double COMPOSE_LEFT_BOUND = 0d;
	private static double COMPOSE_RIGHT_BOUND = 0d;

	private String filePath;
	private File targetFile;

	// #########################################################

	// ############# OWL variables Start ######################

	private static final OWLOntologyManager myManager = OWLManager.createOWLOntologyManager();
	private final OWLObjectRenderer myRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
	private final Metrics myMetrics = new Metrics(myRenderer);

	private Set<OWLAxiom> axiomsT = null;
	private Set<OWLAxiom> axiomsTtmp = null;

	private String ontologyFolder = null;
	private String ontologyName = null;
	private File hypoFile = null;

	private String ontologyFolderH = null;

	private OWLSubClassOfAxiom lastCE = null;
	private OWLClassExpression lastExpression = null;
	private OWLClass lastName = null;
	private OWLOntology targetOntology = null;
	public OWLOntology hypothesisOntology = null;

	private ELEngine elQueryEngineForT = null;
	public ELEngine elQueryEngineForH = null;

	private ELLearner elLearner = null;
	private ELOracle elOracle = null;

	// ############# OWL variables End ######################

	// #########################################################

	// ############# Oracle and Learner Skills Start ######################

	private boolean oracleSaturate = false;
	private boolean oracleMerge = false;
	private boolean oracleBranch = false;
	private boolean oracleUnsaturate = false;
	private boolean oracleLeftCompose = false;
	private boolean oracleRightCompose = false;

	private boolean learnerSat;
	private boolean learnerMerge;
	private boolean learnerDecompL;
	private boolean learnerUnsat;
	private boolean learnerBranch;
	private boolean learnerDecompR;

	private int conceptNumber;
	private int roleNumber;

	class EquivalentException extends Exception {

		EquivalentException() {
			super();
		}
	}

	// ############# Oracle and Learner Skills End ######################

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

			try {
				// load targetOntology
				setupOntologies();

				elQueryEngineForT = new ELEngine(targetOntology);
				elQueryEngineForH = new ELEngine(hypothesisOntology);

				elLearner = new ELLearner(elQueryEngineForT, elQueryEngineForH, myMetrics);
				elOracle = new ELOracle(elQueryEngineForT, elQueryEngineForH);

				long timeStart = System.currentTimeMillis();
				runLearner(elQueryEngineForT, elQueryEngineForH);
				long timeEnd = System.currentTimeMillis();
				saveOWLFile(hypothesisOntology, hypoFile);
                victory();
                printStats(timeStart, timeEnd, args, false);
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

	private void printStat(String description, String value, Boolean verb) {
		if (verb) {
			System.out.print(description);
			System.out.println(value);
		} else {
			System.out.print(", " + value);
		}
	}

	private void printStat(String description, int value, Boolean verb) {
		printStat(description, String.valueOf(value), verb);
	}

	private void printStat(String description, Boolean verb) {
		if (verb) {
			printStat(description, " ", verb);
		}
	}

	private void printStats(long timeStart, long timeEnd, String[] args, Boolean verb) {
		if (!verb) {
			System.out.print(targetFile.getName());
			Arrays.stream(args).skip(1).forEach(x -> System.out.print(", " + x));
		}
		printStat("Total time (ms): ", String.valueOf(timeEnd - timeStart), verb);

		printStat("Total membership queries: ", myMetrics.getMembCount(), verb);
		printStat("Total equivalence queries: ", myMetrics.getEquivCount(), verb);
		//////////////////////////////////////////////////////////////////////
		printStat("\nLearner Stats:", verb);
		printStat("Total left decompositions: ", elLearner.getNumberLeftDecomposition(), verb);
		printStat("Total right decompositions: ", elLearner.getNumberRightDecomposition(), verb);
		printStat("Total mergings: ", elLearner.getNumberMerging(), verb);
		printStat("Total branchings: ", elLearner.getNumberBranching(), verb);
		printStat("Total saturations: ", elLearner.getNumberSaturations(), verb);
		printStat("Total unsaturations: ", elLearner.getNumberUnsaturations(), verb);
		//////////////////////////////////////////////////////////////////////
		printStat("\nOracle Stats:", verb);
		printStat("Total left compositions: ", elOracle.getNumberLeftComposition(), verb);
		printStat("Total right compositions: ", elOracle.getNumberRightComposition(), verb);
		printStat("Total mergings: ", elOracle.getNumberMerging(), verb);
		printStat("Total branchings: ", elOracle.getNumberBranching(), verb);
		printStat("Total saturations: ", elOracle.getNumberSaturations(), verb);
		printStat("Total unsaturations: ", elOracle.getNumberUnsaturations(), verb);
		printStat("\nOntology sizes:", verb);
		printStat("Target TBox logical axioms: ", targetOntology.getAxiomCount(AxiomType.SUBCLASS_OF)+
				targetOntology.getAxiomCount(AxiomType.EQUIVALENT_CLASSES), verb);
		myMetrics.computeTargetSizes(targetOntology);
		myMetrics.computeHypothesisSizes(hypothesisOntology); 
		printStat("Size of T: ", myMetrics.getSizeOfTarget(), verb);
		printStat("Hypothesis TBox logical axioms: ",  hypothesisOntology.getAxiomCount(AxiomType.SUBCLASS_OF)+
				hypothesisOntology.getAxiomCount(AxiomType.EQUIVALENT_CLASSES), verb);
		printStat("Size of H: ", myMetrics.getSizeOfHypothesis(), verb);
        printStat("Number of concept names: ", conceptNumber, verb);
        printStat("Number of role names: ", roleNumber, verb);
        printStat("Size of largest  concept in T: ", myMetrics.getSizeOfTargetLargestConcept(), verb);
		printStat("Size of largest  concept in H: ", myMetrics.getSizeOfHypothesisLargestConcept(), verb);
		printStat("Size of largest  counterexample: ", myMetrics.getSizeOfLargestCounterExample(), verb);
		//printStat("Size of largest  concept (sum conjunctions) in T: ", myMetrics.getSumSizeOfLargestConcept(), verb); 
		 
		System.out.println();
	}

	private void setOracleSkills(String[] args) {

		if (!args[7].equals("0")) {
			oracleMerge = true;
			MERGE_BOUND = Double.parseDouble(args[7]);
		}
		if (!args[8].equals("0")) {
			oracleSaturate = true;
			SATURATION_BOUND = Double.parseDouble(args[8]);
		}
		if (!args[9].equals("0")) {
			oracleBranch = true;
			BRANCH_BOUND = Double.parseDouble(args[9]);
		}
		if (!args[10].equals("0")) {
			oracleUnsaturate = true;
			UNSATURATE_BOUND = Double.parseDouble(args[10]);
		}
		if (!args[11].equals("0")) {
			oracleLeftCompose = true;
			COMPOSE_LEFT_BOUND = Double.parseDouble(args[11]);
		}
		if (!args[12].equals("0")) {
			oracleRightCompose = true;
			COMPOSE_RIGHT_BOUND = Double.parseDouble(args[12]);
		}

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
		// computes inclusions of the form A implies B
		precomputation(elQueryEngineForT, elQueryEngineForH);

		try {
			while (true) {
				myMetrics.setEquivCount(myMetrics.getEquivCount() + 1);

				lastCE = getCounterExample(elQueryEngineForT, elQueryEngineForH);
				int size=myMetrics.getSizeOfCounterexample(lastCE);
				if(size>myMetrics.getSizeOfLargestCounterExample())
					myMetrics.setSizeOfLargestCounterExample(size);
				OWLSubClassOfAxiom counterexample = lastCE;
				OWLClassExpression left = counterexample.getSubClass();
				OWLClassExpression right = counterexample.getSuperClass();
				lastCE = elLearner.decompose(left, right);
 
				if (canTransformELrhs()) {
					lastCE = computeEssentialRightCounterexample();
					Set<OWLSubClassOfAxiom> myAxiomSet = elQueryEngineForH.getOntology()
							.getSubClassAxiomsForSubClass(lastName);
					for (OWLSubClassOfAxiom ax : myAxiomSet) {
						if (ax.getSubClass().getClassExpressionType() == ClassExpressionType.OWL_CLASS) {
							OWLClass cl = (OWLClass) ax.getSubClass();
							if (cl.equals(lastName)) {
								Set<OWLClassExpression> mySet = new HashSet<>();
								mySet.addAll(ax.getSuperClass().asConjunctSet());//done in this way to avoid nesting of intersectionOf
								mySet.addAll(lastExpression.asConjunctSet());
								lastExpression=elQueryEngineForT.getOWLObjectIntersectionOf(mySet);
								lastCE = elQueryEngineForT.getSubClassAxiom(lastName,
										lastExpression);
							}
						}
					}

					lastCE = computeEssentialRightCounterexample();
					 
					addHypothesis(lastCE);
				} else if (canTransformELlhs()) {
					lastCE = computeEssentialLeftCounterexample();
					 
					addHypothesis(lastCE);
				} else {
					addHypothesis(lastCE);
					 
					System.out.println("Not an EL Terminology:" + lastCE.toString());

				}

			}
		} catch (EquivalentException e) {
			// nothing to do: no counterexample has been found
		}

//		lastCE = null;
	}

	private void addHypothesis(OWLAxiom addedAxiom) {

		myManager.addAxiom(hypothesisOntology, addedAxiom);

	}

	private void saveOWLFile(OWLOntology ontology, File file) throws Exception {

		elLearner.minimiseHypothesis(this);
		OWLOntologyFormat format = myManager.getOntologyFormat(ontology);
		ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
		if (format.isPrefixOWLOntologyFormat()) {
			// need to remove prefixes
			manSyntaxFormat.clearPrefixes();
		}

		myManager.saveOntology(ontology, manSyntaxFormat, IRI.create(file.toURI()));
	}

	private Boolean canTransformELrhs() {

		OWLSubClassOfAxiom counterexample = lastCE;
		OWLClassExpression left = counterexample.getSubClass();
		OWLClassExpression right = counterexample.getSuperClass();
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
		OWLClassExpression left = counterexample.getSubClass();
		OWLClassExpression right = counterexample.getSuperClass();
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

	private OWLSubClassOfAxiom computeEssentialLeftCounterexample() throws Exception {
		OWLSubClassOfAxiom axiom = lastCE;

		lastExpression = axiom.getSubClass();
		lastName = (OWLClass) axiom.getSuperClass();

		if (learnerDecompL) {
			axiom = elLearner.decomposeLeft(lastExpression, lastName);

			lastExpression = axiom.getSubClass();
			lastName = (OWLClass) axiom.getSuperClass();
		}

		if (learnerBranch) {
			axiom = elLearner.branchLeft(lastExpression, lastName);
			lastExpression = axiom.getSubClass();
			lastName = (OWLClass) axiom.getSuperClass();
		}

		if (learnerUnsat) {
			axiom = elLearner.unsaturateLeft(lastExpression, lastName);
		}

		return axiom;
	}

	private OWLSubClassOfAxiom computeEssentialRightCounterexample() throws Exception {
		OWLSubClassOfAxiom axiom = lastCE;

		lastName = (OWLClass) axiom.getSubClass();
		lastExpression = axiom.getSuperClass();

		if (learnerDecompR) {//do decomposition before saturation to try to get a smaller tree and do less saturation steps
			axiom = elLearner.decomposeRight(lastName, lastExpression);

			lastName = (OWLClass) axiom.getSubClass();
			lastExpression = axiom.getSuperClass();
		}

		if (learnerSat) {
			axiom = elLearner.saturateRight(lastName, lastExpression);
			lastName = (OWLClass) axiom.getSubClass();
			lastExpression = axiom.getSuperClass();
		}

		if (learnerDecompR) {//do decomposition after saturation to capture the case an inserted concept
							 //allows a subtree to be removed
			axiom = elLearner.decomposeRight(lastName, lastExpression);

			lastName = (OWLClass) axiom.getSubClass();
			lastExpression = axiom.getSuperClass();
		}
		if (learnerMerge) {
			axiom = elLearner.mergeRight(lastName, lastExpression);

		}

		return axiom;
	}

	private void victory() throws Exception {

		// sanity check
		if (!elQueryEngineForH.entailed(axiomsT)) {
			throw new Exception("something went horribly wrong!!!!");
		}

		System.out.println("\nOntology learned successfully!");
		System.out.println("You dun did it!!!");

		axiomsT = new HashSet<>();
		for (OWLAxiom axe : targetOntology.getAxioms())
			if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
					|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
				axiomsT.add(axe);
	}

	private void setupOntologies() {

		try {

			System.out.println("Trying to load targetOntology");
			targetFile = new File(filePath);
			targetOntology = myManager.loadOntologyFromOntologyDocument(targetFile);

			axiomsT = new HashSet<>();
			axiomsTtmp = new HashSet<>();
			for (OWLAxiom axe : targetOntology.getAxioms())
				// removed !axe.toString().contains("Thing") &&
				if (axe.isOfType(AxiomType.SUBCLASS_OF) || axe.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
					axiomsT.add(axe);
					axiomsTtmp.add(axe);
				}

			lastCE = null;

			// transfer Origin targetOntology to ManchesterOWLSyntaxOntologyFormat
			OWLOntologyFormat format = myManager.getOntologyFormat(targetOntology);
			ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
			if (format.isPrefixOWLOntologyFormat()) {
				manSyntaxFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
			}

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

			System.out.println(targetOntology);
			System.out.println("Loaded successfully.");
			System.out.println();

			ArrayList<String> concepts = myMetrics.getSuggestionNames("concept", newFile);

			ArrayList<String> roles = myMetrics.getSuggestionNames("role", newFile);

			this.conceptNumber = concepts.size();
			this.roleNumber = roles.size();

			//System.out.println("Total number of concepts is: " + concepts.size());
			//System.out.println("Total number of roles is: " + roles.size());
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

	// --Commented out by Inspection START (30/04/2018, 15:27):
	// private Boolean equivalenceQuery() {
	//
	// return elQueryEngineForH.entailed(axiomsTtmp);
	// }
	// --Commented out by Inspection STOP (30/04/2018, 15:27)

	private OWLSubClassOfAxiom getCounterExample(ELEngine elQueryEngineForT, ELEngine elQueryEngineForH)
			throws Exception {
		// necessary to avoid Concurrent Modification Exception
		// Set<OWLAxiom> tmp = new HashSet<>(axiomsTtmp);

		Iterator<OWLAxiom> iterator = axiomsTtmp.iterator();
		// for (OWLAxiom selectedAxiom : tmp) {
		while (iterator.hasNext()) {
			OWLAxiom selectedAxiom = iterator.next();
			selectedAxiom.getAxiomType();

			// first get CounterExample from an axiom with the type SUBCLASS_OF
			if (selectedAxiom.isOfType(AxiomType.SUBCLASS_OF)) {
				if (!elQueryEngineForH.entailed(selectedAxiom)) {

					OWLSubClassOfAxiom counterexample = (OWLSubClassOfAxiom) selectedAxiom;

					return getCounterExampleSubClassOf(elQueryEngineForT, elQueryEngineForH, counterexample);
				}
				// axiomsTtmp.remove(selectedAxiom);
				iterator.remove();
			}
			if (selectedAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
				OWLEquivalentClassesAxiom equivCounterexample = (OWLEquivalentClassesAxiom) selectedAxiom;
				Set<OWLSubClassOfAxiom> eqsubclassaxioms = equivCounterexample.asOWLSubClassOfAxioms();

				for (OWLSubClassOfAxiom subClassAxiom : eqsubclassaxioms) {
					if (!elQueryEngineForH.entailed(subClassAxiom)) {

						return getCounterExampleSubClassOf(elQueryEngineForT, elQueryEngineForH, subClassAxiom);
					}
				}
				// axiomsTtmp.remove(selectedAxiom);
				iterator.remove();
			}

		}
		throw new EquivalentException();
	}

	private OWLSubClassOfAxiom getCounterExampleSubClassOf(ELEngine elQueryEngineForT, ELEngine elQueryEngineForH,
			OWLSubClassOfAxiom counterexample) throws Exception {
		OWLSubClassOfAxiom newCounterexampleAxiom = counterexample;
		OWLClassExpression left = counterexample.getSubClass();
		OWLClassExpression right = counterexample.getSuperClass();

		if (oracleMerge) {
			newCounterexampleAxiom = elOracle.mergeLeft(left, right, MERGE_BOUND);
			left = newCounterexampleAxiom.getSubClass();
			right = newCounterexampleAxiom.getSuperClass();
		}

		if (oracleSaturate) {
			newCounterexampleAxiom = elOracle.saturateLeft(left, right, SATURATION_BOUND);
			left = newCounterexampleAxiom.getSubClass();
			right = newCounterexampleAxiom.getSuperClass();
		}

		if (oracleBranch) {
			newCounterexampleAxiom = elOracle.branchRight(left, right, BRANCH_BOUND);
			left = newCounterexampleAxiom.getSubClass();
			right = newCounterexampleAxiom.getSuperClass();
		}

		if (oracleLeftCompose) {
			newCounterexampleAxiom = elOracle.composeLeft(left, right, COMPOSE_LEFT_BOUND);
			left = newCounterexampleAxiom.getSubClass();
			right = newCounterexampleAxiom.getSuperClass();
		}

		if (oracleRightCompose) {
			newCounterexampleAxiom = elOracle.composeRight(left, right, COMPOSE_RIGHT_BOUND);
			left = newCounterexampleAxiom.getSubClass();
			right = newCounterexampleAxiom.getSuperClass();
		}

		if (oracleUnsaturate) {
			newCounterexampleAxiom = elOracle.unsaturateRight(left, right, UNSATURATE_BOUND);
		}

		return newCounterexampleAxiom;
	}

	private void precomputation(ELEngine elQueryEngineForT, ELEngine elQueryEngineForH) {
		int i= elQueryEngineForT.getClassesInSignature().size();
		myMetrics.setMembCount(myMetrics.getMembCount() +  i*(i-1));
		for (OWLClass cl1 : elQueryEngineForT.getClassesInSignature()) {
			Set<OWLClass> implied = elQueryEngineForT.getSuperClasses(cl1, true);
			for (OWLClass cl2 : implied) {		 
					OWLSubClassOfAxiom addedAxiom = elQueryEngineForT.getSubClassAxiom(cl1, cl2);
					addHypothesis(addedAxiom);
			}
		}
		
	}
}
