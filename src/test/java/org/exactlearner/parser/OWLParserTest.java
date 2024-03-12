package org.exactlearner.parser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class OWLParserTest {
    OWLParserImpl parser;
    private final static int ANIMAL_CLASSES_NUMBER = 17;
    @Before
    public void setUp() throws OWLOntologyCreationException {
        parser = new OWLParserImpl("src/main/resources/ontologies/small/animals.owl");
    }
    @Test
    public void getClassesTest() {
        if(!parser.getClasses().isPresent()){Assert.fail("FAILED TO LOAD ANIMAL.OWL");}
        Assert.assertEquals(ANIMAL_CLASSES_NUMBER, parser.getClasses().get().size());
        System.out.println(parser.getClasses().get());
    }

    @Test
    public void getClassesNamesAsStringTest() {
        if(!parser.getClasses().isPresent()){Assert.fail("FAILED TO LOAD ANIMAL.OWL");}
        Assert.assertEquals(ANIMAL_CLASSES_NUMBER, parser.getClassesNamesAsString().size());
        System.out.println(parser.getClassesNamesAsString());
    }
}