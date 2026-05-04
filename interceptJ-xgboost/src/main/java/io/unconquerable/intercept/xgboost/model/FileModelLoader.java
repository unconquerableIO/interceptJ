package io.unconquerable.intercept.xgboost.model;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.io.FileInputStream;
import java.io.IOException;

public record FileModelLoader(ModelSource source) implements ModelLoader {

    @Override
    public Booster load() throws ModelLoaderException {
        try {
            return XGBoost.loadModel(new FileInputStream(source.location()));
        } catch (XGBoostError | IOException e) {
            throw new ModelLoaderException(e);
        }
    }
}
