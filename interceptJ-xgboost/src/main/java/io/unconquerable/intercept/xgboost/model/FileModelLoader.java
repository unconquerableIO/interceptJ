package io.unconquerable.intercept.xgboost.model;

import jakarta.annotation.Nonnull;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.io.FileInputStream;
import java.io.IOException;

public record FileModelLoader(@Nonnull ModelSource source) implements ModelLoader {

    @Override
    public Booster load() throws ModelLoaderException {
        try {
            return XGBoost.loadModel(new FileInputStream(source.location()));
        } catch (XGBoostError | IOException e) {
            throw new ModelLoaderException(e);
        }
    }
}
