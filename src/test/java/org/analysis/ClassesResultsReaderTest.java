package org.analysis;

import org.analysis.classes.ClassesResultsReader;
import org.configurations.Configuration;
import org.experiments.Environment;
import org.experiments.logger.SmartLogger;
import org.experiments.task.ExperimentTask;
import org.experiments.task.Task;
import org.utility.SHA256Hash;
import org.experiments.workload.OllamaWorkload;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ClassesResultsReaderTest {

    private String type;
    private int maxTokens;
    private String model;
    private String ontology;
    private String system;
    private String query;
    private ClassesResultsReader classesResultsReader;

    @Before
    public void setUp() {
        Yaml yaml = new Yaml();
        Configuration config;
        try {
            config = yaml.loadAs(new FileInputStream("src/main/java/org/experiments/classesQueryingConf.yml"), Configuration.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        type = config.getType();
        model = config.getModels().get(0);
        ontology = config.getOntologies().get(0);
        system = config.getSystem();
        maxTokens = config.getMaxTokens();

        runSomeTask();
    }

    private void runSomeTask() {
        String message = "Mammal SubClassOf Animal";
        var work = new OllamaWorkload(model, system, message, maxTokens);
        Task task = new ExperimentTask(type, model, ontology, message, system, work);
        Environment.run(task);

        message = "Backbone SubClassOf Bird";
        work = new OllamaWorkload(model, system, message, maxTokens);
        task = new ExperimentTask(type, model, ontology, message, system, work);
        Environment.run(task);
    }

    @Test
    public void testComputeResultsTrue() {

        query = "Mammal SubClassOf Animal";
        classesResultsReader = new ClassesResultsReader(type, model, ontology, query, system);
        checkFileExists();
        classesResultsReader.computeResults();

        assert (classesResultsReader.getParentClassName().equals("Animal"));
        assert (classesResultsReader.getChildClassName().equals("Mammal"));
        assert (classesResultsReader.getFileNameToAnalyze().equals(SHA256Hash.sha256(type + model + ontology + query + system)));
    }

    @Test
    public void testComputeResultsFalse() {
        query = "Backbone SubClassOf Bird";
        classesResultsReader = new ClassesResultsReader(type, model, ontology, query, system);
        checkFileExists();
        classesResultsReader.computeResults();
        assert (classesResultsReader.getParentClassName().isEmpty());
        assert (classesResultsReader.getChildClassName().isEmpty());
        assert (classesResultsReader.getFileNameToAnalyze().equals(SHA256Hash.sha256(type + model + ontology + query + system)));
    }

    private void checkFileExists() {
        if (SmartLogger.isFileInCache(classesResultsReader.getFileNameToAnalyze())) {
            System.out.println("File is in cache");
        } else {
            throw new RuntimeException("File is not in cache");
        }
    }
}