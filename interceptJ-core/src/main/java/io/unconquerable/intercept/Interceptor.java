package io.unconquerable.intercept;

import io.unconquerable.intercept.decide.Decided;
import io.unconquerable.intercept.decide.Decider;
import io.unconquerable.intercept.decide.Decision;
import io.unconquerable.intercept.detect.Detected;
import io.unconquerable.intercept.detect.Detector;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * The central entry point for the Intercept fraud detection pipeline.
 *
 * <p>{@code Interceptor} orchestrates a two-phase, detect-then-decide workflow:
 * <ol>
 *   <li><b>Detection</b> — one or more {@link Detector} instances are registered alongside the
 *       target value they should analyze. Detectors may be wrapped in a
 *       {@link io.unconquerable.intercept.detect.ConditionalDetector} to run only when a runtime
 *       condition is satisfied.</li>
 *   <li><b>Decision</b> — all registered detectors are executed, their
 *       {@link Detected} results are collected, and the resulting list is handed to a
 *       {@link Decider}. The decider produces a {@link Decided} outcome which is wrapped in a
 *       {@link Decision} for fluent result handling.</li>
 * </ol>
 *
 * <p>Usage example:
 * <pre>{@code
 * Optional<Response> response = Interceptor.interceptor()
 *     .detect(request.getIpAddress(), ipReputationDetector)
 *     .detect(request.getDeviceId(), deviceFingerprintDetector)
 *     .decide(fraudDecider)
 *     .onBlock(() -> Response.status(403).build())
 *     .onChallenge(() -> Response.status(429).header("X-Challenge", "captcha").build())
 *     .onProceed(() -> processRequest(request))
 *     .result();
 * }</pre>
 *
 * <p>Instances are not thread-safe and should not be shared across threads. Create a new
 * {@code Interceptor} per request or invocation.
 *
 * @see Detector
 * @see Decider
 * @see Decision
 * @see io.unconquerable.intercept.detect.Detectors
 * @author Rizwan Idrees
 */
public class Interceptor {

    private final List<Detectable<?>> detectables = new ArrayList<>();

    /**
     * Creates a new {@code Interceptor} instance.
     *
     * <p>Prefer this factory method over direct constructor invocation for readability in
     * fluent pipeline chains.
     *
     * @return a fresh, empty {@code Interceptor}
     */
    public static Interceptor interceptor() {
        return new Interceptor();
    }

    /**
     * Registers a target value and its associated {@link Detector} with this interceptor.
     *
     * <p>Multiple detectors may be registered by chaining calls to this method. They are
     * executed in registration order when {@link #decide(Decider)} is called. To make a
     * detector conditional, wrap it with
     * {@link io.unconquerable.intercept.detect.Detectors#detector(Detector)} before passing it
     * here.
     *
     * @param <T>      the type of value being inspected
     * @param t        the target value to analyse; must not be {@code null}
     * @param detector the detector that will analyse {@code t}; must not be {@code null}
     * @return this {@code Interceptor} for fluent chaining
     */
    public <T> Interceptor detect(@Nonnull T t, @Nonnull Detector<T> detector) {
        detectables.add(new Detectable<>(t, detector));
        return this;
    }

    /**
     * Executes all registered detectors and delegates their results to the given {@link Decider}.
     *
     * <p>Each registered {@link Detector} is invoked against its corresponding target value,
     * producing a {@link Detected} result. The full list of results is then passed to
     * {@code decider}, which produces a {@link Decided} outcome. The outcome is wrapped in a
     * {@link Decision} that exposes typed handler methods for each possible outcome.
     *
     * @param <R>     the result type produced by the outcome handlers
     * @param decider the strategy that examines all {@link Detected} results and reaches a
     *                verdict; must not be {@code null}
     * @return a {@link Decision} ready for fluent outcome handling
     */
    public <R> Decision<R> decide(@Nonnull Decider decider) {
        List<Detected> detections = detectables.stream().map(this::detect).toList();
        Decided decided = decider.decide(detections);
        return new Decision<>(decided);
    }

    private <T> Detected detect(Detectable<T> detectable) {
        return detectable.detector.detect(detectable.t);
    }

    private record Detectable<T>(T t, Detector<T> detector) {
    }

}