package org.exactlearner.engine;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.Set;

public interface LLMEngine {

    boolean isSubClassOf(OWLClass classA, OWLClass classB);

    OWLAxiom subClassAxiom(OWLClassExpression classA, OWLClassExpression classB);

    boolean runTaskAndGetResult(String message);

    Set<OWLClass> getClassesInSignature();

    boolean entailed(OWLAxiom a);

    boolean entailed(Set<OWLAxiom> s);
}
