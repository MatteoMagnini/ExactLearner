package org.exactlearner.parser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class OWLParserTest {
    OWLParserImpl parser;
    private final static int ANIMAL_CLASSES_NUMBER = 17;
    private final static int ANIMAL_AXIOMS_NUMBER = 33;

    @Before
    public void setUp() throws OWLOntologyCreationException {
        parser = new OWLParserImpl("src/main/resources/ontologies/small/animals.owl");
        if (parser.getClasses().isEmpty()) {
            Assert.fail("FAILED TO LOAD ANIMAL.OWL");
        }
    }

    @Test
    public void getClassesTest() {
        Assert.assertEquals(ANIMAL_CLASSES_NUMBER, parser.getClasses().get().size());
        System.out.println(parser.getClasses().get());
    }

    @Test
    public void getClassesNamesAsStringTest() {
        Assert.assertEquals(ANIMAL_CLASSES_NUMBER, parser.getClassesNamesAsString().size());
        System.out.println(parser.getClassesNamesAsString());
    }

    @Test
    public void getAxiomsTest() {
        Assert.assertEquals(ANIMAL_AXIOMS_NUMBER, parser.getAxioms().size());
        parser.getAxioms().forEach(a -> {
            System.out.println(a.toString() + "\n");
        });
    }
}