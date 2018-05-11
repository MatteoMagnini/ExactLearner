package org.zhaowl.learner;

import static org.junit.Assert.fail;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.zhaowl.engine.ELEngine;
import org.zhaowl.oracle.ELOracle;
import org.zhaowl.utils.Metrics;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.io.OWLObjectRenderer;

public class ELLearnerTest {

    private final OWLObjectRenderer myRenderer =  new ManchesterOWLSyntaxOWLObjectRendererImpl();
    private final Metrics metrics = new Metrics(myRenderer);
    private final OWLOntologyManager man = OWLManager.createOWLOntologyManager();
    private OWLOntology targetOntology = null;
    private OWLOntology hypothesisOntology = null;
    private ELEngine elQueryEngineForT = null;
    private ELEngine elQueryEngineForH = null;
    private ELLearner elLearner = null;
     


    @Before
    public void setUp() throws Exception {
        Logger.getRootLogger().setLevel(Level.OFF);

        targetOntology = man.createOntology();
        hypothesisOntology = man.createOntology();

        elQueryEngineForH = new ELEngine(hypothesisOntology);
        elQueryEngineForT = new ELEngine(targetOntology);

        elLearner = new ELLearner(elQueryEngineForT, elQueryEngineForH, metrics);
        
    }

    @Test
    public void learnerSiblingMerge1() {
        OWLDataFactory df = man.getOWLDataFactory();


        OWLClass A = df.getOWLClass(IRI.create(":A"));
        OWLClass B = df.getOWLClass(IRI.create(":B"));
        OWLClass C = df.getOWLClass(IRI.create(":C"));
        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":r"));
        OWLObjectProperty S = df.getOWLObjectProperty(IRI.create(":s"));
        OWLClass left = A;
        OWLClassExpression right = df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, C), df.getOWLObjectSomeValuesFrom(R, B), df.getOWLObjectSomeValuesFrom(R,A));
        OWLSubClassOfAxiom axiom;
        OWLSubClassOfAxiom mergedAxiom= df.getOWLSubClassOfAxiom(A, df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, df.getOWLObjectIntersectionOf(B,C)),df.getOWLObjectSomeValuesFrom(R,A)));
        man.addAxiom(targetOntology, mergedAxiom);
        try {
            axiom = elLearner.mergeRight(left, right);
            System.out.println("Merged: " + axiom);
            if(!axiom.equals(mergedAxiom))
                fail("Did not merge.");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @Test
    public void unsaturateLeft() {
        OWLDataFactory df = man.getOWLDataFactory();
        // Create 6 classes to target ontology
        OWLClass A = df.getOWLClass(IRI.create(":A"));
        OWLClass B = df.getOWLClass(IRI.create(":B"));
        OWLClass C = df.getOWLClass(IRI.create(":C"));
        OWLClassExpression ABC = df.getOWLObjectIntersectionOf(A,B,C);

        OWLClass D = df.getOWLClass(IRI.create(":D"));
        OWLClass E = df.getOWLClass(IRI.create(":E"));
        OWLClass F = df.getOWLClass(IRI.create(":F"));
        OWLClassExpression DEF = df.getOWLObjectIntersectionOf(D,E,F);

        // Create and add an axiom to ontology
        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(ABC, DEF);
        man.addAxiom(targetOntology, axiom);

        // Add expected axiom to ontology
        axiom = df.getOWLSubClassOfAxiom(B, A);
        man.addAxiom(targetOntology, axiom);

        // DEBUG: Look at classes in ontology
        //System.out.println(targetOntology.getClassesInSignature());

        // Begin unsaturation of inclusion
        // A \sqcap B \sqcap C \sqcap D \sqcap E \sqcap F  \sqsubseteq A
        OWLClassExpression ABCDEF = df.getOWLObjectIntersectionOf(A,B,C,D,E,F);
        axiom = df.getOWLSubClassOfAxiom(ABCDEF, A);
        man.addAxiom(targetOntology, axiom);

        try
        {
            axiom = elLearner.unsaturateLeft(ABCDEF, A);

            // Expected
            // B \sqsubseteq A
            System.out.println("Unsaturation: " + axiom);
        }
        catch (Exception e) {
            System.out.println("Error in unsaturate left");
            e.printStackTrace();
        }
    }

    @Test
    public void decompose() {
        OWLDataFactory df = man.getOWLDataFactory();

        OWLClass A = df.getOWLClass(IRI.create(":A"));
        OWLClass B = df.getOWLClass(IRI.create(":B"));
        OWLClass C = df.getOWLClass(IRI.create(":C"));
        OWLClass D = df.getOWLClass(IRI.create(":D"));
        OWLClass E = df.getOWLClass(IRI.create(":E"));

        OWLClass F = df.getOWLClass(IRI.create(":F"));
        OWLClass G = df.getOWLClass(IRI.create(":G"));
        OWLClass H = df.getOWLClass(IRI.create(":H"));

        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":r"));


        // Left decomposition
        OWLClassExpression ArB = df.getOWLObjectIntersectionOf(A, df.getOWLObjectSomeValuesFrom(R, B));
        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(ArB, C);
        man.addAxiom(targetOntology, axiom);
        // Expected axiom
        // B \sqsubseteq D
        axiom = df.getOWLSubClassOfAxiom(B, df.getOWLObjectIntersectionOf(D,E));
        man.addAxiom(targetOntology, axiom);


        // Right decomposition
        OWLClassExpression FrG = df.getOWLObjectIntersectionOf(F, df.getOWLObjectSomeValuesFrom(R, G));
        axiom = df.getOWLSubClassOfAxiom(E, FrG);
        man.addAxiom(targetOntology, axiom);
        // Expected axiom
        // H \sqsubseteq G
        axiom = df.getOWLSubClassOfAxiom(H, G);
        man.addAxiom(targetOntology, axiom);
        try
        {
            // Try two decompositions
            axiom = elLearner.decomposeLeft(ArB, C);
            System.out.println("Decompose left: " + axiom);

            axiom = elLearner.decomposeRight(E, FrG);
            System.out.println("Decompose right: " + axiom);
        }
        catch(Exception e)
        {
            System.out.println("Error in decompose");
            e.printStackTrace();
        }

    }

    @Test
    public void saturateWithTreeRight() {

        OWLDataFactory df = man.getOWLDataFactory();
        // Create 6 classes to target ontology
        OWLClass A = df.getOWLClass(IRI.create(":A"));
        OWLClass B = df.getOWLClass(IRI.create(":B"));
        OWLClass C = df.getOWLClass(IRI.create(":C"));
        OWLClassExpression ABC = df.getOWLObjectIntersectionOf(A,B,C);

        OWLClass D = df.getOWLClass(IRI.create(":D"));
        OWLClass E = df.getOWLClass(IRI.create(":E"));
        OWLClass F = df.getOWLClass(IRI.create(":F"));
        OWLClassExpression DEF = df.getOWLObjectIntersectionOf(D,E,F);

        // Create and add an axiom to ontology
        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(ABC, DEF);
        man.addAxiom(targetOntology, axiom);

        // DEBUG: Look at classes in ontology
        //System.out.println(targetOntology.getClassesInSignature());

        // Begin saturation of inclusion
        // A \sqsubseteq B \sqcap C
        OWLClassExpression BC = df.getOWLObjectIntersectionOf(B,C);
        axiom = df.getOWLSubClassOfAxiom(A, BC);
        man.addAxiom(targetOntology, axiom);

        try
        {
            axiom = elLearner.saturateRight(A, BC);

            // Expected
            // A \sqsubseteq A \sqcap B \sqcap C \sqcap D \sqcap E \sqcap F
            System.out.println("Saturation: " + axiom);
        }
        catch (Exception e) {
            System.out.println("Error in saturate right");
            e.printStackTrace();
        }

    }


    @Test
    public void learnerSiblingMerge() {
        OWLDataFactory df = man.getOWLDataFactory();


        OWLClass A = df.getOWLClass(IRI.create(":A"));
        OWLClass B = df.getOWLClass(IRI.create(":B"));
        OWLClass C = df.getOWLClass(IRI.create(":C"));
        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":r"));
        OWLObjectProperty S = df.getOWLObjectProperty(IRI.create(":s"));
        OWLClass left = A;
        OWLClassExpression right = df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, C), df.getOWLObjectSomeValuesFrom(R, B), df.getOWLObjectSomeValuesFrom(R,A));
        OWLSubClassOfAxiom axiom;
        OWLSubClassOfAxiom mergedAxiom= df.getOWLSubClassOfAxiom(A, df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, df.getOWLObjectIntersectionOf(B,C)),df.getOWLObjectSomeValuesFrom(R,A)));
        man.addAxiom(targetOntology, mergedAxiom);
        try {
            axiom = elLearner.mergeRight(left, right);
            System.out.println("Merged: " + axiom);
            if(!axiom.equals(mergedAxiom))
                fail("Did not merge.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void branchLeft() {
        OWLDataFactory df = man.getOWLDataFactory();

        OWLClass A = df.getOWLClass(IRI.create(":A"));
        OWLClass right = A;
        OWLClass B = df.getOWLClass(IRI.create(":B"));
        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":r"));
        OWLClass C = df.getOWLClass(IRI.create(":C"));
        OWLClass D = df.getOWLClass(IRI.create(":D"));
        // try to branch this expression
        OWLClassExpression left = df.getOWLObjectSomeValuesFrom(R, df.getOWLObjectIntersectionOf(B,C,D));

        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(left, A);
        man.addAxiom(targetOntology, axiom);
        axiom = null;

        // Expected Branched Axiom
        // r.B \sqcap r.C \sqsubseteq A
        OWLSubClassOfAxiom branchedAxiom = df.getOWLSubClassOfAxiom(df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, D), df.getOWLObjectSomeValuesFrom(R, B), df.getOWLObjectSomeValuesFrom(R, C)), A);
        //System.out.println("Expected: " + branchedAxiom);
        man.addAxiom(targetOntology, branchedAxiom);
        try {
            axiom = elLearner.branchLeft(left, right);
            System.out.println("Branched: " + axiom);
            if(!axiom.equals(branchedAxiom))
                fail("Did not branch.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
 
}