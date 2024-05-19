package org.pac;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Pac {

    private Double epsilon;
    private Double delta;
    private Long numberOfExamples;

    private Set<String> pacStatements;

    public Pac(Integer numberOfExamples, Double epsilon, Double delta, Integer hypothesisSize, Set<String> allStatements, int seed) {
        this.epsilon = epsilon;
        this.delta = delta;
        this.numberOfExamples = Math.round((hypothesisSize*Math.log(numberOfExamples) - Math.log(delta)) / epsilon);
        // Select a random subset of size numberOfExamples from allStatements
        // using the seed to ensure reproducibility
        List<Integer> indices = IntStream.range(0, allStatements.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(indices, new Random(seed));
        this.pacStatements = indices.stream().limit(this.numberOfExamples).map(i -> (String) allStatements.toArray()[i]).collect(Collectors.toSet());
    }

    public double getEpsilon() {
        return epsilon;
    }

    public double getDelta() {
        return delta;
    }

    public long getNumberOfExamples() {
        return numberOfExamples;
    }

    public Set<String> getPacStatements() {
        return pacStatements;
    }

    public int getPacStatementsSize() {
        return pacStatements.size();
    }

    public String getRandomStatement() {
        String statement = pacStatements.stream().findAny().orElse(null);
        pacStatements.remove(statement);
        return statement;
    }

}
