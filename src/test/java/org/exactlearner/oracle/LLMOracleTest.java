package org.exactlearner.oracle;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.exactlearner.engine.LLMEngine;
import org.configurations.Configuration;
import org.utility.YAMLConfigLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

public class LLMOracleTest {

    private final OWLOntologyManager man = OWLManager.createOWLOntologyManager();
    private OWLOntology targetOntology = null;
    private OWLOntology hypothesisOntology = null;
    private LLMEngine elQueryEngineForT = null;
    private LLMEngine elQueryEngineForH = null;
    //private ELLearner elLearner = null;
    private BaseOracle baseOracle = null;


    @Before
    public void setUp() throws Exception {
        Logger.getRootLogger().setLevel(Level.OFF);

        targetOntology = man.createOntology();
        hypothesisOntology = man.createOntology();
        var config = new YAMLConfigLoader().getConfig("src/main/java/org/pac/statementsQueryingConf.yml", Configuration.class);
        elQueryEngineForH = new LLMEngine(hypothesisOntology, config.getModels().get(3), config.getSystem(), config.getMaxTokens());
        elQueryEngineForT = new LLMEngine(targetOntology, config.getModels().get(3), config.getSystem(), config.getMaxTokens());
        baseOracle = new Oracle(elQueryEngineForT, elQueryEngineForH);

    }

    @Test
    public void unsaturateRight() {
        OWLDataFactory df = man.getOWLDataFactory();
        // Create 6 classes for target ontology
        OWLClass B = df.getOWLClass(IRI.create(":Bird"));
        OWLClass C = df.getOWLClass(IRI.create(":Crow"));
        OWLClassExpression BC = df.getOWLObjectIntersectionOf(B, C);

        OWLClass A = df.getOWLClass(IRI.create(":Animal"));

        // Create and add an axiom to ontology
        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(BC, A);
        man.addAxiom(targetOntology, axiom);

        // Add expected axiom to ontology
        axiom = df.getOWLSubClassOfAxiom(C, B);
        man.addAxiom(targetOntology, axiom);

        // We add A \sqsubseteq D to the hypothesis
        // This forces the oracle to try a different combination when unsaturating
        axiom = df.getOWLSubClassOfAxiom(C, B);
        man.addAxiom(hypothesisOntology, axiom);


        // Begin unsaturation of inclusion
        // A  \sqsubseteq A \sqcap B \sqcap C \sqcap D \sqcap E \sqcap F
        OWLClassExpression ABC = df.getOWLObjectIntersectionOf(A, B, C);
        axiom = df.getOWLSubClassOfAxiom(B, ABC);
        man.addAxiom(targetOntology, axiom);

        try {
            //							  left, right, chance
            axiom = baseOracle.unsaturateRight(B, ABC, 1);
            System.out.println("Unsaturated: " + axiom);
        } catch (Exception e) {
            System.out.println("Error in unsaturate right \n");
            e.printStackTrace();
        }
    }

    @Test
    public void branchRight() {
        OWLDataFactory df = man.getOWLDataFactory();

        OWLClass A = df.getOWLClass(IRI.create(":Fish"));
        OWLClass B = df.getOWLClass(IRI.create(":Cat"));
        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":produce_milk"));
        OWLClass C = df.getOWLClass(IRI.create(":Mammal"));
        OWLClassExpression right = df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, df.getOWLObjectIntersectionOf(B, C)));
        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(A, df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, B), df.getOWLObjectSomeValuesFrom(R, C)));
        man.addAxiom(targetOntology, axiom);
        try {
            OWLSubClassOfAxiom newCounterexampleAxiom = baseOracle.branchRight(A, right, 2);
            man.removeAxiom(targetOntology, axiom);
            System.out.println("Branched: " + axiom);
            Assert.assertEquals(axiom, newCounterexampleAxiom);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void saturateLeft() {
        OWLDataFactory df = man.getOWLDataFactory();
        // Create 6 classes for target ontology
        OWLClass A = df.getOWLClass(IRI.create(":Albatross"));
        OWLClass B = df.getOWLClass(IRI.create(":Black_Bird"));
        OWLClass C = df.getOWLClass(IRI.create(":Canary"));
        OWLClassExpression ABC = df.getOWLObjectIntersectionOf(A, B, C);

        OWLClass D = df.getOWLClass(IRI.create(":Dabb_Lizard"));
        OWLClass E = df.getOWLClass(IRI.create(":Eared_Legless_Lizard"));
        OWLClass F = df.getOWLClass(IRI.create(":Faded_Black-striped_Snake"));
        OWLClassExpression DEF = df.getOWLObjectIntersectionOf(D, E, F);

        // Create and add an axiom to ontology
        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(ABC, DEF);
        man.addAxiom(targetOntology, axiom);

        // Begin saturation of inclusion
        // A \sqcap B \sqsubseteq C
        OWLClassExpression A_ = df.getOWLObjectIntersectionOf(A);
        axiom = df.getOWLSubClassOfAxiom(A_, C);
        man.addAxiom(targetOntology, axiom);

        try {
            //						   left, right, chance
            axiom = baseOracle.saturateLeft(A_, C, 1);
            System.out.println("Saturate left: " + axiom);
        } catch (Exception e) {
            System.out.println("Error in saturate left \n");
            e.printStackTrace();
        }
    }

    @Test
    public void mergeLeft() {
        OWLDataFactory df = man.getOWLDataFactory();


        OWLClass A = df.getOWLClass(IRI.create(":Animal"));
        OWLClass B = df.getOWLClass(IRI.create(":Cat"));
        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":produce_milk"));
        OWLClass C = df.getOWLClass(IRI.create(":Mammal"));
        OWLClassExpression left = df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, B), df.getOWLObjectSomeValuesFrom(R, C));

        // expected axiom
        // r.(B \sqcap C) \sqsubseteq A
        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(df.getOWLObjectSomeValuesFrom(R, df.getOWLObjectIntersectionOf(B, C)), A);
        man.addAxiom(targetOntology, axiom);

        try {
            axiom = baseOracle.mergeLeft(left, A, 1);
            System.out.println("Merge left: " + axiom);
        } catch (Exception e) {
            System.out.println("Error in merge left \n");
            e.printStackTrace();
        }
    }


    @Test
    public void compose() {
        OWLDataFactory df = man.getOWLDataFactory();

        OWLClass A = df.getOWLClass(IRI.create(":Animal"));
        OWLClass B = df.getOWLClass(IRI.create(":Seal"));
        OWLClass C = df.getOWLClass(IRI.create(":Canary"));
        OWLClass D = df.getOWLClass(IRI.create(":Animal"));
        OWLClass E = df.getOWLClass(IRI.create(":Elk"));
        OWLClass F = df.getOWLClass(IRI.create(":Fox"));
        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":has_legs"));

        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(df.getOWLObjectIntersectionOf(A, B, C), df.getOWLObjectIntersectionOf(D, E, F));
        man.addAxiom(targetOntology, axiom);

        // Left composition
        OWLClassExpression ArB = df.getOWLObjectIntersectionOf(A, df.getOWLObjectSomeValuesFrom(R, B));
        axiom = df.getOWLSubClassOfAxiom(ArB, C);
        man.addAxiom(targetOntology, axiom);
        // Expected axiom
        // A \sqcap \exists r.F \sqsubseteq C
        axiom = df.getOWLSubClassOfAxiom(F, B);
        man.addAxiom(targetOntology, axiom);


        // Right decomposition
        OWLClassExpression ErF = df.getOWLObjectIntersectionOf(E, df.getOWLObjectSomeValuesFrom(R, F));
        axiom = df.getOWLSubClassOfAxiom(C, ErF);
        man.addAxiom(targetOntology, axiom);
        // Expected axiom
        // C \sqsubseteq E \sqcap \exists r.B

        try {
            axiom = baseOracle.composeLeft(ArB, C, 1);
            System.out.println("Compose left: " + axiom);
            axiom = baseOracle.composeRight(C, ErF, 1);
            System.out.println("Compose right: " + axiom);
        } catch (Exception e) {
            System.out.println("Error in composition \n");
            e.printStackTrace();
        }
    }
}