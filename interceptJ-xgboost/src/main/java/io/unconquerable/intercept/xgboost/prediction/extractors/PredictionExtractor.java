package io.unconquerable.intercept.xgboost.prediction.extractors;

import io.unconquerable.intercept.xgboost.prediction.Prediction;

public interface PredictionExtractor<P extends Prediction<?>> {

    P extract(float[][] rawResult);

}
