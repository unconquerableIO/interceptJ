package io.unconquerable.intercept.detect;

import jakarta.annotation.Nonnull;

import java.math.BigDecimal;

/**
 * A {@link Detected} result that expresses a detector's finding as a continuous numeric risk score.
 *
 * <p>{@code DetectedScore} is appropriate when a {@link Detector} produces a quantitative signal —
 * for example, a machine-learning model confidence, a velocity count, or a composite risk index.
 * A {@link io.unconquerable.intercept.decide.Decider} can compare the score against thresholds to
 * determine whether to block, challenge, defer, or proceed.
 *
 * <p>Example usage inside a custom {@link Detector}:
 * <pre>{@code
 * public Detected detect(String ipAddress) {
 *     BigDecimal riskScore = reputationService.scoreOf(ipAddress);
 *     return new DetectedScore(name(), riskScore);
 * }
 * }</pre>
 *
 * <p>Example usage inside a custom {@link io.unconquerable.intercept.decide.Decider}:
 * <pre>{@code
 * public Decided decide(List<Detected> detections) {
 *     return detections.stream()
 *         .filter(d -> d instanceof DetectedScore)
 *         .map(d -> (DetectedScore) d)
 *         .filter(d -> d.detectorName().equals("ip-reputation"))
 *         .findFirst()
 *         .map(d -> d.score().compareTo(RISK_THRESHOLD) > 0
 *                 ? Decided.decidedToBlock()
 *                 : Decided.decidedToProceed())
 *         .orElse(Decided.decidedToProceed());
 * }
 * }</pre>
 *
 * @param detectorName the name of the {@link Detector} that produced this result; must not be
 *                     {@code null}
 * @param score        the numeric risk score; higher values conventionally indicate higher risk,
 *                     but the scale is defined by the producing detector; must not be {@code null}
 * @author Rizwan Idrees
 * @see DetectedStatus
 * @see Detected
 */
public record DetectedScore(@Nonnull String detectorName, @Nonnull BigDecimal score) implements Detected {
}
