package org.pac;

import org.apache.jena.sparql.algebra.Op;

import java.util.Optional;
import java.util.Set;

public interface StatementBuilder {

    Optional<String> chooseRandomStatement();

    Integer getNumberOfStatements();

    Set<String> getAllStatements();
}
