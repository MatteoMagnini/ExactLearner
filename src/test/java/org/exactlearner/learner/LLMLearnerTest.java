package org.exactlearner.learner;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.exactlearner.engine.ELEngine;
import org.exactlearner.engine.LLMEngine;
import org.exactlearner.utils.Metrics;
import org.configurations.Configuration;
import org.utility.YAMLConfigLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import static org.junit.Assert.fail;

public class LLMLearnerTest {

    private final OWLObjectRenderer myRenderer =  new ManchesterOWLSyntaxOWLObjectRendererImpl();
    private final Metrics metrics = new Metrics(myRenderer);
    private final OWLOntologyManager man = OWLManager.createOWLOntologyManager();
    private OWLOntology targetOntology = null;
    private OWLOntology hypothesisOntology = null;
    private LLMEngine elQueryEngineForT = null;
    private ELEngine elQueryEngineForH = null;
    private BaseLearner baseLearner = null;



    @Before
    public void setUp() throws Exception {
        Logger.getRootLogger().setLevel(Level.OFF);

        targetOntology = man.createOntology();
        hypothesisOntology = man.createOntology();
        var config = new YAMLConfigLoader().getConfig("src/main/java/org/pac/statementsQueryingConf.yml", Configuration.class);
        elQueryEngineForT = new LLMEngine(targetOntology, config.getModels().get(3), config.getSystem(), config.getMaxTokens());
        elQueryEngineForH = new ELEngine(hypothesisOntology);
        baseLearner = new Learner(elQueryEngineForT, elQueryEngineForH, metrics);

    }

    @Test
    public void learnerSiblingMerge1() {
        OWLDataFactory df = man.getOWLDataFactory();
        OWLClass A = df.getOWLClass(IRI.create(":Cat"));
        OWLClass B = df.getOWLClass(IRI.create(":Mammal"));
        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":has_legs"));
        OWLClass C = df.getOWLClass(IRI.create(":Feline"));
        OWLClassExpression right = df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, C), df.getOWLObjectSomeValuesFrom(R, B), df.getOWLObjectSomeValuesFrom(R,A));
        OWLSubClassOfAxiom axiom;
        OWLSubClassOfAxiom mergedAxiom= df.getOWLSubClassOfAxiom(A, df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, df.getOWLObjectIntersectionOf(B,C)),df.getOWLObjectSomeValuesFrom(R,A)));
        man.addAxiom(targetOntology, mergedAxiom);

        try {
            axiom = baseLearner.mergeRight(A, right);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Merged: " + axiom);
        Assert.assertEquals(mergedAxiom, axiom);
    }
    @Test
    public void unsaturateLeft() {
        OWLDataFactory df = man.getOWLDataFactory();
        // Create 6 classes to target ontology
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
            axiom = baseLearner.unsaturateLeft(ABCDEF, A);

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

        OWLClass A = df.getOWLClass(IRI.create(":Albatross"));
        OWLClass B = df.getOWLClass(IRI.create(":Animal"));
        OWLClass C = df.getOWLClass(IRI.create(":Canary"));


        OWLClass D = df.getOWLClass(IRI.create(":Bird"));
        OWLClass E = df.getOWLClass(IRI.create(":Lizard"));
        OWLClass F = df.getOWLClass(IRI.create(":Snake"));

        OWLClass G = df.getOWLClass(IRI.create(":Goat"));
        OWLClass H = df.getOWLClass(IRI.create(":Horse"));

        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":has_legs"));


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
            axiom = baseLearner.decomposeLeft(ArB, C);
            System.out.println("Decompose left: " + axiom);

            axiom = baseLearner.decomposeRight(E, FrG);
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
        OWLClass A = df.getOWLClass(IRI.create(":Albatross"));
        OWLClass B = df.getOWLClass(IRI.create(":Bird"));
        OWLClass C = df.getOWLClass(IRI.create(":Canary"));


        OWLClass D = df.getOWLClass(IRI.create(":Raven"));
        OWLClass E = df.getOWLClass(IRI.create(":Animal"));
        OWLClass F = df.getOWLClass(IRI.create(":Falcon"));
        OWLClassExpression ABC = df.getOWLObjectIntersectionOf(A,B,C);
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
            axiom = baseLearner.saturateRight(A, BC);

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


        OWLClass A = df.getOWLClass(IRI.create(":Cat"));
        OWLClass B = df.getOWLClass(IRI.create(":Animal"));
        OWLObjectProperty R = df.getOWLObjectProperty(IRI.create(":has_legs"));
        OWLClass C = df.getOWLClass(IRI.create(":Mammal"));
        OWLClassExpression right = df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, C), df.getOWLObjectSomeValuesFrom(R, B), df.getOWLObjectSomeValuesFrom(R,A));
        OWLSubClassOfAxiom axiom;
        OWLSubClassOfAxiom mergedAxiom= df.getOWLSubClassOfAxiom(A, df.getOWLObjectIntersectionOf(df.getOWLObjectSomeValuesFrom(R, df.getOWLObjectIntersectionOf(B,C)),df.getOWLObjectSomeValuesFrom(R,A)));
        man.addAxiom(targetOntology, mergedAxiom);
        try {
            axiom = baseLearner.mergeRight(A, right);
            System.out.println("Merged: " + axiom);
            Assert.assertEquals(mergedAxiom, axiom);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void branchLeft() {
        OWLDataFactory df = man.getOWLDataFactory();

        OWLClass A = df.getOWLClass(IRI.create(":A"));
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
            axiom = baseLearner.branchLeft(left, A);
            System.out.println("Branched: " + axiom);
            if(!axiom.equals(branchedAxiom))
                fail("Did not branch.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
