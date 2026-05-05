package io.unconquerable.intercept.xgboost.normalizer;

public class SigmoidNormalizer implements Normalizer {

    @Override
    public double normalize(float rawScore) {
        return 1.0 / (1.0 + Math.exp(-rawScore));
    }
}