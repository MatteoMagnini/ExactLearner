package org.pac;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class StatementBuilderImpl implements StatementBuilder {
    private final Set<String> classes;
    private final Set<String> objectProperties;
    private final Set<String> generatedStatementsType1 = new HashSet<>();
    private final Set<String> generatedStatementsType2 = new HashSet<>();
    private final Set<String> generatedStatementsType3 = new HashSet<>();
    private final Random rand;

    public StatementBuilderImpl(Integer seed, Set<String> classes, Set<String> objectProperties) {
        rand = new Random(seed);
        this.classes = classes.stream().filter(s-> !s.contains("Thin")).collect(java.util.stream.Collectors.toSet());
        this.objectProperties = objectProperties;
        generateStatements();
    }

    private void generateStatements() {
        createStatementsType1();
        createStatementsType2();
        createStatementsType3();
    }

    // Type 1: (A ∩ B) ⊑ C
    private void createStatementsType1() {
        classes.forEach(c1 -> {
            classes.forEach(c2 -> classes
                    .forEach(c3 -> generatedStatementsType1.add("( " + c1 + " and " + c2 + " ) SubClassOf: " + c3)));
        });
    }

    // Type 2: B ⊑ ∃R.A
    private void createStatementsType2() {
        classes.forEach(c1 -> {
            classes.forEach(c2 -> {
                objectProperties.forEach(o -> generatedStatementsType2.add(c1 + " SubClassOf: ( " + o + " some " + c2 + " )"));
            });
        });
    }

    // Type 3: ∃R.A ⊑ B
    private void createStatementsType3() {
        classes.forEach(c1 -> {
            classes.forEach(c2 -> {
                objectProperties.forEach(o -> generatedStatementsType3.add("( " + o + " some " + c1 + " ) SubClassOf: " + c2));
            });
        });
    }


    @Override
    public String chooseRandomStatement() {
        try {
            return uniformPick().get();
        } catch (RuntimeException e) {
            throw new RuntimeException("No statements found");
        }
    }

    private Optional<String> uniformPick() {
        return getAllStatements().stream().skip(rand.nextInt(getAllStatements().size())).findFirst();
    }

    @Override
    public Integer getNumberOfStatements() {
        return getAllStatements().size();
    }

    @Override
    public Set<String> getAllStatements() {
        Set<String> allStatements = new HashSet<>();
        allStatements.addAll(generatedStatementsType1);
        allStatements.addAll(generatedStatementsType2);
        allStatements.addAll(generatedStatementsType3);
        return allStatements;
    }

}
