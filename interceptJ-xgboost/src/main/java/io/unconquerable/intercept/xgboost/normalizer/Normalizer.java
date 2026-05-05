package io.unconquerable.intercept.xgboost.normalizer;

@FunctionalInterface
public interface Normalizer {

    double normalize(float rawScore);

}
