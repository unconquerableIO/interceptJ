package io.unconquerable.intercept.decide;

import io.unconquerable.intercept.detect.Detected;

import java.util.List;

/**
 * Strategy interface that examines a set of {@link Detected} results and produces a
 * {@link Decided} verdict.
 *
 * <p>A {@code Decider} is the policy layer of the fraud pipeline. It receives the aggregated
 * output of all {@link io.unconquerable.intercept.detect.Detector} instances registered on an
 * {@link io.unconquerable.intercept.Interceptor}, and is responsible for translating those
 * signals into an actionable outcome: {@code BLOCK}, {@code PROCEED}, {@code CHALLENGE},
 * or {@code DEFER}.
 *
 * <p>Because {@code Decider} is a {@link FunctionalInterface} it can be implemented as a lambda
 * for simple rules:
 * <pre>{@code
 * Decider blockAll = detections -> Decided.decidedToBlock();
 * }</pre>
 *
 * <p>For score-based decisions:
 * <pre>{@code
 * Decider scoreDecider = detections -> {
 *     BigDecimal totalRisk = detections.stream()
 *         .filter(d -> d instanceof DetectedScore)
 *         .map(d -> ((DetectedScore) d).score())
 *         .reduce(BigDecimal.ZERO, BigDecimal::add);
 *
 *     if (totalRisk.compareTo(HIGH_RISK) > 0)  return Decided.decidedToBlock();
 *     if (totalRisk.compareTo(MED_RISK)  > 0)  return Decided.decidedToChallenge();
 *     return Decided.decidedToProceed();
 * };
 * }</pre>
 *
 * <p>Implementations must be stateless or thread-safe, as the same instance is typically
 * reused across multiple requests.
 *
 * @author Rizwan Idrees
 * @see Decided
 * @see DecisionDetail
 * @see io.unconquerable.intercept.Interceptor
 */
@FunctionalInterface
public interface Decider {

    /**
     * Evaluates the provided detection results and returns a verdict.
     *
     * <p>Implementations should handle the case where {@code detections} contains results with
     * {@link io.unconquerable.intercept.detect.DetectedStatus.Status#SKIPPED}, which indicate
     * that a conditional detector was bypassed and should not be treated as a negative signal.
     *
     * @param detections the complete list of {@link Detected} results produced by all registered
     *                   detectors; never {@code null}, but may be empty if no detectors were
     *                   registered
     * @return a non-null {@link Decided} verdict indicating the action to take
     */
    Decided decide(List<Detected> detections);

}
