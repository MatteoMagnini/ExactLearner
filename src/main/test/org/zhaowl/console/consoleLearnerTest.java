package org.zhaowl.console;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.File;
import org.junit.Test;

import static org.junit.Assert.*;

public class consoleLearnerTest {

    @Test
    public void smallOntologies() {
        Logger.getRootLogger().setLevel(Level.OFF);
        File dir = new File("src/main/resources/ontologies/small");
        runInFolder(dir);
    }

    private void runInFolder(File dir) {
        File[] directoryListing = dir.listFiles();
        System.out.println("Running in " + directoryListing.toString());
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