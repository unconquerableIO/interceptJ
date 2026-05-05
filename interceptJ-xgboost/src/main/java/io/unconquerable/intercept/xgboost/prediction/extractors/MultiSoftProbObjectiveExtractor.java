package io.unconquerable.intercept.xgboost.prediction.extractors;

import io.unconquerable.intercept.xgboost.prediction.ProbabilityPrediction;
import jakarta.annotation.Nonnull;

/**
 * multi:softprob objective
 * @param fraudIndex
 */
public record MultiSoftProbObjectiveExtractor(int fraudIndex) implements PredictionExtractor<ProbabilityPrediction> {

    @Override
    public ProbabilityPrediction extract(@Nonnull float[][] rawResult) {
        return new ProbabilityPrediction((double) rawResult[0][fraudIndex]);
    }
}
