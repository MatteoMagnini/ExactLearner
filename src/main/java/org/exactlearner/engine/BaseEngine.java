package org.exactlearner.engine;

import org.semanticweb.owlapi.model.*;

import java.util.Set;

public interface BaseEngine {

    OWLSubClassOfAxiom getSubClassAxiom(OWLClassExpression classA, OWLClassExpression classB);

    OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom(OWLClassExpression concept1, OWLClassExpression concept2);

    OWLClassExpression getOWLObjectIntersectionOf(Set<OWLClassExpression> mySet);

    Set<OWLClass> getClassesInSignature();

    Boolean entailed(OWLAxiom a);

    Boolean entailed(Set<OWLAxiom> s);

    OWLOntology getOntology();
}
