package org.analysis;

import org.analysis.exp1.classes.JenaQueryClassesExecutor;
import org.configurations.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;

public class JenaQueryExecutorTest {

    private String type;
    private int maxTokens;
    private String model;
    private String ontology;
    private String system;

    @Before
    public void setUp() {
        Yaml yaml = new Yaml();
        Configuration config;
        try {
            config = yaml.loadAs(new FileInputStream("src/main/java/org/configurations/classesQueryingConf.yml"), Configuration.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        type = config.getType();
        model = config.getModels().get(0);
        ontology = config.getOntologies().get(0);
        system = config.getSystem();
        maxTokens = config.getMaxTokens();
    }

    @Test
    public void testExecuteClassesQuerying() {
        String query = "Mammal SubClassOf Animal";
        String ontology = this.ontology;
        JenaQueryClassesExecutor jenaQueryExecutor = new JenaQueryClassesExecutor(query, ontology);
        assertEquals("false", jenaQueryExecutor.executeQuery());

        query = "Bird SubClassOf Vertebrate";
        jenaQueryExecutor = new JenaQueryClassesExecutor(query, ontology);
        assertEquals("true", jenaQueryExecutor.executeQuery());
    }
}