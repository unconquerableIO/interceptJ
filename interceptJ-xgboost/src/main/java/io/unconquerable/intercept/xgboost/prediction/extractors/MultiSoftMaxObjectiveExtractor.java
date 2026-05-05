package io.unconquerable.intercept.xgboost.prediction.extractors;

import io.unconquerable.intercept.xgboost.prediction.ProbabilityPrediction;
import jakarta.annotation.Nonnull;

/**
 * multi:softmax objective
 * @param fraudIndex
 */
public record MultiSoftMaxObjectiveExtractor(int fraudIndex) implements PredictionExtractor<ProbabilityPrediction> {

    @Override
    public ProbabilityPrediction extract(@Nonnull float[][] rawResult) {
        int predictedClassIndex = (int) rawResult[0][0];
        double probability = predictedClassIndex == fraudIndex ? 1.0 : 0.0;
        return new ProbabilityPrediction(probability);
    }

}
