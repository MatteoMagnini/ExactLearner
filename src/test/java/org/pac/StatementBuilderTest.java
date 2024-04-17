package org.pac;

import org.analysis.OntologyManipulator;
import org.exactlearner.parser.OWLParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StatementBuilderTest {

    Set<String> classesNames;
    Set<String> objectDataPropertiesNames;
    @Test
    public void testAnimalStatementChecker() {
        // ANIMALS ONTOLOGY
        OWLParser parser = OntologyManipulator.getParser("src/main/resources/ontologies/small/animals.owl");
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectProperties().stream().map(Object::toString).map(s -> s.split("#")[1].replace(">", "")).collect(Collectors.toSet());
        StatementBuilder statementBuilder = new StatementBuilderImpl(classesNames, objectDataPropertiesNames);
        //Animals ontology has 6256 statements (17 classes and 4 object properties):
        // type 1 statement = 17 * 16 * 15 = 4080
        // type 2 and 3 statement = 2 * ( 17 * 4 * 16) = 2176
        Assert.assertEquals(Optional.of(6256).get(), statementBuilder.getNumberOfStatements());
        System.out.println(statementBuilder.chooseRandomStatement());
    }

    @Test
    public void testGenerationsStatementChecker() {
        // GENeRATIONS ONTOLOGY
        OWLParser parser = OntologyManipulator.getParser("src/main/resources/ontologies/small/generations(large).owl");
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectProperties().stream().map(Object::toString).map(s -> s.split("#")[1].replace(">", "")).collect(Collectors.toSet());
        StatementBuilder statementBuilder = new StatementBuilderImpl(classesNames, objectDataPropertiesNames);
        Assert.assertEquals(Optional.of(9880).get(), statementBuilder.getNumberOfStatements());
        System.out.println(statementBuilder.chooseRandomStatement());
    }

    @Test
    public void testCellStatementChecker() {
        // FAMILIES ONTOLOGY
        OWLParser parser = OntologyManipulator.getParser("src/main/resources/ontologies/small/cell.owl");
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectProperties().stream().map(Object::toString).map(s -> s.split("#")[1].replace(">", "")).collect(Collectors.toSet());
        StatementBuilder statementBuilder = new StatementBuilderImpl(classesNames, objectDataPropertiesNames);
        Assert.assertEquals(Optional.of(9240).get(), statementBuilder.getNumberOfStatements());
        System.out.println(statementBuilder.chooseRandomStatement());
    }

    @Test
    public void testUniversityStatementChecker() {
        // UNIVERSITY ONTOLOGY
        OWLParser parser = OntologyManipulator.getParser("src/main/resources/ontologies/small/university.owl");
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectProperties().stream().map(Object::toString).map(s -> s.split("#")[1].replace(">", "")).collect(Collectors.toSet());
        StatementBuilder statementBuilder = new StatementBuilderImpl(classesNames, objectDataPropertiesNames);
        Assert.assertEquals(Optional.of(462).get(), statementBuilder.getNumberOfStatements());
        System.out.println(statementBuilder.chooseRandomStatement());
    }

    @Test
    public void testFootballStatementChecker() {
        // FOOTBALL ONTOLOGY
        OWLParser parser = OntologyManipulator.getParser("src/main/resources/ontologies/small/football.owl");
        classesNames = parser.getClassesNamesAsString();
        objectDataPropertiesNames = parser.getObjectProperties().stream().map(Object::toString).map(s -> s.split("#")[1].replace(">", "")).collect(Collectors.toSet());
        StatementBuilder statementBuilder = new StatementBuilderImpl(classesNames, objectDataPropertiesNames);
        Assert.assertEquals(Optional.of(1260).get(), statementBuilder.getNumberOfStatements());
        System.out.println(statementBuilder.chooseRandomStatement());
    }

}