package io.unconquerable.intercept.xgboost.prediction;

public sealed interface Error permits PredictionError {

    String message();
}