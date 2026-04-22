package io.unconquerable.intercept;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * ConditionalDetector
 *
 * @param detector
 * @param condition
 * @param <T>
 * @author Rizwan Idrees
 */
public record ConditionalDetector<T>(Detector<T> detector, BooleanSupplier condition) {

    public static <T> ConditionalDetectorBuilder<T> detector(Detector<T> detector) {
        return new ConditionalDetectorBuilder<T>(detector);
    }

    public static class ConditionalDetectorBuilder<T> {

        private final Detector<T> detector;
        private BooleanSupplier composed = () -> true;

        ConditionalDetectorBuilder(Detector<T> detector) {
            this.detector = Objects.requireNonNull(detector);
        }

        public ConditionalDetectorBuilder<T> when(BooleanSupplier condition) {
            this.composed = condition;
            return this;
        }

        public ConditionalDetectorBuilder<T> and(BooleanSupplier next) {
            BooleanSupplier prev = this.composed;
            this.composed = () -> prev.getAsBoolean() && next.getAsBoolean();
            return this;
        }

        public ConditionalDetectorBuilder<T> or(BooleanSupplier next) {
            BooleanSupplier prev = this.composed;
            this.composed = () -> prev.getAsBoolean() || next.getAsBoolean();
            return this;
        }

        public ConditionalDetector<T> build() {
            return new ConditionalDetector<>(detector, composed);
        }
    }
}
