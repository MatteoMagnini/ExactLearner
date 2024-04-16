package org.pac;

import java.util.Set;

public interface StatementBuilder {

    String chooseRandomStatement();

    Integer getNumberOfStatements();

    Set<String> getAllStatements();
}
