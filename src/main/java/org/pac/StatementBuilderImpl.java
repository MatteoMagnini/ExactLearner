package org.pac;

import java.util.HashSet;
import java.util.Set;

public class StatementBuilderImpl implements StatementBuilder {
    private final Set<String> classes;
    private final Set<String> objectProperties;
    private final Set<String> generatedStatementsType1 = new HashSet<>();
    private final Set<String> generatedStatementsType2 = new HashSet<>();
    private final Set<String> generatedStatementsType3 = new HashSet<>();

    public StatementBuilderImpl(Set<String> classes, Set<String> objectProperties) {
        this.classes = classes;
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
            classes.stream().filter(c2 -> !c2.equals(c1)).forEach(c2 -> classes.stream()
                    .filter(c3 -> !c3.equals(c1) && !c3.equals(c2))
                    .forEach(c3 -> generatedStatementsType1.add("( " + c1 + " and " + c2 + " ) SubClassOf: " + c3)));
        });
    }

    // Type 2: B ⊑ ∃R.A
    private void createStatementsType2() {
        classes.forEach(c1 -> {
            classes.stream().filter(c2 -> !c2.equals(c1)).forEach(c2 -> {
                objectProperties.forEach(o -> generatedStatementsType2.add(c1 + " SubClassOf: ( " + o + " some " + c2 + " )"));
            });
        });
    }

    // Type 3: ∃R.A ⊑ B
    private void createStatementsType3() {
        classes.forEach(c1 -> {
            classes.stream().filter(c2 -> !c2.equals(c1)).forEach(c2 -> {
                objectProperties.forEach(o -> generatedStatementsType3.add("( " + o + " some " + c1 + " ) SubClassOf: " + c2));
            });
        });
    }


    @Override
    public String chooseRandomStatement() {
        Set<String> allStatements = new HashSet<>();
        allStatements.addAll(generatedStatementsType1);
        allStatements.addAll(generatedStatementsType2);
        allStatements.addAll(generatedStatementsType3);
        //RANDOM PICK DOESN'T CONSIDER THE SIZE OF EACH SET, WE WANT UNIFORM DISTRIBUTION
        int randomIndex = (int) (Math.random() * allStatements.size());
        return (String) allStatements.toArray()[randomIndex];
    }

    @Override
    public Integer getNumberOfStatements() {
        Set<String> allStatements = new HashSet<>();
        allStatements.addAll(generatedStatementsType1);
        allStatements.addAll(generatedStatementsType2);
        allStatements.addAll(generatedStatementsType3);
        return allStatements.size();
    }

}
