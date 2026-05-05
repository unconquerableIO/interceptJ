package io.unconquerable.intercept.xgboost.predictor;

import io.unconquerable.intercept.functional.Either;
import io.unconquerable.intercept.xgboost.prediction.Error;
import io.unconquerable.intercept.xgboost.prediction.Prediction;
import io.unconquerable.intercept.xgboost.prediction.PredictionError;
import ml.dmlc.xgboost4j.java.DMatrix;

import java.util.Optional;
import java.util.function.Function;

public class Predictor<T> {

    private static final PredictionError NO_FEATURES_ERROR = PredictionError.of("No feature extractor is defined");

    private final XGBoostPredictor model;
    private final T type;
    private Function<T, DMatrix> featureExtractor;
    private Either<? extends Prediction<?>, Error> result;

    private Predictor(XGBoostPredictor model, T type) {
        this.model = model;
        this.type = type;
    }

    public static <T> Predictor<T> predictor(XGBoostPredictor model, T type) {
        return new Predictor<>(model, type);
    }

    public Predictor<T> extractFeatures(Function<T, DMatrix> featureExtractor) {
        this.featureExtractor = featureExtractor;
        return this;
    }

    public Predictor<T> predict() {
        this.result = Optional.ofNullable(featureExtractor)
                .map(fe -> fe.apply(type))
                .map(model::predict)
                .orElseGet(() -> Either.right(NO_FEATURES_ERROR));
        return this;
    }




}
