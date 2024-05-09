package org.pac;

public class Pac {

    private Double epsilon;
    private Double delta;
    private Long nTrainingSamples;
    private Integer numberOfExamples;
    private Double hypothesisSpace;
    private Integer hypothesisSize;

    public Pac(Integer numberOfExamples, Double epsilon, Double delta, Integer hypothesisSize) {
        this.epsilon = epsilon;
        this.delta = delta;
        this.hypothesisSize = hypothesisSize;
        this.numberOfExamples = numberOfExamples;
        this.hypothesisSpace = Math.pow(numberOfExamples,hypothesisSize);
        nTrainingSamples = Math.round((hypothesisSize*Math.log(numberOfExamples) - Math.log(delta)) / epsilon);
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(Double epsilon) {
        this.epsilon = epsilon;
        nTrainingSamples = Math.round((hypothesisSize*Math.log(numberOfExamples) - Math.log(delta)) / epsilon);
    }

    public double getDelta() {
        return delta;
    }

    public void setDelta(Double delta) {
        this.delta = delta;
        nTrainingSamples = Math.round((hypothesisSize*Math.log(numberOfExamples) - Math.log(delta)) / epsilon);
    }

    public Double getHypothesisSpace() {
        return hypothesisSpace;
    }

    public void setHypothesisSpace(Double hypothesisSpace) {
        this.hypothesisSpace = hypothesisSpace;
        nTrainingSamples = Math.round((Math.log(hypothesisSpace) - Math.log(delta)) / epsilon);
    }

    public long getTrainingSamples() {
        return nTrainingSamples;
    }

    public int getNumberOfExamples() {
        return numberOfExamples;
    }

    public void setNumberOfExamples(Integer numberOfExamples) {
        this.numberOfExamples = numberOfExamples;
        nTrainingSamples = Math.round((hypothesisSize*Math.log(numberOfExamples) - Math.log(delta)) / epsilon);
    }
}
