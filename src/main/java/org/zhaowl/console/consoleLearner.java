package org.zhaowl.console;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.zhaowl.userInterface.ELEngine;
import org.zhaowl.utils.SimpleClass;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JOptionPane;

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
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.zhaowl.learner.ELLearner;
import org.zhaowl.oracle.ELOracle;
import org.apache.log4j.*;

public class consoleLearner {

	// ############# Game variables Start ######################

	public int membCount = 0;
	public int equivCount = 0;
	public long timeStart = 0;
	public long timeEnd = 0;
	public boolean win;
	public boolean wePlayin;
	public boolean ezBox;
	public boolean autoBox = true;
	public String filePath;

	// ############# Game variables Start ######################
	
	// #########################################################
	
	// ############# OWL variables Start ######################

	public String ontologyPath = null;
	public OWLOntologyManager manager = null;
	public ManchesterOWLSyntaxOWLObjectRendererImpl rendering = null;

	public OWLReasoner reasonerForT = null;
	public Set<OWLAxiom> axiomsT = null;
	public Set<OWLAxiom> axiomsTCheck = null;
	public ELEngine ELQueryEngineForT = null;
	public String ontologyFolder = null;
	public String ontologyName = null;
	public File hypoFile = null;
	public File newFile = null;

	public ArrayList<String> concepts = new ArrayList<String>();
	public ArrayList<String> roles = new ArrayList<String>();

	public Set<OWLClass> cIo = null;

	public OWLReasoner reasonerForH = null;
	public ShortFormProvider shortFormProvider = null;
	public Set<OWLAxiom> axiomsH = null;
	public String ontologyFolderH = null;
	public OWLOntology ontology = null;
	public OWLOntology ontologyH = null;
	public OWLAxiom lastCE = null;

	public OWLAxiom smallestOne = null;
	public int smallestSize = 0;

	// ############# OWL variables Start ######################

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

	// ############# Oracle and Learner skills END ######################

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		/*
		 * ----- [0] ontology path if jar file is in project folder [ZhaOWL] then you
		 * specify the small ontology for animals as
		 * 
		 * src/main/resources/ontologies/SMALL/animals.owl
		 * 
		 * [1] = mode, if "on" then ez if "off" then normal mode AND allows for oracle
		 * 
		 * args[2:7] = learner skills [2] = decompose left [3] = branch left [4] =
		 * unsaturate left [5] = decompose right [6] = merge right [7] = saturate right
		 * 
		 * [8] = saturate left [9] = merge left [10] = branch right [11] = unsaturate
		 * right
		 * 
		 * 
		 * ----- OUTPUT aside from some console metrics (number of equivalence queries
		 * and some other info) a new ontology file will be created in the folder of
		 * ontology input this new ontology will be the hypothesis learned by the
		 * program
		 * 
		 */
		Logger.getRootLogger().setLevel(Level.ALL);
		consoleLearner maker = new consoleLearner();
		// maker.setValues(args);
		maker.doIt(args);

	}

	public void doIt(String[] args) {
		/*
		// FOR TESTING 
		args = new String[12];
		args[0] = "src/main/resources/ontologies/university.owl";
		args[1] = "on";
		for(int i = 2; i < 8; i++)
			args[i] = "t";
		for(int i = 8; i < 12; i++)
			args[i] = "f";
		*/
		try {
			// ontology from parameters
			filePath = args[0];

			// normal mode allows for oracle skills
			if (args[1].equals("on"))
				ezBox = true; // oracle skills not allowed
			else
				ezBox = false; // oracle skills allowed

			// setLearnerSkills
			setLearnerSkills(args);

			// setLearnerSkills
			setOracleSkills(args);
			// load ontology

			try {
				loadOntology();
				timeStart = System.currentTimeMillis();
 
				learner();
				System.out.println("Total membership queries: " + membCount);
				System.out.println("Total equivalence queries: " + equivCount);
				System.out.println("Target TBox logical axioms: " + axiomsT.size());
				
				
				
				new SimpleClass(rendering).showCIT(axiomsT,true);
				
				System.out.println("Hypothesis TBox logical axioms: " + ontologyH.getAxioms().size());
				new SimpleClass(rendering).showCIT(ontologyH.getAxioms(),false);

			} catch (Throwable e) {
				e.printStackTrace();
				System.out.println("error in learner call ----- " + e);
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

	public void learner() throws Throwable {
		ELLearner learner = new ELLearner(reasonerForH, shortFormProvider, ontology, ontologyH, ELQueryEngineForT, this,
				rendering);

		
		if (autoBox) {  

			if (equivalenceQuery()) {
				victory();
				timeEnd = System.currentTimeMillis();
				System.out.println("Total time (ms): " + (timeEnd - timeStart));
				lastCE = null;
				learner = null;
				return;
			} else if (ezBox) {
				equivCount++;
				ezEq();
			} else {
				equivCount++;
				doCE();
			}
			// System.out.println(rendering.render(lastCE));

			OWLClassExpression left = null;
			OWLClassExpression right = null;
			// lastCE is last counter example provided by oracle, unsaturate and saturate
			if (lastCE.isOfType(AxiomType.SUBCLASS_OF)) {
				left = ((OWLSubClassOfAxiom) lastCE).getSubClass();
				right = ((OWLSubClassOfAxiom) lastCE).getSuperClass();
			} else {

				learner = null;
				learner();

				return;

			}
			lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);
			// check if complex side is left
			if (checkLeft(lastCE)) {

				// decompose tries to find underlying inclusions inside the left hand side
				// by recursively breaking the left expression and adding new inclusions to the
				// hypothesis
				/*
				 * if(oracleMerge.isSelected()) oracle.oracleSiblingMerge(left, right);
				 * if(oracleSaturate.isSelected()) oracle.saturateWithTreeLeft(lastCE);
				 */
				if (learnerDecompL) {
					// System.out.println("lhs decomp");
					learner.decompose(left, right);
				}
				// branch edges on left side of the inclusion (if possible) to make it logically
				// stronger (more general)
				if (learnerBranch) {
					// System.out.println("lhs branch");
					left = learner.branchLeft(left, right);
				}
				lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);

				// unsaturate removes useless concepts from nodes in the inclusion
				if (learnerUnsat) {
					// System.out.println("lhs unsaturate");

					left = learner.unsaturateLeft(lastCE);
				}
				lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);
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
					learner.decompose(left, right);
				}
				// merge edges on right side of the inclusion (if possible) to make it logically
				// stronger (more general)
				if (learnerMerge) {
					// System.out.println("rhs merge");
					right = learner.learnerSiblingMerge(left, right);
				}
				// rebuild inclusion for final step
				lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);
				if (learnerSat) {
					// System.out.println("rhs saturate");
					lastCE = learner.saturateWithTreeRight(lastCE);
				}
				left = ((OWLSubClassOfAxiom) lastCE).getSubClass();
				right = ((OWLSubClassOfAxiom) lastCE).getSuperClass();
				try {
					addHypothesis(lastCE);
					learner = null;
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
			learner();
		}

	}

	public void equivalenceCheck() {

		int x = 0;
		if (!wePlayin)
			JOptionPane.showMessageDialog(null, "No Ontology loaded yet, please load an Ontology to start playing!",
					"Alert", JOptionPane.INFORMATION_MESSAGE);
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
		win = true;
		System.out.println("Ontology learned successfully!");
		System.out.println("You dun did it!!!");
		
		axiomsT = new HashSet<OWLAxiom>();
		for (OWLAxiom axe : ontology.getAxioms())
			if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
					|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
				axiomsT.add(axe);
	}

	public void loadOntology() throws InterruptedException {
		win = false;

		try {
			equivCount = 0;
			membCount = 0;
			manager = OWLManager.createOWLOntologyManager();
			System.out.println("Trying to load ontology");
			ontology = manager.loadOntologyFromOntologyDocument(new File(filePath));
			rendering = new ManchesterOWLSyntaxOWLObjectRendererImpl();
 
			reasonerForT = createReasoner(ontology);
			shortFormProvider = new SimpleShortFormProvider();
			axiomsT = new HashSet<OWLAxiom>();
			for (OWLAxiom axe : ontology.getAxioms())
				if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
						|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
					axiomsT.add(axe);
			axiomsTCheck = new HashSet<OWLAxiom>();
			for (OWLAxiom axe : ontology.getAxioms())
				if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
						|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
					axiomsTCheck.add(axe);

			lastCE = null;

			ELQueryEngineForT = new ELEngine(reasonerForT, shortFormProvider);
			
			// transfer Origin ontology to ManchesterOWLSyntaxOntologyFormat
			OWLOntologyFormat format = manager.getOntologyFormat(ontology);
			ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
			if (format.isPrefixOWLOntologyFormat()) {
				manSyntaxFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
			}
			format = null;
			
			// create personalized names for ontology
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
				manager.saveOntology(ontology, manSyntaxFormat, IRI.create(newFile.toURI()));

				// Create OWL Ontology Manager for hypothesis and load hypothesis file
				if (hypoFile.exists()) {
					hypoFile.delete();
				}
				hypoFile.createNewFile();

				ontologyH = manager.loadOntologyFromOntologyDocument(hypoFile);
			}

			shortFormProvider = new SimpleShortFormProvider();
			axiomsH = ontologyH.getAxioms();
			wePlayin = true; 

			System.out.println(ontology);
			System.out.println("Loaded successfully.");
			System.out.println();

			concepts = new SimpleClass(rendering).getSuggestionNames("concept", newFile);
			roles = new SimpleClass(rendering).getSuggestionNames("role", newFile);

			System.out.println("Total number of concepts is: " + concepts.size());


			
			System.out.flush();
		} catch (OWLOntologyCreationException e) {
			System.out.println("Could not load ontology: " + e.getMessage());
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
				System.out.println("Won't add " + rendering.render(ax));
		} 
	}
	
	public void getOntologyName() {

		int con = 0;
		for (int i = 0; i < ontology.getOntologyID().toString().length(); i++)
			if (ontology.getOntologyID().toString().charAt(i) == '/')
				con = i;
		ontologyName = ontology.getOntologyID().toString().substring(con + 1,
				ontology.getOntologyID().toString().length());
		ontologyName = ontologyName.substring(0, ontologyName.length() - 3);
		if (!ontologyName.contains(".owl"))
			ontologyName = ontologyName + ".owl";
		ontologyFolder += ontologyName;
		ontologyFolderH += "hypo_" + ontologyName;
	}

	public void resetVariables() {
		ontologyPath = null;
		manager = null;
		rendering = null;

		reasonerForT = null;
		axiomsT = null;
		axiomsTCheck = null;
		ELQueryEngineForT = null;
		ontologyFolder = null;
		ontologyName = null;
		hypoFile = null;
		newFile = null;

		concepts = new ArrayList<String>();
		roles = new ArrayList<String>();

		cIo = null;

		reasonerForH = null;
		shortFormProvider = null;
		axiomsH = null;
		ontologyFolderH = null;
		ontology = null;
		ontologyH = null;
		lastCE = null;

		smallestOne = null;
		smallestSize = 0;
	}

	public Boolean equivalenceQuery() {

		reasonerForH = createReasoner(ontologyH);
		ELEngine ELQueryEngineForH = new ELEngine(reasonerForH, shortFormProvider);
		Boolean queryAns = ELQueryEngineForH.entailed(axiomsT);
		reasonerForH.dispose();
		return queryAns;
	}

	public OWLReasoner createReasoner(OWLOntology ontology) {
		ElkReasonerFactory reasoningFactory = new ElkReasonerFactory();
		return reasoningFactory.createReasoner(ontology);
	}

	public String getCounterExample() throws Exception {
		reasonerForH = createReasoner(ontologyH);
		ELEngine ELQueryEngineForH = new ELEngine(reasonerForH, shortFormProvider);

		ELOracle oracle = new ELOracle(reasonerForH, shortFormProvider, ontology, ontologyH, ELQueryEngineForT, this);
		// reasonerForH.dispose();

		Iterator<OWLAxiom> iteratorT = axiomsT.iterator();
		while (iteratorT.hasNext()) {
			OWLAxiom selectedAxiom = iteratorT.next();
			selectedAxiom.getAxiomType();

			// first get CounterExample from an axiom with the type SUBCLASS_OF
			if (selectedAxiom.isOfType(AxiomType.SUBCLASS_OF)) {
				Boolean queryAns = ELQueryEngineForH.entailed(selectedAxiom);
				// if hypothesis does NOT entail the CI
				if (!queryAns) {
					// System.out.println("Chosen CE:" + rendering.render(selectedAxiom));
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
								ex = oracle.oracleSiblingMerge(
										((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(),
										((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
								newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(ex, superclass);
								ex = null;
							}
							if (oracleSaturate)
								newCounterexampleAxiom = oracle
										.saturateWithTreeLeft((OWLSubClassOfAxiom) newCounterexampleAxiom);
						} else {

							if (oracleBranch) {
								ex = null;
								OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) newCounterexampleAxiom;
								ex = oracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
								newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(auxAx.getSubClass(), ex);
								auxAx = null;
								ex = null;
							}
							if (oracleUnsaturate) {
								ex = null;
								ex = oracle.unsaturateRight(newCounterexampleAxiom);
								newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(subclass, ex);
								ex = null;
							}
						}
						/*
						 * } else { ex = siblingMerge(((OWLSubClassOfAxiom)
						 * newCounterexampleAxiom).getSuperClass()); newCounterexampleAxiom =
						 * saturateWithTreeRight( ELQueryEngineForT.getSubClassAxiom(subclass, ex)); }
						 */

						// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
						lastCE = newCounterexampleAxiom;
						subclass = null;
						superclass = null;
						oracle = null;
						counterexample = null;
						selectedAxiom = null;
						iteratorT = null;
						oracle = null;
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

					Set<OWLClass> superclasses = ELQueryEngineForT.getSuperClasses(subclass, true);
					if (!superclasses.isEmpty()) {
						Iterator<OWLClass> iteratorSuperClass = superclasses.iterator();
						while (iteratorSuperClass.hasNext()) {
							OWLClassExpression SuperclassInSet = iteratorSuperClass.next();
							OWLAxiom newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(subclass,
									SuperclassInSet);
							Boolean querySubClass = ELQueryEngineForH.entailed(newCounterexampleAxiom);
							Boolean querySubClassforT = ELQueryEngineForT.entailed(newCounterexampleAxiom);
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
										ex = oracle.oracleSiblingMerge(
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(),
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
										newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(ex,
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
										ex = null;
									}
									if (oracleSaturate)
										newCounterexampleAxiom = oracle
												.saturateWithTreeLeft((OWLSubClassOfAxiom) newCounterexampleAxiom);
								} else {
									if (oracleBranch) {
										ex = null;
										OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) newCounterexampleAxiom;
										ex = oracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
										newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(auxAx.getSubClass(),
												ex);
										auxAx = null;
										ex = null;
									}
									if (oracleUnsaturate) {
										ex = null;
										ex = oracle.unsaturateRight(newCounterexampleAxiom);
										newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(), ex);
										ex = null;
									}
								}
								// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
								lastCE = newCounterexampleAxiom;
								oracle = null;
								oracle = null;
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
					Boolean queryAns = ELQueryEngineForH.entailed(selectedAxiom);

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
								ex = oracle.oracleSiblingMerge(((OWLSubClassOfAxiom) selectedAxiom).getSubClass(),
										((OWLSubClassOfAxiom) selectedAxiom).getSuperClass());
								selectedAxiom = (OWLSubClassOfAxiom) ELQueryEngineForT.getSubClassAxiom(ex,
										((OWLSubClassOfAxiom) selectedAxiom).getSuperClass());
								ex = null;
							}
							if (oracleSaturate)
								selectedAxiom = (OWLSubClassOfAxiom) oracle
										.saturateWithTreeLeft((OWLSubClassOfAxiom) selectedAxiom);
						} else {

							if (oracleBranch) {
								ex = null;
								OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) selectedAxiom;
								ex = oracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
								selectedAxiom = (OWLSubClassOfAxiom) ELQueryEngineForT
										.getSubClassAxiom(auxAx.getSubClass(), ex);
								auxAx = null;
								ex = null;
							}
							if (oracleUnsaturate) {
								ex = null;
								ex = oracle.unsaturateRight((OWLSubClassOfAxiom) selectedAxiom);
								selectedAxiom = (OWLSubClassOfAxiom) ELQueryEngineForT
										.getSubClassAxiom(((OWLSubClassOfAxiom) selectedAxiom).getSubClass(), ex);
								ex = null;
							}

						}
						// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
						lastCE = selectedAxiom;

						oracle = null;
						Axiom = null;
						iterator = null;
						iteratorT = null;
						oracle = null;
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
						Boolean queryAns = ELQueryEngineForH.entailed(subClassAxiom);
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
									ex = oracle.oracleSiblingMerge(((OWLSubClassOfAxiom) subClassAxiom).getSubClass(),
											((OWLSubClassOfAxiom) subClassAxiom).getSuperClass());
									subClassAxiom = (OWLSubClassOfAxiom) ELQueryEngineForT.getSubClassAxiom(ex,
											((OWLSubClassOfAxiom) subClassAxiom).getSuperClass());
									ex = null;
								}
								if (oracleSaturate)
									subClassAxiom = (OWLSubClassOfAxiom) oracle
											.saturateWithTreeLeft((OWLSubClassOfAxiom) subClassAxiom);
							} else {
								if (oracleBranch) {
									ex = null;
									OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) subClassAxiom;
									ex = oracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
									subClassAxiom = (OWLSubClassOfAxiom) ELQueryEngineForT
											.getSubClassAxiom(auxAx.getSubClass(), ex);
									auxAx = null;
									ex = null;
								}
								if (oracleUnsaturate) {
									ex = null;
									ex = oracle.unsaturateRight((OWLSubClassOfAxiom) subClassAxiom);
									subClassAxiom = (OWLSubClassOfAxiom) ELQueryEngineForT
											.getSubClassAxiom(((OWLSubClassOfAxiom) subClassAxiom).getSubClass(), ex);
									ex = null;
								}
							}
							// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*?*-*-*-*-*-*-*-*
							lastCE = subClassAxiom;
							oracle = null;
							Axiom = null;
							iterator = null;
							iteratorAsSub = null;
							oracle = null;
							iteratorT = null;
							System.out.flush();
							return addHypothesis(subClassAxiom);
						}
					}
				}
			}
		}
		System.out.println("no more CIs");
		oracle = null;
		iterator = null;
		iteratorT = null;
		System.out.flush();
		return null;
	}

	public boolean checkLeft(OWLAxiom axiom) {

		String left = rendering.render(((OWLSubClassOfAxiom) axiom).getSubClass());
		String right = rendering.render(((OWLSubClassOfAxiom) axiom).getSuperClass());
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
		reasonerForH = createReasoner(ontologyH);
		ELEngine ELQueryEngineForH = new ELEngine(reasonerForH, shortFormProvider);
		Set<OWLClass> superclasses = ELQueryEngineForT.getSuperClasses(superclass, false);
		Set<OWLClass> subclasses = ELQueryEngineForT.getSubClasses(subclass, false);

		if (!subclasses.isEmpty()) {

			Iterator<OWLClass> iteratorSubClass = subclasses.iterator();
			while (iteratorSubClass.hasNext()) {
				OWLClassExpression SubclassInSet = iteratorSubClass.next();
				OWLAxiom newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(SubclassInSet, superclass);
				Boolean querySubClass = ELQueryEngineForH.entailed(newCounterexampleAxiom);
				Boolean querySubClassforT = ELQueryEngineForT.entailed(newCounterexampleAxiom);
				if (!querySubClass && querySubClassforT) {
					SubclassInSet = null;
					superclass = null;
					iteratorSubClass = null;
					ELQueryEngineForH = null;

					reasonerForH.dispose();
					return newCounterexampleAxiom;
				}
			}
		}
		if (!superclasses.isEmpty()) {

			Iterator<OWLClass> iteratorSuperClass = superclasses.iterator();
			while (iteratorSuperClass.hasNext()) {
				OWLClassExpression SuperclassInSet = iteratorSuperClass.next();
				OWLAxiom newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(subclass, SuperclassInSet);
				Boolean querySubClass = ELQueryEngineForH.entailed(newCounterexampleAxiom);
				Boolean querySubClassforT = ELQueryEngineForT.entailed(newCounterexampleAxiom);
				if (!querySubClass && querySubClassforT) {

					SuperclassInSet = null;
					superclass = null;
					subclass = null;
					iteratorSuperClass = null;
					ELQueryEngineForH = null;

					reasonerForH.dispose();
					return newCounterexampleAxiom;
				}
			}
		}

		ELQueryEngineForH = null;
		superclass = null;
		subclass = null;
		reasonerForH.dispose();
		return null;
	}

	public String addHypothesis(OWLAxiom addedAxiom) throws Exception {
		String StringAxiom = rendering.render(addedAxiom);

		AddAxiom newAxiomInH = new AddAxiom(ontologyH, addedAxiom);
		manager.applyChange(newAxiomInH);
		saveOWLFile(ontologyH, hypoFile);

		// minimize hypothesis
		ontologyH = MinHypothesis(ontologyH, addedAxiom);
		saveOWLFile(ontologyH, hypoFile);
		newAxiomInH = null;
		addedAxiom = null;
		return StringAxiom;
	}

	private void saveOWLFile(OWLOntology ontology, File file) throws Exception {

		OWLOntologyFormat format = manager.getOntologyFormat(ontology);
		ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
		if (format.isPrefixOWLOntologyFormat()) {
			// need to remove prefixes
			manSyntaxFormat.clearPrefixes();
		}
		format = null;
		manager.saveOntology(ontology, manSyntaxFormat, IRI.create(file.toURI()));
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
					manager.applyChange(removedAxiom);

					OWLReasoner tmpreasoner = createReasoner(tmpOntologyH);
					ELEngine tmpELQueryEngine = new ELEngine(tmpreasoner, shortFormProvider);
					Boolean queryAns = tmpELQueryEngine.entailed(checkedAxiom);
					tmpreasoner.dispose();

					if (queryAns) {
						RemoveAxiom removedAxiomFromH = new RemoveAxiom(hypoOntology, checkedAxiom);
						manager.applyChange(removedAxiomFromH);
						removedstring = "\t[" + rendering.render(checkedAxiom) + "]\n";
						if (checkedAxiom.equals(addedAxiom)) {
							flag = true;
						}
					} else {
						AddAxiom addAxiomtoH = new AddAxiom(hypoOntology, checkedAxiom);
						manager.applyChange(addAxiomtoH);
						addAxiomtoH = null;
					}
				}
			}
			if (!removedstring.equals("")) {
				String message;
				if (flag) {
					message = "The axiom [" + rendering.render(addedAxiom) + "] will not be added to the hypothesis\n"
							+ "since it can be replaced by some axiom(s) that already exist in the hypothesis.";
				} else {
					message = "The axiom [" + removedstring + "]" + "will be removed after adding: \n["
							+ rendering.render(addedAxiom) + "]";
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
