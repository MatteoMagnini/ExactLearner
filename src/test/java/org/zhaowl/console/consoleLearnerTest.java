package org.zhaowl.console;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.File;
import org.junit.Test;

import static org.junit.Assert.*;

public class consoleLearnerTest {

    @Test
    public void smallOntologiesCorpus() {
        Logger.getRootLogger().setLevel(Level.OFF);
        String path = "src/main/resources/ontologies/small/";

        String[] ontologies = {"animals.owl", "football.owl",  "cell.owl",
                "generations.owl", "university.owl"};
        for (String fn : ontologies) {
            System.out.println("running on " + path + fn);
            String[] args = {path + fn, "on", "t", "t", "t", "t", "t", "t", "t", "t", "t", "t"};
            consoleLearner cl = new consoleLearner();
            cl.doIt(args);
        }
    }

    @Test
    public void mediumOntologiesCorpus() {
        Logger.getRootLogger().setLevel(Level.OFF);
        String path = "src/main/resources/ontologies/medium/";

        String[] ontologies = {"fungal_anatomy.owl",
                "infectious_disease.owl",
                "/space.owl",
                "worm_development.owl"};
        for (String fn : ontologies) {
            System.out.println("running on " + path + fn);
            String[] args = {path + fn, "on", "t", "t", "t", "t", "t", "t", "t", "t", "t", "t"};
            consoleLearner cl = new consoleLearner();
            cl.doIt(args);
        }
    }

    public void smallOntologies() {
        Logger.getRootLogger().setLevel(Level.OFF);
        File dir = new File("src/main/resources/ontologies/small");
        runInFolder(dir);
    }

    private void runInFolder(File dir) {
        System.out.println("Running in " + dir.toString());
        File[] directoryListing = dir.listFiles();
        if(directoryListing != null) {
            for(File ont : directoryListing) {
                System.out.println(ont.toString());
                consoleLearner cl = new consoleLearner();
                String[] args = {ont.toString(), "on", "t", "t", "t", "t", "t", "t", "t", "t", "t", "t"};
                cl.doIt(args);
            }
        }
    }
}