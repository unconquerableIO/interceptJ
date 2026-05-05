package io.unconquerable.intercept.xgboost.prediction.extractors;

import io.unconquerable.intercept.xgboost.prediction.ProbabilityPrediction;
import jakarta.annotation.Nonnull;

/**
 * Objective binary:logistic
 */
public record BinaryLogisticObjectiveExtractor() implements PredictionExtractor<ProbabilityPrediction> {

    @Override
    public ProbabilityPrediction extract(@Nonnull float[][] rawResult) {
        return new ProbabilityPrediction((double) rawResult[0][0]);
    }
}
