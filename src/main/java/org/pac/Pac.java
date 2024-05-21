package org.pac;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.pow;

public class Pac {

    private List<String> classes;
    private List<String> objectProperties;
    private Double epsilon;
    private Double delta;
    private Long numberOfSamples;
    private int seed = 0;

    private Long providedSamples = 0L;

    public Pac(Set<String> classes, Set<String> objectProperties, Double epsilon, Double delta, Integer hypothesisSize, int seed) {
        // Alphabetically sort classes and then shuffle them using the seed to ensure reproducibility
        this.classes = classes.stream().filter(s -> !s.toLowerCase().contains("thin")).distinct().collect(Collectors.toList());
        this.objectProperties = new ArrayList<>(objectProperties);
        this.epsilon = epsilon;
        this.delta = delta;
        this.numberOfSamples = Math.round((hypothesisSize*Math.log(computeInstanceSpaceSize()) - Math.log(delta)) / epsilon);
        this.seed = seed;

        Collections.sort(this.classes);
        Collections.sort(this.objectProperties);
        Collections.shuffle(this.classes, new Random(seed));
        Collections.shuffle(this.objectProperties, new Random(seed));
    }

    public double getEpsilon() {
        return epsilon;
    }

    public double getDelta() {
        return delta;
    }

    public long getNumberOfSamples() {
        return numberOfSamples;
    }

    public double getNumberOfProvidedSamples() {
        return providedSamples;
    }

    public String getRandomStatement() {
        /*
          Pick up a random statement from the list of all possible statements with uniform probability.
          Use the seed and the current number of provided samples to ensure reproducibility.
          There are three types of statements:
          1. (A ∩ B) ⊑ C
          2. B ⊑ ∃R.A
          3. ∃R.A ⊑ B
          Generate 3 indices each for class and/or object property and use them to create a statement.
          If the indices generated an invalid statement, regenerate them until a valid statement is created.
          Increment the number of provided samples.
          Return the generated statement
         */
        Random rand = new Random(seed + providedSamples);
        int maxRange = classes.size() + objectProperties.size();
        int index1, index2, index3;
        String statement = null;
        while (statement == null) {
            index1 = rand.nextInt(maxRange);
            index2 = rand.nextInt(maxRange);
            index3 = rand.nextInt(maxRange);
            if (index1 < classes.size() && index2 < classes.size() && index3 < classes.size()) {
                statement = "( " + classes.get(index1) + " and " + classes.get(index2) + " ) SubClassOf: " + classes.get(index3);
            } else if (index1 < classes.size() && index2 >= classes.size() && index3 < classes.size()) {
                statement = classes.get(index1) + " SubClassOf: ( " + objectProperties.get(index2 - classes.size()) + " some " + classes.get(index3) + " )";
            } else if (index1 >= classes.size() && index2 < classes.size() && index3 < classes.size()) {
                statement = "( " + objectProperties.get(index1 - classes.size()) + " some " + classes.get(index2) + " ) SubClassOf: " + classes.get(index3);
            }
        }
        providedSamples++;
        return statement;
    }

    private double computeInstanceSpaceSize() {
        return pow(this.classes.size(), 3) + 2 * (pow(this.classes.size(), 2) * this.objectProperties.size());
    }

}
