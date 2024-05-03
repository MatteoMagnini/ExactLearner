package org.exactlearner.learner;

import org.exactlearner.engine.BaseEngine;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public interface BaseLearner {
    OWLSubClassOfAxiom decompose(OWLClassExpression left, OWLClassExpression right) throws Exception;

    OWLSubClassOfAxiom decomposeLeft(OWLClassExpression expression, OWLClass cl) throws Exception;

    OWLSubClassOfAxiom decomposeRight(OWLClass cl, OWLClassExpression expression) throws Exception;

    OWLSubClassOfAxiom unsaturateLeft(OWLClassExpression expression, OWLClass cl) throws Exception;

    OWLSubClassOfAxiom saturateRight(OWLClass cl, OWLClassExpression expression) throws Exception;

    OWLSubClassOfAxiom mergeRight(OWLClass cl, OWLClassExpression expression) throws Exception;

    OWLSubClassOfAxiom branchLeft(OWLClassExpression expression, OWLClass cl) throws Exception;

    int getNumberUnsaturations();

    int getNumberSaturations();

    int getNumberMerging();

    int getNumberBranching();

    int getNumberLeftDecomposition();

    int getNumberRightDecomposition();

    void minimiseHypothesis(BaseEngine elQueryEngineForH, OWLOntology hypothesisOntology);
}
