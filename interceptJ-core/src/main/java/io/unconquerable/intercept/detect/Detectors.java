package io.unconquerable.intercept.detect;

import io.unconquerable.intercept.detect.ConditionalDetector.ConditionalDetectorBuilder;

/**
 * Factory class providing the entry point for building conditional detectors.
 *
 * <p>{@code Detectors} is the preferred way to wrap an existing {@link Detector} with a runtime
 * condition before registering it with an {@link io.unconquerable.intercept.Interceptor}. It
 * delegates to {@link ConditionalDetector}'s builder and is designed to be statically imported
 * for a readable, DSL-like usage:
 *
 * <pre>{@code
 * import static io.unconquerable.intercept.detect.Detectors.detector;
 *
 * interceptor()
 *     .detect(request.getIpAddress(),
 *             detector(ipReputationDetector)
 *                 .when(() -> !request.isInternalNetwork())
 *                 .build())
 *     .decide(decider);
 * }</pre>
 *
 * <p>This class is not instantiable.
 *
 * @author Rizwan Idrees
 * @see ConditionalDetector
 * @see Detector
 */
public final class Detectors {

    private Detectors() {
    }

    /**
     * Begins building a {@link ConditionalDetector} that wraps the given {@link Detector}.
     *
     * <p>The returned builder starts with a default condition of {@code true} (always run).
     * Call {@link ConditionalDetector.ConditionalDetectorBuilder#when(java.util.function.BooleanSupplier) when()},
     * {@link ConditionalDetector.ConditionalDetectorBuilder#and(java.util.function.BooleanSupplier) and()},
     * or {@link ConditionalDetector.ConditionalDetectorBuilder#or(java.util.function.BooleanSupplier) or()}
     * to compose the desired runtime condition, then call
     * {@link ConditionalDetector.ConditionalDetectorBuilder#build() build()} to obtain the
     * configured detector.
     *
     * @param <T>      the type of the target value the detector analyses
     * @param detector the detector to wrap; must not be {@code null}
     * @return a fluent builder for attaching runtime conditions to the given detector
     */
    public static <T> ConditionalDetectorBuilder<T> detector(Detector<T> detector) {
        return ConditionalDetector.detector(detector);
    }

}
