package org.exactlearner.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class Metrics {
    private final OWLObjectRenderer myRenderer;
    private int membCount = 0;
    private int equivCount = 0;
    private int sizeOfTargetLargestConcept = 0;
    private int sizeOfHypothesisLargestConcept = 0;
    private int sumSizeOfLargestConcept = 0;
    private int depthOfLargestConcept = 0;
    private int sizeOfLargestCounterExample = 0;
    private int sizeOfHypothesis = 0;
    private int sizeOfTarget = 0;

    public Metrics(OWLObjectRenderer renderer) {
        this.myRenderer = renderer;
    }

    private int sizeOfCIT(Set<OWLLogicalAxiom> axSet) {

        int ontSize = 0;

        for (OWLAxiom axe : axSet) {

            String inclusion = myRenderer.render(axe);

            if (inclusion.contains("SubClassOf") || inclusion.contains("EquivalentTo")) {
                inclusion = inclusion.replaceAll(" and ", " ");
                inclusion = inclusion.replaceAll(" some ", " ");
                inclusion = inclusion.replaceAll("SubClassOf", " ");
                inclusion = inclusion.replaceAll("EquivalentTo", " ");
                ontSize += inclusion.split(" ").length;
            }

        }
        return ontSize;
    }

    private int sizeOfConcept(Set<OWLLogicalAxiom> axSet) {
        int largestConceptSize = 0;

        for (OWLAxiom axe : axSet) {
            if (axe.isOfType(AxiomType.SUBCLASS_OF)) {
                OWLSubClassOfAxiom axiom = (OWLSubClassOfAxiom) axe;
                axiom.getSubClass();

                String left = myRenderer.render(axiom.getSubClass());
                String right = myRenderer.render(axiom.getSuperClass());

                left = left.replaceAll(" and ", " ");
                left = left.replaceAll(" some ", " ");

                if (left.split(" ").length > largestConceptSize) {
                    largestConceptSize = left.split(" ").length;
                }

                right = right.replaceAll(" and ", " ");
                right = right.replaceAll(" some ", " ");
                if (right.split(" ").length > largestConceptSize) {
                    largestConceptSize = right.split(" ").length;
                }

            }
            if (axe.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                OWLEquivalentClassesAxiom axiom = (OWLEquivalentClassesAxiom) axe;
                String concept;
                for (OWLClassExpression exp : axiom.getClassExpressions()) {
                    concept = myRenderer.render(exp);
                    concept = concept.replaceAll(" and ", " ");
                    concept = concept.replaceAll(" some ", " ");
                    if (concept.split(" ").length > largestConceptSize)
                        largestConceptSize = concept.split(" ").length;
                }
            }
        }
        return largestConceptSize;

    }

    public int sumOfSizeOfConcept(OWLOntology ontology) {
        int largestConceptSize = 0;
        Set<OWLLogicalAxiom> axSet = ontology.getLogicalAxioms();
        for (OWLClass cl : ontology.getClassesInSignature()) {

            int tmp = 0;

            for (OWLAxiom axe : axSet) {
                if (axe.isOfType(AxiomType.SUBCLASS_OF)) {
                    OWLSubClassOfAxiom axiom = (OWLSubClassOfAxiom) axe;
                    if (axiom.getSubClass().equals(cl)) {

                        String right = myRenderer.render(axiom.getSuperClass());

                        right = right.replaceAll(" and ", " ");
                        right = right.replaceAll(" some ", " ");

                        tmp = +right.split(" ").length;
                    }
                }
                if (axe.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                    OWLEquivalentClassesAxiom axiom = (OWLEquivalentClassesAxiom) axe;
                    String concept;
                    if (axiom.contains(cl)) {
                        for (OWLClassExpression exp : axiom.getClassExpressions()) {
                            if (!exp.equals(cl)) {
                                concept = myRenderer.render(exp);
                                concept = concept.replaceAll(" and ", " ");
                                concept = concept.replaceAll(" some ", " ");

                                tmp = +concept.split(" ").length;
                            }
                        }
                    }
                }
            }
            if (tmp > largestConceptSize)
                largestConceptSize = tmp;
        }
        return largestConceptSize;

    }

    public ArrayList<String> getSuggestionNames(String s, File newFile) throws IOException {

        ArrayList<String> names = new ArrayList<>();

        FileInputStream in = new FileInputStream(newFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line = reader.readLine();
        if (s.equals("concept")) {
            while (line != null) {
                if (line.startsWith("Class:")) {
                    String conceptName = line.substring(7);
                    if (!conceptName.equals("owl:Thing")) {
                        names.add(conceptName);
                    }
                }
                line = reader.readLine();
            }
        } else if (s.equals("role")) {
            while (line != null) {
                if (line.startsWith("ObjectProperty:")) {
                    String roleName = line.substring(16);
                    names.add(roleName);

                }

                line = reader.readLine();
            }
        }
        reader.close();
        return names;
    }

    public int getMembCount() {
        return membCount;
    }

    public void setMembCount(int membCount) {
        this.membCount = membCount;
    }

    public int getEquivCount() {
        return equivCount;
    }

    public void setEquivCount(int equivCount) {
        this.equivCount = equivCount;
    }

    public int getSizeOfTargetLargestConcept() {
        return sizeOfTargetLargestConcept;
    }

    private void setSizeOfTargetLargestConcept(int sizeOfLargestConcept) {
        this.sizeOfTargetLargestConcept = sizeOfLargestConcept;
    }

    public int getSizeOfHypothesisLargestConcept() {
        return sizeOfHypothesisLargestConcept;
    }

    private void setSizeOfHypothesisLargestConcept(int sizeOfLargestConcept) {
        this.sizeOfHypothesisLargestConcept = sizeOfLargestConcept;
    }

    public int getDepthOfLargestConcept() {
        return depthOfLargestConcept;
    }

    public void setDepthOfLargestConcept(int depthOfLargestConcept) {
        this.depthOfLargestConcept = depthOfLargestConcept;
    }

    public int getSizeOfHypothesis() {
        return sizeOfHypothesis;
    }

    public void setSizeOfHypothesis(int sizeOfHypothesis) {
        this.sizeOfHypothesis = sizeOfHypothesis;
    }

    public int getSizeOfTarget() {
        return sizeOfTarget;
    }

    private void setSizeOfTarget(int sizeOfTarget) {
        this.sizeOfTarget = sizeOfTarget;
    }

    public void computeTargetSizes(OWLOntology ontology) {
        Set<OWLLogicalAxiom> logicalAxioms = ontology.getLogicalAxioms();
        this.setSizeOfTarget(sizeOfCIT(logicalAxioms));
        this.setSizeOfTargetLargestConcept(sizeOfConcept(logicalAxioms));
        //this.setSumSizeOfLargestConcept(sumOfSizeOfConcept(ontology));
    }

    public void computeHypothesisSizes(OWLOntology ontology) {
        Set<OWLLogicalAxiom> logicalAxioms = ontology.getLogicalAxioms();
        this.sizeOfHypothesis = sizeOfCIT(logicalAxioms);
        this.setSizeOfHypothesisLargestConcept(sizeOfConcept(logicalAxioms));
    }

    public int getSumSizeOfLargestConcept() {
        return sumSizeOfLargestConcept;
    }

    public void setSumSizeOfLargestConcept(int sumSizeOfLargestConcept) {
        this.sumSizeOfLargestConcept = sumSizeOfLargestConcept;
    }


    public int getSizeOfLargestCounterExample() {
        return sizeOfLargestCounterExample;
    }

    public void setSizeOfLargestCounterExample(int sizeOfLargestCounterExample) {
        this.sizeOfLargestCounterExample = sizeOfLargestCounterExample;
    }

    public int getSizeOfCounterexample(OWLLogicalAxiom axe) {


        String inclusion = myRenderer.render(axe);

        if (inclusion.contains("SubClassOf") || inclusion.contains("EquivalentTo")) {
            inclusion = inclusion.replaceAll(" and ", " ");
            inclusion = inclusion.replaceAll(" some ", " ");
            inclusion = inclusion.replaceAll("SubClassOf", " ");
            inclusion = inclusion.replaceAll("EquivalentTo", " ");
            return inclusion.split(" ").length;
        }
        // else the axiom is not of one of the types above.
        // Let's not count it
        return 0;

    }
}
