package io.unconquerable.intercept.xgboost.prediction;

public record ProbabilityPrediction(Double probability) implements Prediction<Double> {

    @Override
    public Double get() {
        return probability;
    }
}
