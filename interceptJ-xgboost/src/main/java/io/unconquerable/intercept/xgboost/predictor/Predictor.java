package io.unconquerable.intercept.xgboost.predictor;

import io.unconquerable.intercept.xgboost.prediction.RawPrediction;
import ml.dmlc.xgboost4j.java.DMatrix;

import java.util.Optional;
import java.util.function.Function;

public class Predictor<T> {

    private final XGBoostPredictor model;
    private final T type;
    private Function<T, DMatrix> featureExtractor;
    private RawPrediction rawPrediction;

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
        this.rawPrediction = Optional.ofNullable(featureExtractor)
                .map(fe -> fe.apply(type))
                .map(model::predict)
                .orElseGet(() -> null);
        return this;
    }

}
