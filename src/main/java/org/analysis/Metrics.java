package org.analysis;

import java.util.Arrays;

public class Metrics {
    public static double calculateAccuracy(int[][] confusionMatrix) {
        int total = Arrays.stream(confusionMatrix).flatMapToInt(Arrays::stream).sum();
        int correct = 0;
        for (int i = 0; i < confusionMatrix.length; i++) {
            correct += confusionMatrix[i][i];
        }
        return (double) correct / total;
    }

    public static double calculatePrecision(int[][] confusionMatrix) {
        int tp = confusionMatrix[0][0];
        int fp = confusionMatrix[1][0];
        return (double) tp / (tp + fp);
    }

    public static double calculateRecall(int[][] confusionMatrix) {
        int tp = confusionMatrix[0][0];
        int fn = confusionMatrix[0][1] + confusionMatrix[0][2];
        return (double) tp / (tp + fn);
    }

    public static double calculateF1Score(int[][] confusionMatrix) {
        double precision = calculatePrecision(confusionMatrix);
        double recall = calculateRecall(confusionMatrix);
        if (precision + recall == 0) {
            return 0.0; // Avoid division by zero
        } else {
            return 2.0 * (precision * recall) / (precision + recall);
        }
    }

    public static double calculateLogLoss(int[][] confusionMatrix) {
        double sum = 0;
        int total = Arrays.stream(confusionMatrix).flatMapToInt(Arrays::stream).sum();
        for (int[] matrix : confusionMatrix) {
            for (int i : matrix) {
                double prob = (double) i / total;
                sum += i == 0 ? 0 : i * Math.log(prob);
            }
        }
        return -sum / total;
    }

    public static double calculateMatthewsCorrelationCoefficient(int[][] confusionMatrix) {
        int tp = confusionMatrix[0][0];
        int tn = confusionMatrix[1][1] + confusionMatrix[1][2];
        int fp = confusionMatrix[1][0];
        int fn = confusionMatrix[0][1] + confusionMatrix[0][2];
        //      T   F   U
        //  T   TP  FN  FN
        //  F   FP  TN  TN

        double numerator = (tp * tn) - (fp * fn);
        double denominator = Math.sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn));

        if (denominator == 0) {
            return 0.0; // Avoid division by zero
        } else {
            return numerator / denominator;
        }
    }
}
