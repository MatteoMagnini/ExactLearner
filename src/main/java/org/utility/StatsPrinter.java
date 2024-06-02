package org.utility;

import org.exactlearner.learner.Learner;
import org.exactlearner.oracle.Oracle;
import org.exactlearner.utils.Metrics;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;

import java.io.File;
import java.util.Arrays;

public class StatsPrinter {
    public static double totalSat = 0;
    public static double totalDesat = 0;
    public static double totalRDecomp = 0;
    public static double totalLDecomp = 0;
    public static double totalMerge = 0;
    public static double totalBranch = 0;

    public static void printStat(String description, String value, boolean verb) {
        if (verb) {
            System.out.print(description);
            System.out.println(value);
        } else {
            System.out.print(", " + value);
        }
    }

    public static void printStat(String description, int value, boolean verb) {
        printStat(description, String.valueOf(value), verb);
    }

    public static void printStat(String description, boolean verb) {
        if (verb) {
            printStat(description, " ", verb);
        }
    }

    public static void printStats(long timeStart, long timeEnd, String[] args, boolean verb, File targetFile,
                                  Metrics myMetrics, Learner baseLearner, Oracle baseOracle,
                                  int conceptNumber, int roleNumber, OWLOntology targetOntology,
                                  OWLOntology hypothesisOntology) {
        if (!verb) {
            System.out.print(targetFile.getName());
            Arrays.stream(args).skip(1).forEach(x -> System.out.print(", " + x));
        }
        printStat("Total time (ms): ", String.valueOf(timeEnd - timeStart), verb);

        printStat("Total membership queries: ", myMetrics.getMembCount(), verb);
        printStat("Total equivalence queries: ", myMetrics.getEquivCount(), verb);

        printLearnerStats(baseLearner, verb);
        printOracleStats(baseOracle, verb);
        printOntologySizes(targetOntology, hypothesisOntology, myMetrics, verb, conceptNumber, roleNumber);
    }

    private static void printLearnerStats(Learner baseLearner, boolean verb) {
        var lComp = baseLearner.getNumberLeftDecomposition();
        var tComp = baseLearner.getNumberRightDecomposition();
        var merge = baseLearner.getNumberMerging();
        var branch = baseLearner.getNumberBranching();
        var sat = baseLearner.getNumberSaturations();
        var unsat = baseLearner.getNumberUnsaturations();
        var totalOps = lComp + tComp + merge + branch + sat + unsat;
        if(totalOps != 0){
            totalLDecomp += (double) lComp / totalOps;
            totalRDecomp += (double) tComp / totalOps;
            totalMerge += (double) merge / totalOps;
            totalBranch += (double) branch / totalOps;
            totalSat += (double) sat / totalOps;
            totalDesat += (double) unsat / totalOps;
        }
        printStat("\nLearner Stats:", verb);
        printStat("Total left decompositions: ", baseLearner.getNumberLeftDecomposition(), verb);
        printStat("Total right decompositions: ", baseLearner.getNumberRightDecomposition(), verb);
        printStat("Total mergings: ", baseLearner.getNumberMerging(), verb);
        printStat("Total branchings: ", baseLearner.getNumberBranching(), verb);
        printStat("Total saturations: ", baseLearner.getNumberSaturations(), verb);
        printStat("Total unsaturations: ", baseLearner.getNumberUnsaturations(), verb);
    }

    private static void printOracleStats(Oracle baseOracle, boolean verb) {
        printStat("\nOracle Stats:", verb);
        printStat("Total left compositions: ", baseOracle.getNumberLeftComposition(), verb);
        printStat("Total right compositions: ", baseOracle.getNumberRightComposition(), verb);
        printStat("Total mergings: ", baseOracle.getNumberMerging(), verb);
        printStat("Total branchings: ", baseOracle.getNumberBranching(), verb);
        printStat("Total saturations: ", baseOracle.getNumberSaturations(), verb);
        printStat("Total unsaturations: ", baseOracle.getNumberUnsaturations(), verb);
    }

    private static void printOntologySizes(OWLOntology targetOntology, OWLOntology hypothesisOntology,
                                           Metrics myMetrics, boolean verb, int conceptNumber, int roleNumber) {
        printStat("\nOntology sizes:", verb);
        printStat("Target TBox logical axioms: ", targetOntology.getAxiomCount(AxiomType.SUBCLASS_OF) +
                targetOntology.getAxiomCount(AxiomType.EQUIVALENT_CLASSES), verb);
        myMetrics.computeTargetSizes(targetOntology);
        myMetrics.computeHypothesisSizes(hypothesisOntology);
        printStat("Size of T: ", myMetrics.getSizeOfTarget(), verb);
        printStat("Hypothesis TBox logical axioms: ", hypothesisOntology.getAxiomCount(AxiomType.SUBCLASS_OF) +
                hypothesisOntology.getAxiomCount(AxiomType.EQUIVALENT_CLASSES), verb);
        printStat("Size of H: ", myMetrics.getSizeOfHypothesis(), verb);
        printStat("Number of concept names: ", conceptNumber, verb);
        printStat("Number of role names: ", roleNumber, verb);
        printStat("Size of largest  concept in T: ", myMetrics.getSizeOfTargetLargestConcept(), verb);
        printStat("Size of largest  concept in H: ", myMetrics.getSizeOfHypothesisLargestConcept(), verb);
        printStat("Size of largest  counterexample: ", myMetrics.getSizeOfLargestCounterExample(), verb);
    }
}
