package io.unconquerable.intercept.xgboost.model;

import jakarta.annotation.Nonnull;

public record ModelSource(@Nonnull String location,
                          @Nonnull String modelId,
                          @Nonnull String version) {
}
