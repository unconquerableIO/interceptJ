package io.unconquerable.intercept.xgboost.prediction;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record PredictionError(@Nonnull String message, @Nullable Throwable cause) implements Error {

    public static PredictionError of(String message) {
        return new PredictionError(message, null);
    }

    public static PredictionError of(Throwable cause) {
        return new PredictionError(cause.getMessage(), cause);
    }
}