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
    private ELOracle elOracle = null;


    @Before
    public void setUp() throws Exception {
        Logger.getRootLogger().setLevel(Level.OFF);

        targetOntology = man.createOntology();
        hypothesisOntology = man.createOntology();

        elQueryEngineForH = new ELEngine(hypothesisOntology);
        elQueryEngineForT = new ELEngine(targetOntology);

        elLearner = new ELLearner(elQueryEngineForT, elQueryEngineForH, metrics);
        elOracle = new ELOracle(elQueryEngineForT, elQueryEngineForH);
    }

    @Test
    public void unsaturateLeft() {
        OWLDataFactory df = man.getOWLDataFactory();
        OWLClass left = df.getOWLClass(IRI.create("left"));

        OWLClass right = df.getOWLClass(IRI.create("right"));

        
        try {
            elLearner.unsaturateLeft(left,right);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void decompose() {
    }

    @Test
    public void saturateWithTreeRight() {
    }

    @Test
    public void learnerSiblingMerge() {
    	 OWLDataFactory df = man.getOWLDataFactory();
    	 
    	 
    	 OWLClass A = df.getOWLClass(IRI.create(":A"));
         OWLClass left = A; 
         OWLClass B = df.getOWLClass(IRI.create(":B"));
         OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":r"));
         OWLClass C = df.getOWLClass(IRI.create(":C"));
         OWLClassExpression right = df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, C), df.getOWLObjectSomeValuesFrom(R, B));
         OWLSubClassOfAxiom axiom;
         OWLSubClassOfAxiom mergedAxiom= df.getOWLSubClassOfAxiom(A, df.getOWLObjectSomeValuesFrom(R, df.getOWLObjectIntersectionOf(B,C)));
         man.addAxiom(targetOntology, mergedAxiom);
         try {
        	 axiom=elLearner.mergeRight(left, right);
             if(!axiom.equals(mergedAxiom))
            	 fail("Did not merge.");
         } catch (Exception e) {
             e.printStackTrace();
         }
    }

    @Test
    public void branchLeft() {
    }
    
    @Test
    public void branchRight() {
        OWLDataFactory df = man.getOWLDataFactory();


        OWLClass A = df.getOWLClass(IRI.create(":A"));
        OWLClass left = A;
        OWLClass B = df.getOWLClass(IRI.create(":B"));
        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":r"));
        OWLClass C = df.getOWLClass(IRI.create(":C"));
        OWLClassExpression right = df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, df.getOWLObjectIntersectionOf(B,C)));
        OWLSubClassOfAxiom axiom= df.getOWLSubClassOfAxiom(A, df.getOWLObjectSomeValuesFrom(R, df.getOWLObjectIntersectionOf(B,C)));
      //  man.addAxiom(targetOntology, mergedAxiom);
        try {
            OWLSubClassOfAxiom newCounterexampleAxiom = elOracle.branchRight(left, right, 2);
            //axiom=elLearner.mergeRight(left, right);
            if(!axiom.equals(newCounterexampleAxiom))
                fail("Did not merge.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void powerSetBySize() {
    }

    @Test
    public void isCounterExample() {
    }
}