package org.pac;

import org.exactlearner.parser.OWLParser;
import org.exactlearner.parser.OWLParserImpl;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StatementBuilderTest {

    Set<String> classesNames;
    Set<String> objectDataPropertiesNames;
    @Test
    public void testAnimalStatementChecker() {
        // ANIMALS ONTOLOGY
        OWLParser parser = new OWLParserImpl("src/main/resources/ontologies/small/animals.owl", OWLManager.createOWLOntologyManager());
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectPropertiesAsString();
        StatementBuilder statementBuilder = new StatementBuilderImpl(0,classesNames, objectDataPropertiesNames);
        Assert.assertEquals(Optional.of(7225).get(), statementBuilder.getNumberOfStatements());
        System.out.println(statementBuilder.chooseRandomStatement());
    }

    @Test
    public void testGenerationsStatementChecker() {
        // GENeRATIONS ONTOLOGY
        OWLParser parser = new OWLParserImpl("src/main/resources/ontologies/small/generations.owl",OWLManager.createOWLOntologyManager());
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectPropertiesAsString();
        StatementBuilder statementBuilder = new StatementBuilderImpl(0,classesNames, objectDataPropertiesNames);
        Assert.assertEquals(Optional.of(9747).get(), statementBuilder.getNumberOfStatements());
        System.out.println(statementBuilder.chooseRandomStatement());
    }

    @Test
    public void testCellStatementChecker() {
        // FAMILIES ONTOLOGY
        OWLParser parser = new OWLParserImpl("src/main/resources/ontologies/small/cl.owl", OWLManager.createOWLOntologyManager());
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectPropertiesAsString();
        StatementBuilder statementBuilder = new StatementBuilderImpl(0,classesNames, objectDataPropertiesNames);
        Assert.assertEquals(Optional.of(10648).get(), statementBuilder.getNumberOfStatements());
        System.out.println(statementBuilder.chooseRandomStatement());
    }

    @Test
    public void testUniversityStatementChecker() {
        // UNIVERSITY ONTOLOGY
        OWLParser parser = new OWLParserImpl("src/main/resources/ontologies/small/university.owl", OWLManager.createOWLOntologyManager());
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectPropertiesAsString();
        StatementBuilder statementBuilder = new StatementBuilderImpl(0,classesNames, objectDataPropertiesNames);
        Assert.assertEquals(Optional.of(637).get(), statementBuilder.getNumberOfStatements());
        System.out.println(statementBuilder.chooseRandomStatement());
    }

    @Test
    public void testFootballStatementChecker() {
        // FOOTBALL ONTOLOGY
        OWLParser parser = new OWLParserImpl("src/main/resources/ontologies/small/football.owl", OWLManager.createOWLOntologyManager());
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectPropertiesAsString();
        StatementBuilder statementBuilder = new StatementBuilderImpl(0,classesNames, objectDataPropertiesNames);
        Assert.assertEquals(Optional.of(1215).get(), statementBuilder.getNumberOfStatements());
        System.out.println(statementBuilder.chooseRandomStatement());
    }

}