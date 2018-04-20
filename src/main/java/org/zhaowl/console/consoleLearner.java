package org.zhaowl.console;

import org.slf4j.LoggerFactory;
import org.zhaowl.utils.Metrics;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

//import javax.swing.JOptionPane;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.zhaowl.engine.ELEngine;
import org.zhaowl.learner.ELLearner;
import org.zhaowl.oracle.ELOracle;
import org.apache.log4j.*;

public class consoleLearner {

	// ############# Game variables Start ######################

	public int membCount = 0;
	public int equivCount = 0;
	public long timeStart = 0;
	public long timeEnd = 0;
	//public boolean win;
	public boolean wePlayin;
	public boolean ezBox;
	public boolean autoBox = true;
	public String filePath;

	// ############# Game variables End ######################
	
	// #########################################################
	
	// ############# OWL variables Start ######################

	private String ontologyPath = null;
	private OWLOntologyManager myManager = null;
	private ManchesterOWLSyntaxOWLObjectRendererImpl myRenderer = null;


	private Set<OWLAxiom> axiomsT = null;
	private Set<OWLAxiom> axiomsTCheck = null;

	private String ontologyFolder = null;
	private String ontologyName = null;
	private File hypoFile = null;
	private File newFile = null;

	public ArrayList<String> concepts = new ArrayList<String>();
	private ArrayList<String> roles = new ArrayList<String>();

	private Set<OWLClass> cIo = null;

	private Set<OWLAxiom> axiomsH = null;
	private String ontologyFolderH = null;

	private OWLAxiom lastCE = null;

	private OWLAxiom smallestOne = null;
	private int smallestSize = 0;

	private OWLOntology targetOntology = null;
	private OWLOntology hypothesisOntology = null;

	private ELEngine elQueryEngineForT = null;
	private ELEngine elQueryEngineForH = null;

	private ELLearner elLearner = null;
	private ELOracle elOracle = null;

	// ############# OWL variables End ######################

	// #########################################################
	
	// ############# Oracle and Learner skills Start ######################

	public boolean oracleSaturate;
	public boolean oracleMerge;
	public boolean oracleBranch;
	public boolean oracleUnsaturate;

	public boolean learnerSat;
	public boolean learnerMerge;
	public boolean learnerDecompL;
	public boolean learnerUnsat;
	public boolean learnerBranch;
	public boolean learnerDecompR;

	private static final org.slf4j.Logger LOGGER_ = LoggerFactory
			.getLogger(consoleLearner.class);

	// ############# Oracle and Learner skills End ######################

	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.OFF);
		consoleLearner maker = new consoleLearner();
		maker.doIt(args);

	}

	public void doIt(String[] args) {

		try {
			// targetOntology from parameters
			filePath = args[0];

			// normal mode allows for elOracle skills
			if (args[1].equals("on"))
				ezBox = true; // elOracle skills not allowed
			else
				ezBox = false; // elOracle skills allowed

			// setLearnerSkills
			setLearnerSkills(args);

			// setLearnerSkills
			setOracleSkills(args);
			// load targetOntology

			try {
				setupOntologies();

				elQueryEngineForT = new ELEngine(targetOntology);
				elQueryEngineForH = new ELEngine(hypothesisOntology);

				elLearner = new ELLearner(elQueryEngineForT, elQueryEngineForH, this);
				elOracle = new ELOracle(elQueryEngineForT, elQueryEngineForH, this);


				timeStart = System.currentTimeMillis();
 
				runLearner();
				System.out.println("Total membership queries: " + membCount);
				System.out.println("Total equivalence queries: " + equivCount);
				System.out.println("Target TBox logical axioms: " + axiomsT.size());
				
				
				
				new Metrics(myRenderer).showCIT(axiomsT,true);
				
				System.out.println("Hypothesis TBox logical axioms: " + hypothesisOntology.getAxioms().size());
				new Metrics(myRenderer).showCIT(hypothesisOntology.getAxioms(),false);

			} catch (Throwable e) {
				e.printStackTrace();
				System.out.println("error in runLearner call ----- " + e);
			}

		} catch (Throwable e) {
			// TODO Auto-generated catch block
			System.out.println("error in doIt --- " + e);
		}

	}

	public void setOracleSkills(String[] args) {
		if (args[8].equals("t"))
			oracleMerge = true;
		else
			oracleMerge = false;

		if (args[9].equals("t"))
			oracleSaturate = true;
		else
			oracleSaturate = false;

		if (args[10].equals("t"))
			oracleBranch = true;
		else
			oracleBranch = false;

		if (args[11].equals("t"))
			oracleUnsaturate = true;
		else
			oracleUnsaturate = false;
	}

	public void setLearnerSkills(String[] args) {

		if (args[2].equals("t"))
			learnerDecompL = true;
		else
			learnerDecompL = false;

		if (args[3].equals("t"))
			learnerBranch = true;
		else
			learnerBranch = false;

		if (args[4].equals("t"))
			learnerUnsat = true;
		else
			learnerUnsat = false;

		if (args[5].equals("t"))
			learnerDecompR = true;
		else
			learnerDecompR = false;

		if (args[6].equals("t"))
			learnerMerge = true;
		else
			learnerMerge = false;

		if (args[7].equals("t"))
			learnerSat = true;
		else
			learnerSat = false;

	}

	public void runLearner() throws Throwable {

		
		if (autoBox) {  

			if (equivalenceQuery()) {
				victory();
				timeEnd = System.currentTimeMillis();
				System.out.println("Total time (ms): " + (timeEnd - timeStart));
				lastCE = null;
				return;
			} else if (ezBox) {
				equivCount++;
				ezEq();
			} else {
				equivCount++;
				doCE();
			}
			// System.out.println(myRenderer.render(lastCE));

			OWLClassExpression left = null;
			OWLClassExpression right = null;
			// lastCE is last counter example provided by elOracle, unsaturate and saturate
			if (lastCE.isOfType(AxiomType.SUBCLASS_OF)) {
				left = ((OWLSubClassOfAxiom) lastCE).getSubClass();
				right = ((OWLSubClassOfAxiom) lastCE).getSuperClass();
			} else {

				runLearner();

				return;

			}
			lastCE = elQueryEngineForT.getSubClassAxiom(left, right);
			// check if complex side is left
			if (checkLeft(lastCE)) {

				// decompose tries to find underlying inclusions inside the left hand side
				// by recursively breaking the left expression and adding new inclusions to the
				// hypothesis
				/*
				 * if(oracleMerge.isSelected()) elOracle.oracleSiblingMerge(left, right);
				 * if(oracleSaturate.isSelected()) elOracle.saturateWithTreeLeft(lastCE);
				 */
				if (learnerDecompL) {
					// System.out.println("lhs decomp");
					elLearner.decompose(left, right);
				}
				// branch edges on left side of the inclusion (if possible) to make it logically
				// stronger (more general)
				if (learnerBranch) {
					// System.out.println("lhs branch");
					left = elLearner.branchLeft(left, right);
				}
				lastCE = elQueryEngineForT.getSubClassAxiom(left, right);

				// unsaturate removes useless concepts from nodes in the inclusion
				if (learnerUnsat) {
					// System.out.println("lhs unsaturate");

					left = elLearner.unsaturateLeft(lastCE);
				}
				lastCE = elQueryEngineForT.getSubClassAxiom(left, right);
				try {
					addHypothesis(lastCE); 
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			} else {
				// decompose tries to find underlying inclusions inside the right hand side
				// by recursively breaking the left expression and adding new inclusions to the
				// hypothesis
				if (learnerDecompR) {
					// System.out.println("rhs decomp");
					elLearner.decompose(left, right);
				}
				// merge edges on right side of the inclusion (if possible) to make it logically
				// stronger (more general)
				if (learnerMerge) {
					// System.out.println("rhs merge");
					right = elLearner.learnerSiblingMerge(left, right);
				}
				// rebuild inclusion for final step
				lastCE = elQueryEngineForT.getSubClassAxiom(left, right);
				if (learnerSat) {
					// System.out.println("rhs saturate");
					lastCE = elLearner.saturateWithTreeRight(lastCE);
				}
				left = ((OWLSubClassOfAxiom) lastCE).getSubClass();
				right = ((OWLSubClassOfAxiom) lastCE).getSuperClass();
				try {
					addHypothesis(lastCE);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
			runLearner();
		}

	}

	public void equivalenceCheck() {

		int x = 0;
		if (!wePlayin) {}
			//JOptionPane.showMessageDialog(null, "No Ontology loaded yet, please load an Ontology to start playing!",
					//"Alert", JOptionPane.INFORMATION_MESSAGE);
		else {
			if (autoBox) {
				System.gc();
				boolean check = equivalenceQuery();
				do {
					equivCount++;
					x++;
					if (check) {
						// victory
						victory();
						System.out.println("It took: " + x);
						System.out.flush();
					} else {
						// generate counter example
						System.out.flush();
						doCE();
					}
				} while (!equivalenceQuery());
			} else {
				boolean check = equivalenceQuery();
				equivCount++;
				if (check) {
					// victory
					victory();
				} else {
					// generate counter example
					doCE();
				}
			}
		}

	}

	public void doCE() {
		String counterEx = "";
		System.out.println("Generating counterexample... ");
		try {
			counterEx = getCounterExample();

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println(counterEx);
	}

	public void victory() {
		//win = true;
		System.out.println("Ontology learned successfully!");
		System.out.println("You dun did it!!!");
		
		axiomsT = new HashSet<OWLAxiom>();
		for (OWLAxiom axe : targetOntology.getAxioms())
			if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
					|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
				axiomsT.add(axe);
	}

	public void setupOntologies() throws InterruptedException {
		//win = false;

		try {
			myManager = OWLManager.createOWLOntologyManager();
			System.out.println("Trying to load targetOntology");
			targetOntology = myManager.loadOntologyFromOntologyDocument(new File(filePath));
			myRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

			axiomsT = new HashSet<OWLAxiom>();
			for (OWLAxiom axe : targetOntology.getAxioms())
				if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
						|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
					axiomsT.add(axe);
			axiomsTCheck = new HashSet<OWLAxiom>();
			for (OWLAxiom axe : targetOntology.getAxioms())
				if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
						|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
					axiomsTCheck.add(axe);

			lastCE = null;


			// transfer Origin targetOntology to ManchesterOWLSyntaxOntologyFormat
			OWLOntologyFormat format = myManager.getOntologyFormat(targetOntology);
			ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
			if (format.isPrefixOWLOntologyFormat()) {
				manSyntaxFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
			}
			format = null;
			
			// create personalized names for targetOntology
			ontologyFolderH = "src/main/resources/tmp/";
			ontologyFolder = "src/main/resources/tmp/";
			ontologyName = "";
			getOntologyName();

			{ // save ontologies
				newFile = new File(ontologyFolder);
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
			}


			axiomsH = hypothesisOntology.getAxioms();
			wePlayin = true;



			System.out.println(targetOntology);
			System.out.println("Loaded successfully.");
			System.out.println();

			concepts = new Metrics(myRenderer).getSuggestionNames("concept", newFile);
			roles = new Metrics(myRenderer).getSuggestionNames("role", newFile);

			System.out.println("Total number of concepts is: " + concepts.size());


			
			System.out.flush();
		} catch (OWLOntologyCreationException e) {
			System.out.println("Could not load targetOntology: " + e.getMessage());
		} catch (OWLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void ezEq() {
		if (equivalenceQuery()) {
			victory();
			return;
		}

		for (OWLAxiom ax : axiomsTCheck) {
			if (ax.toString().contains("Thing"))
				continue;
			if (!axiomsH.contains(ax)) {
				try {
					addHypothesis(ax);
					lastCE = ax; 
					axiomsTCheck.remove(ax);
					axiomsH.add(ax);
					break;
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}else
				System.out.println("Won't add " + myRenderer.render(ax));
		} 
	}
	
	public void getOntologyName() {

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

	public void resetVariables() {
		ontologyPath = null;
		myManager = null;
		myRenderer = null;

		axiomsT = null;
		axiomsTCheck = null;
		elQueryEngineForT = null;
		ontologyFolder = null;
		ontologyName = null;
		hypoFile = null;
		newFile = null;

		concepts = new ArrayList<String>();
		roles = new ArrayList<String>();

		cIo = null;


		axiomsH = null;
		ontologyFolderH = null;
		targetOntology = null;
		hypothesisOntology = null;
		lastCE = null;

		smallestOne = null;
		smallestSize = 0;
	}

	public Boolean equivalenceQuery() {

		Boolean queryAns = elQueryEngineForH.entailed(axiomsT);
		return queryAns;
	}



	public String getCounterExample() throws Exception {
		Iterator<OWLAxiom> iteratorT = axiomsT.iterator();
		while (iteratorT.hasNext()) {
			OWLAxiom selectedAxiom = iteratorT.next();
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

					OWLAxiom newCounterexampleAxiom = getCounterExamplefromSubClassAxiom(subclass, superclass);
					if (newCounterexampleAxiom != null) {
						// if we actually got something, we use it as new counter example

						// System.out.println("subclass 1");
						// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
						// ADD SATURATION FOR newCounterexampleAxiom HERE
						OWLClassExpression ex = null;
						if (checkLeft(newCounterexampleAxiom)) {
							if (oracleMerge) {
								ex = null;
								// System.out.println(newCounterexampleAxiom);
								// if (checkLeft(newCounterexampleAxiom)) {
								ex = elOracle.oracleSiblingMerge(
										((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(),
										((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
								newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(ex, superclass);
								ex = null;
							}
							if (oracleSaturate)
								newCounterexampleAxiom = elOracle
										.saturateWithTreeLeft((OWLSubClassOfAxiom) newCounterexampleAxiom);
						} else {

							if (oracleBranch) {
								ex = null;
								OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) newCounterexampleAxiom;
								ex = elOracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
								newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(auxAx.getSubClass(), ex);
								auxAx = null;
								ex = null;
							}
							if (oracleUnsaturate) {
								ex = null;
								ex = elOracle.unsaturateRight(newCounterexampleAxiom);
								newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(subclass, ex);
								ex = null;
							}
						}
						/*
						 * } else { ex = siblingMerge(((OWLSubClassOfAxiom)
						 * newCounterexampleAxiom).getSuperClass()); newCounterexampleAxiom =
						 * saturateWithTreeRight( elQueryEngineForT.getSubClassAxiom(subclass, ex)); }
						 */

						// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
						lastCE = newCounterexampleAxiom;
						subclass = null;
						superclass = null;
						counterexample = null;
						selectedAxiom = null;
						iteratorT = null;
						return addHypothesis(newCounterexampleAxiom);
					}
				}
			}

			// get CounterExample from an axiom with the type EQUIVALENT_CLASSES
			if (selectedAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {

				OWLEquivalentClassesAxiom counterexample = (OWLEquivalentClassesAxiom) selectedAxiom;
				Set<OWLSubClassOfAxiom> eqsubclassaxioms = counterexample.asOWLSubClassOfAxioms();
				Iterator<OWLSubClassOfAxiom> iterator = eqsubclassaxioms.iterator();

				while (iterator.hasNext()) {
					OWLSubClassOfAxiom subClassAxiom = iterator.next();

					OWLClassExpression subclass = subClassAxiom.getSubClass();

					Set<OWLClass> superclasses = elQueryEngineForT.getSuperClasses(subclass, true);
					if (!superclasses.isEmpty()) {
						Iterator<OWLClass> iteratorSuperClass = superclasses.iterator();
						while (iteratorSuperClass.hasNext()) {
							OWLClassExpression SuperclassInSet = iteratorSuperClass.next();
							OWLAxiom newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(subclass,
									SuperclassInSet);
							Boolean querySubClass = elQueryEngineForH.entailed(newCounterexampleAxiom);
							Boolean querySubClassforT = elQueryEngineForT.entailed(newCounterexampleAxiom);
							if (!querySubClass && querySubClassforT) {

								// System.out.println("eq 1");
								// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
								// ADD SATURATION FOR newCounterexampleAxiom HERE
								OWLClassExpression ex = null;
								if (checkLeft(newCounterexampleAxiom)) {
									if (oracleMerge) {

										ex = null;
										// System.out.println(newCounterexampleAxiom);
										// if (checkLeft(newCounterexampleAxiom)) {
										ex = elOracle.oracleSiblingMerge(
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(),
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
										newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(ex,
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
										ex = null;
									}
									if (oracleSaturate)
										newCounterexampleAxiom = elOracle
												.saturateWithTreeLeft((OWLSubClassOfAxiom) newCounterexampleAxiom);
								} else {
									if (oracleBranch) {
										ex = null;
										OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) newCounterexampleAxiom;
										ex = elOracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
										newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(auxAx.getSubClass(),
												ex);
										auxAx = null;
										ex = null;
									}
									if (oracleUnsaturate) {
										ex = null;
										ex = elOracle.unsaturateRight(newCounterexampleAxiom);
										newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(), ex);
										ex = null;
									}
								}
								// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
								lastCE = newCounterexampleAxiom;
								subclass = null;
								SuperclassInSet = null;
								superclasses = null;
								counterexample = null;
								subClassAxiom = null;
								selectedAxiom = null;
								iteratorT = null;
								System.out.flush();
								return addHypothesis(newCounterexampleAxiom);
							}
						}
					}

				}
			}
		}

		Iterator<OWLAxiom> iterator = axiomsT.iterator();

		while (iterator.hasNext()) {
			OWLAxiom Axiom = iterator.next();

			Axiom.getAxiomType();
			if ((Axiom.isOfType(AxiomType.SUBCLASS_OF)) || (Axiom.isOfType(AxiomType.EQUIVALENT_CLASSES))) {

				Axiom.getAxiomType();
				if (Axiom.isOfType(AxiomType.SUBCLASS_OF)) {
					OWLSubClassOfAxiom selectedAxiom = (OWLSubClassOfAxiom) Axiom;
					Boolean queryAns = elQueryEngineForH.entailed(selectedAxiom);

					if (!queryAns) {
						lastCE = selectedAxiom;
						// System.out.println("subclass 2");
						// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
						// ADD SATURATION FOR Axiom HERE
						OWLClassExpression ex = null;
						if (checkLeft(selectedAxiom)) {
							if (oracleMerge) {
								ex = null;
								// System.out.println(newCounterexampleAxiom);
								// if (checkLeft(newCounterexampleAxiom)) {
								ex = elOracle.oracleSiblingMerge(((OWLSubClassOfAxiom) selectedAxiom).getSubClass(),
										((OWLSubClassOfAxiom) selectedAxiom).getSuperClass());
								selectedAxiom = (OWLSubClassOfAxiom) elQueryEngineForT.getSubClassAxiom(ex,
										((OWLSubClassOfAxiom) selectedAxiom).getSuperClass());
								ex = null;
							}
							if (oracleSaturate)
								selectedAxiom = (OWLSubClassOfAxiom) elOracle
										.saturateWithTreeLeft((OWLSubClassOfAxiom) selectedAxiom);
						} else {

							if (oracleBranch) {
								ex = null;
								OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) selectedAxiom;
								ex = elOracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
								selectedAxiom = (OWLSubClassOfAxiom) elQueryEngineForT
										.getSubClassAxiom(auxAx.getSubClass(), ex);
								auxAx = null;
								ex = null;
							}
							if (oracleUnsaturate) {
								ex = null;
								ex = elOracle.unsaturateRight((OWLSubClassOfAxiom) selectedAxiom);
								selectedAxiom = (OWLSubClassOfAxiom) elQueryEngineForT
										.getSubClassAxiom(((OWLSubClassOfAxiom) selectedAxiom).getSubClass(), ex);
								ex = null;
							}

						}
						// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
						lastCE = selectedAxiom;

						Axiom = null;
						iterator = null;
						iteratorT = null;
						System.out.flush();
						return addHypothesis((OWLSubClassOfAxiom) selectedAxiom);
					}
				}

				if (Axiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
					OWLEquivalentClassesAxiom counterexample = (OWLEquivalentClassesAxiom) Axiom;

					Set<OWLSubClassOfAxiom> eqsubclassaxioms = counterexample.asOWLSubClassOfAxioms();
					Iterator<OWLSubClassOfAxiom> iteratorAsSub = eqsubclassaxioms.iterator();
					while (iteratorAsSub.hasNext()) {
						OWLSubClassOfAxiom subClassAxiom = iteratorAsSub.next();
						Boolean queryAns = elQueryEngineForH.entailed(subClassAxiom);
						if (!queryAns) {
							lastCE = subClassAxiom;
							// System.out.println("eqcl 2");
							// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
							// ADD SATURATION FOR subClassAxiom HERE
							OWLClassExpression ex = null;

							if (checkLeft(subClassAxiom)) {
								if (oracleMerge) {
									ex = null;
									// System.out.println(newCounterexampleAxiom);
									// if (checkLeft(newCounterexampleAxiom)) {
									ex = elOracle.oracleSiblingMerge(((OWLSubClassOfAxiom) subClassAxiom).getSubClass(),
											((OWLSubClassOfAxiom) subClassAxiom).getSuperClass());
									subClassAxiom = (OWLSubClassOfAxiom) elQueryEngineForT.getSubClassAxiom(ex,
											((OWLSubClassOfAxiom) subClassAxiom).getSuperClass());
									ex = null;
								}
								if (oracleSaturate)
									subClassAxiom = (OWLSubClassOfAxiom) elOracle
											.saturateWithTreeLeft((OWLSubClassOfAxiom) subClassAxiom);
							} else {
								if (oracleBranch) {
									ex = null;
									OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) subClassAxiom;
									ex = elOracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
									subClassAxiom = (OWLSubClassOfAxiom) elQueryEngineForT
											.getSubClassAxiom(auxAx.getSubClass(), ex);
									auxAx = null;
									ex = null;
								}
								if (oracleUnsaturate) {
									ex = null;
									ex = elOracle.unsaturateRight((OWLSubClassOfAxiom) subClassAxiom);
									subClassAxiom = (OWLSubClassOfAxiom) elQueryEngineForT
											.getSubClassAxiom(((OWLSubClassOfAxiom) subClassAxiom).getSubClass(), ex);
									ex = null;
								}
							}
							// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*?*-*-*-*-*-*-*-*
							lastCE = subClassAxiom;
							Axiom = null;
							iterator = null;
							iteratorAsSub = null;
							iteratorT = null;
							System.out.flush();
							return addHypothesis(subClassAxiom);
						}
					}
				}
			}
		}
		System.out.println("no more CIs");
		iterator = null;
		iteratorT = null;
		System.out.flush();
		return null;
	}

	public boolean checkLeft(OWLAxiom axiom) {

		String left = myRenderer.render(((OWLSubClassOfAxiom) axiom).getSubClass());
		String right = myRenderer.render(((OWLSubClassOfAxiom) axiom).getSuperClass());
		for (String rol : roles) {
			if (left.contains(rol)) {
				return true;
			} else if (right.contains(rol)) {
				return false;
			} else
				continue;
		}
		return true;
	}

	private OWLAxiom getCounterExamplefromSubClassAxiom(OWLClassExpression subclass, OWLClassExpression superclass) {
		Set<OWLClass> superclasses = elQueryEngineForT.getSuperClasses(superclass, false);
		Set<OWLClass> subclasses = elQueryEngineForT.getSubClasses(subclass, false);

		if (!subclasses.isEmpty()) {

			Iterator<OWLClass> iteratorSubClass = subclasses.iterator();
			while (iteratorSubClass.hasNext()) {
				OWLClassExpression SubclassInSet = iteratorSubClass.next();
				OWLAxiom newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(SubclassInSet, superclass);
				Boolean querySubClass = elQueryEngineForH.entailed(newCounterexampleAxiom);
				Boolean querySubClassforT = elQueryEngineForT.entailed(newCounterexampleAxiom);
				if (!querySubClass && querySubClassforT) {
					SubclassInSet = null;
					superclass = null;
					iteratorSubClass = null;
					elQueryEngineForH = null;

					return newCounterexampleAxiom;
				}
			}
		}
		if (!superclasses.isEmpty()) {

			Iterator<OWLClass> iteratorSuperClass = superclasses.iterator();
			while (iteratorSuperClass.hasNext()) {
				OWLClassExpression SuperclassInSet = iteratorSuperClass.next();
				OWLAxiom newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(subclass, SuperclassInSet);
				Boolean querySubClass = elQueryEngineForH.entailed(newCounterexampleAxiom);
				Boolean querySubClassforT = elQueryEngineForT.entailed(newCounterexampleAxiom);
				if (!querySubClass && querySubClassforT) {

					SuperclassInSet = null;
					superclass = null;
					subclass = null;
					iteratorSuperClass = null;
					elQueryEngineForH = null;

					return newCounterexampleAxiom;
				}
			}
		}

		elQueryEngineForH = null;
		superclass = null;
		subclass = null;
		return null;
	}

	public String addHypothesis(OWLAxiom addedAxiom) throws Exception {
		String StringAxiom = myRenderer.render(addedAxiom);

		//AddAxiom newAxiomInH = new AddAxiom(hypothesisOntology, addedAxiom);
		//myManager.applyChange(newAxiomInH);

		myManager.addAxiom(hypothesisOntology, addedAxiom);

		saveOWLFile(hypothesisOntology, hypoFile);

		// minimize hypothesis
		hypothesisOntology = MinHypothesis(hypothesisOntology, addedAxiom);
		saveOWLFile(hypothesisOntology, hypoFile);
		//newAxiomInH = null;
		addedAxiom = null;
		return StringAxiom;
	}

	private void saveOWLFile(OWLOntology ontology, File file) throws Exception {

		OWLOntologyFormat format = myManager.getOntologyFormat(ontology);
		ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
		if (format.isPrefixOWLOntologyFormat()) {
			// need to remove prefixes
			manSyntaxFormat.clearPrefixes();
		}
		format = null;
		myManager.saveOntology(ontology, manSyntaxFormat, IRI.create(file.toURI()));
	}

	private OWLOntology MinHypothesis(OWLOntology hypoOntology, OWLAxiom addedAxiom) {
		Set<OWLAxiom> tmpaxiomsH = hypoOntology.getAxioms();
		Iterator<OWLAxiom> ineratorMinH = tmpaxiomsH.iterator();
		Set<OWLAxiom> checkedAxiomsSet = new HashSet<OWLAxiom>();
		String removedstring = "";
		Boolean flag = false;
		if (tmpaxiomsH.size() > 1) {
			while (ineratorMinH.hasNext()) {
				OWLAxiom checkedAxiom = ineratorMinH.next();
				if (!checkedAxiomsSet.contains(checkedAxiom)) {
					checkedAxiomsSet.add(checkedAxiom);

					OWLOntology tmpOntologyH = hypoOntology;
					RemoveAxiom removedAxiom = new RemoveAxiom(tmpOntologyH, checkedAxiom);
					myManager.applyChange(removedAxiom);

					ELEngine tmpELQueryEngine = new ELEngine(tmpOntologyH);
					Boolean queryAns = tmpELQueryEngine.entailed(checkedAxiom);
					tmpELQueryEngine.disposeOfReasoner();

					if (queryAns) {
						RemoveAxiom removedAxiomFromH = new RemoveAxiom(hypoOntology, checkedAxiom);
						myManager.applyChange(removedAxiomFromH);
						removedstring = "\t[" + myRenderer.render(checkedAxiom) + "]\n";
						if (checkedAxiom.equals(addedAxiom)) {
							flag = true;
						}
					} else {
						AddAxiom addAxiomtoH = new AddAxiom(hypoOntology, checkedAxiom);
						myManager.applyChange(addAxiomtoH);
						addAxiomtoH = null;
					}
				}
			}
			if (!removedstring.equals("")) {
				String message;
				if (flag) {
					message = "The axiom [" + myRenderer.render(addedAxiom) + "] will not be added to the hypothesis\n"
							+ "since it can be replaced by some axiom(s) that already exist in the hypothesis.";
				} else {
					message = "The axiom [" + removedstring + "]" + "will be removed after adding: \n["
							+ myRenderer.render(addedAxiom) + "]";
				}
				// System.out.println(message);
				// JOptionPane.showMessageDialog(null, message, "Alert",
				// JOptionPane.INFORMATION_MESSAGE);
			}
		}
		tmpaxiomsH = null;
		ineratorMinH = null;
		checkedAxiomsSet = null;

		return hypoOntology;
	}

}
