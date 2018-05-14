package org.exactlearner.console;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.File;
import java.util.Arrays;

import org.junit.Test;
import org.junit.Before;

public class consoleLearnerTest {

	@Before
	public void setUp() throws Exception {
		Logger.getRootLogger().setLevel(Level.OFF);
	}

	@Test
	public void allParameterCombinationsAnimal() {

		int numArgs = 6;

		int maxCounter = 2 << (numArgs - 1);
		String[] args = new String[13];
		args[0] = "src/main/resources/ontologies/small/animals.owl";
		args[7] = "0.1";
		args[8] = "0.1";
		args[9] = "0.1";
		args[10] = "0.1";
		args[11] = "0.1";
		args[12] = "0.1";

		for (int counter = 0; counter < maxCounter; counter++) {
			for (int i = 1; i <= numArgs; i++) {
				if ((counter & (1 << (i - 1))) > 0) {
					args[i] = "f";
				} else {
					args[i] = "t";
				}
			}
			consoleLearner cl = new consoleLearner();
			System.out.println(Arrays.toString(args));
			cl.doIt(args);
		}
	}

	@Test
	public void smallOntologiesNamed() {
		Logger.getRootLogger().setLevel(Level.OFF);
		String path = "src/main/resources/ontologies/small/";

		String[] ontologies = { "animals.owl", "football.owl", "cell.owl", "generations.owl", "university.owl" };
		runDoIt(path, ontologies);
	}

	@Test
	public void mediumOntologiesNamed() {
		Logger.getRootLogger().setLevel(Level.OFF);
		String path = "src/main/resources/ontologies/medium/";

		String[] ontologies = { "fungal_anatomy.owl", "infectious_disease.owl", "space.owl", "worm_development.owl" };
		runDoIt(path, ontologies);
	}

	@Test
	public void smallOntologiesCorpus() {
		Logger.getRootLogger().setLevel(Level.OFF);
		File dir = new File("src/main/resources/corpus/small");
		runInFolder(dir);
	}

	private void runDoIt(String path, String[] ontologies) {
		for (String fn : ontologies) {
			System.out.println("running on " + path + fn);

			String[] args = { path + fn, "t", "t", "t", "t", "t", "t", "0.1", "0.1", "0.1", "0.1", "0.1", "0.1" };

			consoleLearner cl = new consoleLearner();
			cl.doIt(args);
		}
	}

	private void runInFolder(File dir) {
		System.out.println("Running in " + dir.toString());
		File[] directoryListing = dir.listFiles(pathname -> pathname.getName().endsWith(".owl"));

		if (directoryListing != null) {
			for (File ont : directoryListing) {
				System.out.println(ont.toString());
				consoleLearner cl = new consoleLearner();

				String[] args = { ont.toString(), "t", "t", "t", "t", "t", "0.t", "0.1", "0.1", "0.1", "0.1", "0.1",
						"0.1" };

				cl.doIt(args);
			}
		}
	}
}