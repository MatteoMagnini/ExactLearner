package org.pac;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Pac {

    private Double epsilon;
    private Double delta;
    private Long numberOfExamples;

    private Set<String> pacStatements;

    public Pac(Integer numberOfExamples, Double epsilon, Double delta, Integer hypothesisSize, Set<String> allStatements) {
        this.epsilon = epsilon;
        this.delta = delta;
        this.numberOfExamples = Math.round((hypothesisSize*Math.log(numberOfExamples) - Math.log(delta)) / epsilon);
        // Select a random subset of size numberOfExamples from allStatements
        this.pacStatements = new HashSet<>(allStatements);
        this.pacStatements = this.pacStatements.stream().limit(this.numberOfExamples).collect(Collectors.toSet());
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
