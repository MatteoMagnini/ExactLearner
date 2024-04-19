package org.pac;

public class Pac {

    private Double epsilon;
    private Double gamma;
    private Long nTrainingSamples;
    private Integer hypothesisCardinality;

    public Pac(Integer hypothesisCardinality, Double epsilon, Double gamma) {
        this.epsilon = epsilon;
        this.gamma = gamma;
        this.hypothesisCardinality = hypothesisCardinality;
        nTrainingSamples = Math.round(Math.log(hypothesisCardinality / gamma) / epsilon);
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(Double epsilon) {
        this.epsilon = epsilon;
        nTrainingSamples = Math.round(Math.log(hypothesisCardinality / gamma) / epsilon);
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(Double gamma) {
        this.gamma = gamma;
        nTrainingSamples = Math.round(Math.log(hypothesisCardinality / gamma) / epsilon);
    }

    public long getTrainingSamples() {
        return nTrainingSamples;
    }

    public int getHypothesisCardinality() {
        return hypothesisCardinality;
    }

    public void setHypothesisCardinality(Integer hypothesisCardinality) {
        this.hypothesisCardinality = hypothesisCardinality;
        nTrainingSamples = Math.round(Math.log(hypothesisCardinality / gamma) / epsilon);
    }
}
