package io.unconquerable.intercept.xgboost.predictor;

public enum ObjectiveFunction {
    LOGISTIC,
    LOGIT_RAW,
    SOFT_PROB,
    SOFTMAX,
    SQUARED_ERROR,
    ABSOLUTE_ERROR,
    NDCG,
    MAP,
    PAIRWISE
}
