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
	private int equivCount = 0;
	private boolean ezBox;
	public boolean autoBox = true;
	private String filePath;

	// ############# Game variables Start ######################
	
	// #########################################################
	
	// ############# OWL variables Start ######################

	private OWLOntologyManager myManager = null;
	private ManchesterOWLSyntaxOWLObjectRendererImpl myRenderer = null;


	private Set<OWLAxiom> axiomsT = null;
	private Set<OWLAxiom> axiomsTCheck = null;

	private String ontologyFolder = null;
	private String ontologyName = null;
	private File hypoFile = null;
	private File newFile = null;

	private ArrayList<String> concepts = new ArrayList<>();
	private ArrayList<String> roles = new ArrayList<>();

	private Set<OWLAxiom> axiomsH = null;
	private String ontologyFolderH = null;

	private OWLAxiom lastCE = null;
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

	private static final org.slf4j.Logger LOGGER_ = LoggerFactory
			.getLogger(consoleLearner.class);

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

			// normal mode allows for elOracle skills
			// elOracle skills not allowed
			// elOracle skills allowed
			ezBox = args[1].equals("on");

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


				long timeStart = System.currentTimeMillis();
				runLearner(elQueryEngineForT, elQueryEngineForH);
				long timeEnd = System.currentTimeMillis();
				System.out.println("Total time (ms): " + (timeEnd - timeStart));
				System.out.println("Total membership queries: " + membCount);
				System.out.println("Total equivalence queries: " + equivCount);
				System.out.println("Target TBox logical axioms: " + axiomsT.size());
				saveOWLFile(hypothesisOntology, hypoFile);



				new Metrics(myRenderer).showCIT(axiomsT,true);
				
				System.out.println("Hypothesis TBox logical axioms: " + hypothesisOntology.getAxioms().size());
				new Metrics(myRenderer).showCIT(hypothesisOntology.getAxioms(),false);
				elQueryEngineForH.disposeOfReasoner();
				elQueryEngineForT.disposeOfReasoner();

			} catch (Throwable e) {
				e.printStackTrace();
				System.out.println("error in runLearner call ----- " + e);
			}

		} catch (Throwable e) {
			// TODO Auto-generated catch block
			System.out.println("error in doIt --- " + e);
		}
		finally {
			elQueryEngineForH.disposeOfReasoner();
			elQueryEngineForT.disposeOfReasoner();
		}

	}

	private void setOracleSkills(String[] args) {
		oracleMerge = args[8].equals("t");

		oracleSaturate = args[9].equals("t");

		oracleBranch = args[10].equals("t");

		oracleUnsaturate = args[11].equals("t");
	}

	private void setLearnerSkills(String[] args) {

		learnerDecompL = args[2].equals("t");

		learnerBranch = args[3].equals("t");

		learnerUnsat = args[4].equals("t");

		learnerDecompR = args[5].equals("t");

		learnerMerge = args[6].equals("t");

		learnerSat = args[7].equals("t");

	}

 
	private void runLearner(ELEngine elQueryEngineForT,ELEngine elQueryEngineForH) throws Throwable {
		 
		OWLSubClassOfAxiom counterexample=null;
		OWLClassExpression left=null;
		OWLClassExpression right=null;
 
		while (!equivalenceQuery()) {			
			equivCount++;
			if (ezBox) {
				lastCE=getEasyCounterExample(elQueryEngineForT,elQueryEngineForH);
			} else {	
				lastCE=getCounterExample(elQueryEngineForT,elQueryEngineForH);
			}		
			counterexample = (OWLSubClassOfAxiom) lastCE;
			left=counterexample.getSubClass();
			right=counterexample.getSuperClass();
			lastCE=elLearner.decompose(left, right);			
			if(canTransformELrhs()) {
				lastCE=computeEssentialRightCounterexample();
			} else if(canTransformELlhs()) {
				lastCE=computeEssentialLeftCounterexample();
			} 
			addHypothesis(lastCE);  
		}
		victory();		
		lastCE = null;
    }

 
	private OWLAxiom computeEssentialLeftCounterexample() throws Exception {
		OWLAxiom axiom=lastCE;	
		OWLSubClassOfAxiom counterexample =(OWLSubClassOfAxiom) axiom;
		OWLClassExpression left=counterexample.getSubClass();
		OWLClass  right= (OWLClass) counterexample.getSuperClass();
 
		 
						if (learnerDecompL) {
							axiom =elLearner.decomposeLeft(lastExpression, lastName);
							counterexample = (OWLSubClassOfAxiom) axiom;
							left=counterexample.getSubClass();
							right=(OWLClass) counterexample.getSuperClass();
						}
						 
						if (learnerBranch) {				 
							axiom =elLearner.branchLeft(left, right);
							counterexample = (OWLSubClassOfAxiom) axiom;
							left=counterexample.getSubClass();
							right=(OWLClass) counterexample.getSuperClass();
						}
						 
						if (learnerUnsat) {
							axiom = elLearner.unsaturateLeft(left, right);						 
						}
						 
		return axiom;
	}
 
	private OWLAxiom computeEssentialRightCounterexample() throws Exception {
		OWLAxiom axiom=lastCE;	
		OWLSubClassOfAxiom counterexample =(OWLSubClassOfAxiom) axiom;
		OWLClass left= (OWLClass)  counterexample.getSubClass();
		OWLClassExpression right=counterexample.getSuperClass();
 
		 
						if (learnerDecompR) {
							axiom = elLearner.decomposeRight(lastName,lastExpression);
							counterexample = (OWLSubClassOfAxiom) axiom;
							left= (OWLClass)counterexample.getSubClass();
							right=  counterexample.getSuperClass();
						}
						 
						if (learnerMerge) {				 
							axiom = elLearner.mergeRight(left, right);
							counterexample = (OWLSubClassOfAxiom) axiom;
							left= (OWLClass)counterexample.getSubClass();
							right= counterexample.getSuperClass();
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
			myManager = OWLManager.createOWLOntologyManager();
			System.out.println("Trying to load targetOntology");
			targetOntology = myManager.loadOntologyFromOntologyDocument(new File(filePath));
			myRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

			axiomsT = new HashSet<>();
			for (OWLAxiom axe : targetOntology.getAxioms())
				if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
						|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
					axiomsT.add(axe);
			axiomsTCheck = new HashSet<>();
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
			// format = null;
			
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
			//public boolean win;
			// boolean wePlayin = true;



			System.out.println(targetOntology);
			System.out.println("Loaded successfully.");
			System.out.println();

			concepts = new Metrics(myRenderer).getSuggestionNames("concept", newFile);
			roles = new Metrics(myRenderer).getSuggestionNames("role", newFile);

			System.out.println("Total number of concepts is: " + concepts.size());


			
			System.out.flush();
		} catch (OWLOntologyCreationException e) {
			System.out.println("Could not load targetOntology: " + e.getMessage());
		} catch (OWLException | IOException e) {
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

	public void resetVariables() {
		myManager = null;
		myRenderer = null;

		axiomsT = null;
		axiomsTCheck = null;
		elQueryEngineForT = null;
		ontologyFolder = null;
		ontologyName = null;
		hypoFile = null;
		newFile = null;

		concepts = new ArrayList<>();
		roles = new ArrayList<>();

		axiomsH = null;
		ontologyFolderH = null;
		targetOntology = null;
		hypothesisOntology = null;
		lastCE = null;
	}

	private Boolean equivalenceQuery() {

		return elQueryEngineForH.entailed(axiomsT);
	}



	private OWLAxiom getCounterExample(ELEngine elQueryEngineForT, ELEngine elQueryEngineForH) throws Exception {
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

                    OWLAxiom newCounterexampleAxiom = getCounterExamplefromSubClassAxiom(subclass, superclass, elQueryEngineForT, elQueryEngineForH);
                    if (newCounterexampleAxiom != null) {
                        // if we actually got something, we use it as new counter example

                        // System.out.println("subclass 1");
                        // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                        // ADD SATURATION FOR newCounterexampleAxiom HERE
                        OWLClassExpression ex = null;
                        if (checkLeft(newCounterexampleAxiom)) {
                            if (oracleMerge) {
                                // ex = null;
                                // System.out.println(newCounterexampleAxiom);
                                // if (checkLeft(newCounterexampleAxiom)) {
                                ex = elOracle.oracleSiblingMerge(
                                        ((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(),
                                        ((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
                                newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(ex, superclass);
                                // ex = null;
                            }
                            if (oracleSaturate)
                                newCounterexampleAxiom = elOracle
                                        .saturateWithTreeLeft((OWLSubClassOfAxiom) newCounterexampleAxiom);
                        } else {

                            if (oracleBranch) {
                                // ex = null;
                                OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) newCounterexampleAxiom;
                                ex = elOracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
                                newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(auxAx.getSubClass(), ex);
                            }
                            if (oracleUnsaturate) {
                                // ex = null;
                                ex = elOracle.unsaturateRight(newCounterexampleAxiom);
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

                                        // ex = null;
                                        // System.out.println(newCounterexampleAxiom);
                                        // if (checkLeft(newCounterexampleAxiom)) {
                                        ex = elOracle.oracleSiblingMerge(
                                                ((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(),
                                                ((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
                                        newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(ex,
                                                ((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
                                        // ex = null;
                                    }
                                    if (oracleSaturate)
                                        newCounterexampleAxiom = elOracle
                                                .saturateWithTreeLeft((OWLSubClassOfAxiom) newCounterexampleAxiom);
                                } else {
                                    if (oracleBranch) {
                                        // ex = null;
                                        OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) newCounterexampleAxiom;
                                        ex = elOracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
                                        newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(auxAx.getSubClass(),
                                                ex);
                                        // auxAx = null;
                                        // ex = null;
                                    }
                                    if (oracleUnsaturate) {
                                        // ex = null;
                                        ex = elOracle.unsaturateRight(newCounterexampleAxiom);
                                        newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(
                                                ((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(), ex);
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
                        lastCE = selectedAxiom;
                        // System.out.println("subclass 2");
                        // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                        // ADD SATURATION FOR Axiom HERE
                        OWLClassExpression ex = null;
                        if (checkLeft(selectedAxiom)) {
                            if (oracleMerge) {
                                // ex = null;
                                // System.out.println(newCounterexampleAxiom);
                                // if (checkLeft(newCounterexampleAxiom)) {
                                ex = elOracle.oracleSiblingMerge(selectedAxiom.getSubClass(),
                                        selectedAxiom.getSuperClass());
                                selectedAxiom = (OWLSubClassOfAxiom) elQueryEngineForT.getSubClassAxiom(ex,
                                        selectedAxiom.getSuperClass());
                                // ex = null;
                            }
                            if (oracleSaturate)
                                selectedAxiom = (OWLSubClassOfAxiom) elOracle
                                        .saturateWithTreeLeft(selectedAxiom);
                        } else {

                            if (oracleBranch) {
                                OWLSubClassOfAxiom auxAx = selectedAxiom;
                                ex = elOracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
                                selectedAxiom = (OWLSubClassOfAxiom) elQueryEngineForT
                                        .getSubClassAxiom(auxAx.getSubClass(), ex);
//								auxAx = null;
//								ex = null;
                            }
                            if (oracleUnsaturate) {
                                ex = elOracle.unsaturateRight(selectedAxiom);
                                selectedAxiom = (OWLSubClassOfAxiom) elQueryEngineForT
                                        .getSubClassAxiom(selectedAxiom.getSubClass(), ex);
                                // ex = null;
                            }

                        }
                        // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                        lastCE = selectedAxiom;

//						Axiom = null;
//						iterator = null;
//						iteratorT = null;
//						System.out.flush();
                        return selectedAxiom;
                    }
                }

                if (Axiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                    OWLEquivalentClassesAxiom counterexample = (OWLEquivalentClassesAxiom) Axiom;

                    Set<OWLSubClassOfAxiom> eqsubclassaxioms = counterexample.asOWLSubClassOfAxioms();
                    for (OWLSubClassOfAxiom subClassAxiom : eqsubclassaxioms) {
                        Boolean queryAns = elQueryEngineForH.entailed(subClassAxiom);
                        if (!queryAns) {
                            lastCE = subClassAxiom;
                            // System.out.println("eqcl 2");
                            // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                            // ADD SATURATION FOR subClassAxiom HERE
                            OWLClassExpression ex = null;

                            if (checkLeft(subClassAxiom)) {
                                if (oracleMerge) {
                                    // System.out.println(newCounterexampleAxiom);
                                    // if (checkLeft(newCounterexampleAxiom)) {
                                    ex = elOracle.oracleSiblingMerge(subClassAxiom.getSubClass(),
                                            subClassAxiom.getSuperClass());
                                    subClassAxiom = (OWLSubClassOfAxiom) elQueryEngineForT.getSubClassAxiom(ex,
                                            subClassAxiom.getSuperClass());
                                    // ex = null;
                                }
                                if (oracleSaturate)
                                    subClassAxiom = (OWLSubClassOfAxiom) elOracle
                                            .saturateWithTreeLeft(subClassAxiom);
                            } else {
                                if (oracleBranch) {
                                    OWLSubClassOfAxiom auxAx = subClassAxiom;
                                    ex = elOracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
                                    subClassAxiom = (OWLSubClassOfAxiom) elQueryEngineForT
                                            .getSubClassAxiom(auxAx.getSubClass(), ex);
//									auxAx = null;
//									ex = null;
                                }
                                if (oracleUnsaturate) {
                                    ex = elOracle.unsaturateRight(subClassAxiom);
                                    subClassAxiom = (OWLSubClassOfAxiom) elQueryEngineForT
                                            .getSubClassAxiom(subClassAxiom.getSubClass(), ex);
                                    // ex = null;
                                }
                            }
                            // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*?*-*-*-*-*-*-*-*
                            lastCE = subClassAxiom;
//							Axiom = null;
//							iterator = null;
//							iteratorAsSub = null;
//							iteratorT = null;
//							System.out.flush();
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

	private OWLAxiom getEasyCounterExample(ELEngine elQueryEngineForT, ELEngine elQueryEngineForH) {
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

                    OWLAxiom newCounterexampleAxiom = getCounterExamplefromSubClassAxiom(subclass, superclass, elQueryEngineForT, elQueryEngineForH);
                    if (newCounterexampleAxiom != null) {
                        // if we actually got something, we use it as new counter example

                        // System.out.println("subclass 1");
                        // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                        // ADD SATURATION FOR newCounterexampleAxiom HERE
                        OWLClassExpression ex = null;


                        // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                        lastCE = newCounterexampleAxiom;
//						subclass = null;
//						superclass = null;
//						counterexample = null;
//						selectedAxiom = null;
//						iteratorT = null;
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
                            OWLAxiom newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(subclass,
                                    SuperclassInSet);

                            Boolean querySubClass = elQueryEngineForH.entailed(newCounterexampleAxiom);
                            Boolean querySubClassforT = elQueryEngineForT.entailed(newCounterexampleAxiom);
                            if (!querySubClass && querySubClassforT) {

                                // System.out.println("eq 1");
                                // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                                // ADD SATURATION FOR newCounterexampleAxiom HERE
                                OWLClassExpression ex = null;

                                // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                                lastCE = newCounterexampleAxiom;
//								subclass = null;
//								SuperclassInSet = null;
//								superclasses = null;
//								counterexample = null;
//								subClassAxiom = null;
//								selectedAxiom = null;
//								iteratorT = null;
//								System.out.flush();
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
                        lastCE = selectedAxiom;
                        // System.out.println("subclass 2");
                        // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                        // ADD SATURATION FOR Axiom HERE
                        OWLClassExpression ex = null;

                        // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*

//						Axiom = null;
//						iterator = null;
//						iteratorT = null;
//						System.out.flush();
                        return selectedAxiom;
                    }
                }

                if (Axiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                    OWLEquivalentClassesAxiom counterexample = (OWLEquivalentClassesAxiom) Axiom;

                    Set<OWLSubClassOfAxiom> eqsubclassaxioms = counterexample.asOWLSubClassOfAxioms();
                    for (OWLSubClassOfAxiom subClassAxiom : eqsubclassaxioms) {
                        Boolean queryAns = elQueryEngineForH.entailed(subClassAxiom);
                        if (!queryAns) {
                            lastCE = subClassAxiom;
                            // System.out.println("eqcl 2");
                            // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
                            // ADD SATURATION FOR subClassAxiom HERE
                            OWLClassExpression ex = null;


                            // *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*?*-*-*-*-*-*-*-*
//							Axiom = null;
//							iterator = null;
//							iteratorAsSub = null;
//							iteratorT = null;
//							System.out.flush();
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

	private OWLAxiom getCounterExamplefromSubClassAxiom(OWLClassExpression subclass, OWLClassExpression superclass, 
			ELEngine elQueryEngineForT,ELEngine elQueryEngineForH) {
		Set<OWLClass> superclasses = elQueryEngineForT.getSuperClasses(superclass, false);
		Set<OWLClass> subclasses = elQueryEngineForT.getSubClasses(subclass, false);

		if (!subclasses.isEmpty()) {

            for (OWLClass SubclassInSet : subclasses) {
                OWLAxiom newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(SubclassInSet, superclass);
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
                OWLAxiom newCounterexampleAxiom = elQueryEngineForT.getSubClassAxiom(subclass, SuperclassInSet);
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

	public String addHypothesis(OWLAxiom addedAxiom) throws Exception {
		String StringAxiom = myRenderer.render(addedAxiom);

		//AddAxiom newAxiomInH = new AddAxiom(hypothesisOntology, addedAxiom);
		//myManager.applyChange(newAxiomInH);
        System.out.println(addedAxiom.toString());
		myManager.addAxiom(hypothesisOntology, addedAxiom);
		minimiseHypothesis();
		// saveOWLFile(hypothesisOntology, hypoFile);

		return StringAxiom;
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
		
		OWLSubClassOfAxiom counterexample = (OWLSubClassOfAxiom) lastCE;
		OWLClassExpression left=counterexample.getSuperClass();
		OWLClassExpression right=counterexample.getSubClass();
		for(OWLClass cl1 : left.getClassesInSignature()) {
			if(elOracle.isCounterExample(cl1, right)) {
				lastCE=elQueryEngineForT.getSubClassAxiom(cl1, right);
				lastExpression=right;
				lastName=cl1;
				return true;
			}		
		}
		return false;
	}
	private Boolean canTransformELlhs() {
		OWLSubClassOfAxiom counterexample = (OWLSubClassOfAxiom) lastCE;
		OWLClassExpression left=counterexample.getSuperClass();
		OWLClassExpression right=counterexample.getSubClass();
		for(OWLClass cl1 : right.getClassesInSignature()) {
			if(elOracle.isCounterExample(left,cl1)) {
				lastCE=elQueryEngineForT.getSubClassAxiom(left,cl1);
				lastExpression=left;
				lastName=cl1;
				return true;
			}		
		}
		return false;
	}
}
