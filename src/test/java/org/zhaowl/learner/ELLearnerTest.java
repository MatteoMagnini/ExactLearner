package org.zhaowl.learner;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.zhaowl.console.consoleLearner;
import org.zhaowl.oracle.ELOracle;
import org.zhaowl.engine.ELEngine;


import static org.junit.Assert.*;

public class ELLearnerTest {

    private final consoleLearner cl = new consoleLearner();
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

        elLearner = new ELLearner(elQueryEngineForT, elQueryEngineForH, cl);
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
    }

    @Test
    public void branchLeft() {
    }

    @Test
    public void powerSetBySize() {
    }

    @Test
    public void isCounterExample() {
    }
}