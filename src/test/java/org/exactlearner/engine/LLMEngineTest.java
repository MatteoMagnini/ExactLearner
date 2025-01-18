package org.exactlearner.engine;

import junit.framework.TestCase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LLMEngineTest extends TestCase {
    private final OWLOntologyManager man = OWLManager.createOWLOntologyManager();

    public void testSplitEquivalentInEntailed() throws OWLOntologyCreationException {
        OWLDataFactory df = man.getOWLDataFactory();

        OWLClassExpression a = df.getOWLClass(IRI.create("A"));
        OWLClassExpression b = df.getOWLClass(IRI.create("B"));

        OWLAxiom axiom = df.getOWLEquivalentClassesAxiom(a, b);
        DummyWorkloadManager dummy = new DummyWorkloadManager();

        LLMEngine engine = new LLMEngine(man.createOntology(), man, dummy);
        engine.entailed(axiom);

        assertThat(dummy.getQueries(), hasSize(2));
        assertThat(dummy.getQueries(), contains(
                "A SubClassOf B",
                "B SubClassOf A"
        ));
    }

    public void testSplitAxiomInEntailed() throws OWLOntologyCreationException {
        OWLDataFactory df = man.getOWLDataFactory();

        OWLClassExpression a = df.getOWLClass(IRI.create("A"));
        OWLClassExpression b = df.getOWLClass(IRI.create("B"));
        OWLClassExpression c = df.getOWLClass(IRI.create("C"));

        OWLClassExpression expression = df.getOWLObjectIntersectionOf(b, c);
        OWLAxiom axiom = df.getOWLSubClassOfAxiom(a, expression);
        DummyWorkloadManager dummy = new DummyWorkloadManager();

        LLMEngine engine = new LLMEngine(man.createOntology(), man, dummy);
        engine.entailed(axiom);

        assertThat(dummy.getQueries(), hasSize(2));
        assertThat(dummy.getQueries(), contains(
                "A SubClassOf B",
                "A SubClassOf C"
        ));
    }

    public void testSplitAxiomResponseEntailed() throws OWLOntologyCreationException {
        OWLDataFactory df = man.getOWLDataFactory();

        OWLClassExpression a = df.getOWLClass(IRI.create("A"));
        OWLClassExpression b = df.getOWLClass(IRI.create("B"));
        OWLClassExpression c = df.getOWLClass(IRI.create("C"));

        OWLClassExpression expression = df.getOWLObjectIntersectionOf(b, c);
        OWLAxiom axiom = df.getOWLSubClassOfAxiom(a, expression);

        // Test none are true
        DummyWorkloadManager dummy = new DummyWorkloadManager(s -> false);
        LLMEngine engine = new LLMEngine(man.createOntology(), man, dummy);

        assertThat(engine.entailed(axiom), is(false));

        // Test one is true
        dummy = new DummyWorkloadManager(s -> s.equals("A SubClassOf B"));
        engine = new LLMEngine(man.createOntology(), man, dummy);

        assertThat(engine.entailed(axiom), is(false));

        // Test both are true
        dummy = new DummyWorkloadManager(s -> s.equals("A SubClassOf B") || s.equals("A SubClassOf C"));
        engine = new LLMEngine(man.createOntology(), man, dummy);

        assertThat(engine.entailed(axiom), is(true));
    }
}