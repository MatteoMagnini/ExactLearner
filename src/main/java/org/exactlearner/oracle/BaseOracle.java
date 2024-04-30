package org.exactlearner.oracle;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public interface BaseOracle {
    OWLSubClassOfAxiom unsaturateRight(OWLClassExpression cl, OWLClassExpression expression, double bound)
            throws Exception;

    OWLSubClassOfAxiom saturateLeft(OWLClassExpression expression, OWLClassExpression cl, double bound)
            throws Exception;

    OWLSubClassOfAxiom mergeLeft(OWLClassExpression expression, OWLClassExpression cl, double bound)
            throws Exception;

    OWLSubClassOfAxiom branchRight(OWLClassExpression cl, OWLClassExpression expression, double bound)
            throws Exception;

    OWLSubClassOfAxiom composeLeft(OWLClassExpression expression, OWLClassExpression cl, double bound)
            throws Exception;

    OWLSubClassOfAxiom composeRight(OWLClassExpression cl, OWLClassExpression expression, double bound)
            throws Exception;

    // at the moment duplicated
    Boolean isCounterExample(OWLClassExpression left, OWLClassExpression right);

    int getNumberUnsaturations();

    int getNumberSaturations();

    int getNumberMerging();

    int getNumberBranching();

    int getNumberLeftComposition();

    int getNumberRightComposition();
}
