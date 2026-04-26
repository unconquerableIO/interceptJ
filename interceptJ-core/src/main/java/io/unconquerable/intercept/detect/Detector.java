package io.unconquerable.intercept.detect;

/**
 * Strategy interface for inspecting a target value and producing a {@link Detected} result.
 *
 * <p>A {@code Detector} encapsulates a single fraud-signal analysis — for example, IP reputation
 * lookup, device fingerprint matching, velocity checks, or behavioural scoring. Implementations
 * are registered with an {@link io.unconquerable.intercept.Interceptor} alongside the value they
 * should analyse:
 *
 * <pre>{@code
 * Detector<String> ipReputationDetector = new IpReputationDetector();
 *
 * interceptor()
 *     .detect(request.getIpAddress(), ipReputationDetector)
 *     .decide(decider);
 * }</pre>
 *
 * <p>Implementations must be stateless or thread-safe, as the same instance may be reused across
 * multiple requests.
 *
 * <p>To run a detector conditionally, wrap it using
 * {@link Detectors#detector(Detector)}:
 * <pre>{@code
 * Detectors.detector(ipReputation)
 *          .when(() -> featureFlags.isIpCheckEnabled())
 *          .build();
 * }</pre>
 *
 * @param <T> the type of the target value this detector analyses
 * @author Rizwan Idrees
 * @see Detected
 * @see DetectedScore
 * @see DetectedStatus
 * @see ConditionalDetector
 * @see Detectors
 */
public interface Detector<T> {

    /**
     * Returns the unique name that identifies this detector.
     *
     * <p>The name is embedded in every {@link Detected} result produced by this detector,
     * allowing {@link io.unconquerable.intercept.decide.Decider} implementations to distinguish
     * results from different detectors when multiple are registered on the same
     * {@link io.unconquerable.intercept.Interceptor}.
     *
     * @return a non-null, non-empty detector name
     */
    String name();

    /**
     * Analyses the given target value and returns a {@link Detected} result.
     *
     * <p>Implementations should return one of the built-in result types:
     * <ul>
     *   <li>{@link DetectedScore} — when the analysis yields a continuous risk score</li>
     *   <li>{@link DetectedStatus} — when the analysis produces a discrete status
     *       ({@code DETECTED}, {@code NOT_DETECTED})</li>
     * </ul>
     * Custom {@link Detected} implementations are also permitted.
     *
     * @param target the value to analyse; will not be {@code null} when invoked through
     *               {@link io.unconquerable.intercept.Interceptor}
     * @return a non-null {@link Detected} result describing the outcome of the analysis
     */
    Detected detect(T target);

}
