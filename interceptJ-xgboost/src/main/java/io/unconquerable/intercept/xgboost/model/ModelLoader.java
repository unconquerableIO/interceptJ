package io.unconquerable.intercept.xgboost.model;

import ml.dmlc.xgboost4j.java.Booster;

public interface ModelLoader {

    ModelSource source();
    Booster load() throws ModelLoaderException;
}
