package org.exactlearner.parser;

import org.semanticweb.owlapi.model.OWLClass;

import java.util.Optional;
import java.util.Set;

public interface OWLParser {
    Optional<Set<OWLClass>> getClasses();
    Set<String> getClassesNamesAsString();
}
