package io.unconquerable.intercept.detect;

/**
 * Marker interface representing the outcome of a single {@link Detector} analysis.
 *
 * <p>Every {@link Detector} produces a {@code Detected} result that carries the name of the
 * detector that produced it. The full collection of {@code Detected} results from all registered
 * detectors is gathered by {@link io.unconquerable.intercept.Interceptor} and forwarded to a
 * {@link io.unconquerable.intercept.decide.Decider}, which inspects them to reach a
 * {@link io.unconquerable.intercept.decide.Decided} verdict.
 *
 * <p>Two concrete implementations are provided out of the box:
 * <ul>
 *   <li>{@link DetectedScore} — carries a numeric risk score suitable for threshold-based decisions</li>
 *   <li>{@link DetectedStatus} — carries a discrete status ({@code DETECTED}, {@code NOT_DETECTED},
 *       or {@code SKIPPED}) for boolean-style detectors</li>
 * </ul>
 *
 * <p>Custom implementations may be created to carry richer, domain-specific detection metadata.
 *
 * @author Rizwan Idrees
 * @see DetectedScore
 * @see DetectedStatus
 * @see Detector
 */
public interface Detected {

    /**
     * Returns the name of the {@link Detector} that produced this result.
     *
     * <p>This value corresponds to {@link Detector#name()} and is used by
     * {@link io.unconquerable.intercept.decide.Decider} implementations to correlate results with
     * their originating detectors when multiple detectors are registered on the same
     * {@link io.unconquerable.intercept.Interceptor}.
     *
     * @return a non-null, non-empty detector name
     */
    String detectorName();
}
