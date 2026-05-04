package io.unconquerable.intercept.xgboost.model;

public class ModelLoaderException extends RuntimeException {

    public ModelLoaderException(Throwable cause) {
        super(cause);
    }

    public ModelLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

}
