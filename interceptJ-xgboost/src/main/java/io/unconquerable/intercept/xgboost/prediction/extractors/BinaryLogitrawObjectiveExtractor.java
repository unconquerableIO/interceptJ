package io.unconquerable.intercept.xgboost.prediction.extractors;

import io.unconquerable.intercept.xgboost.normalizer.Normalizer;
import io.unconquerable.intercept.xgboost.prediction.ProbabilityPrediction;
import jakarta.annotation.Nonnull;


/**
 * Objective binary:logitraw
 */
public record BinaryLogitrawObjectiveExtractor(Normalizer normalizer) implements PredictionExtractor<ProbabilityPrediction> {

    @Override
    public ProbabilityPrediction extract(@Nonnull float[][] rawResult) {
        return new ProbabilityPrediction(normalizer.normalize(rawResult[0][0]));
    }
}
