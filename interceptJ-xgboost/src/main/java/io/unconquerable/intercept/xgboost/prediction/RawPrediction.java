package io.unconquerable.intercept.xgboost.prediction;

public record RawPrediction(float[][] prediction) implements Prediction<float[][]> {
}
