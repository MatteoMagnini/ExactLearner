package org.pac;

import org.exactlearner.parser.OWLParser;
import org.exactlearner.parser.OWLParserImpl;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.util.Set;
import java.util.stream.Collectors;

public class StatementBuilderTest {

    Set<String> classesNames;
    Set<String> objectDataPropertiesNames;
    @Before
    public void setUp() {
        // ANIMALS ONTOLOGY
        OWLParser parser = loadOntology();
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectProperties().stream().map(Object::toString).map(s -> s.split("#")[1].replace(">", "")).collect(Collectors.toSet());
    }

    private static OWLParser loadOntology() {
        OWLParser parser = null;
        try {
            parser = new OWLParserImpl("src/main/resources/ontologies/small/animals.owl");
        } catch (OWLOntologyCreationException e) {
            System.out.println(e.getMessage());
        }
        return parser;
    }

    @Test
    public void testStatementChecker() {
        StatementBuilder statementBuilder = new StatementBuilderImpl(classesNames, objectDataPropertiesNames);
        System.out.println(statementBuilder.chooseRandomStatement());

    }

}