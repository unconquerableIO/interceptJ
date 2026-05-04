package io.unconquerable.intercept.xgboost.predictor;

import io.unconquerable.intercept.xgboost.model.ModelLoader;
import io.unconquerable.intercept.xgboost.prediction.RawPrediction;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;

public class XGBoostPredictor {

    private final Booster booster;

    public XGBoostPredictor(ModelLoader loader) {
        this.booster = loader.load();
    }

    public RawPrediction predict(DMatrix matrix)  {
        try {
            return new RawPrediction(booster.predict(matrix));
        } catch (XGBoostError e) {
            throw new RuntimeException(e);
        }
    }

}
