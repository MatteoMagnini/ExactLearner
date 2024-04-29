package org.exactlearner.engine;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;

import java.util.Set;

public interface BaseEngine {

    OWLAxiom getSubClassAxiom(OWLClassExpression classA, OWLClassExpression classB);

    OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom(OWLClassExpression concept1, OWLClassExpression concept2);

    OWLClassExpression getOWLObjectIntersectionOf(Set<OWLClassExpression>mySet);

    Set<OWLClass> getClassesInSignature();

    Boolean entailed(OWLAxiom a);

    Boolean entailed(Set<OWLAxiom> s);
}
