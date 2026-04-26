package io.unconquerable.intercept.detect;

import jakarta.annotation.Nonnull;

/**
 * A {@link Detected} result that expresses a detector's finding as a discrete status value.
 *
 * <p>{@code DetectedStatus} is appropriate when a {@link Detector} produces a binary or
 * ternary signal — for example, whether a device fingerprint is on a blocklist, whether a
 * transaction matches a known fraud pattern, or whether the detector was conditionally skipped.
 *
 * <p>The {@link Status#SKIPPED} value is set automatically by
 * {@link ConditionalDetector} when its runtime condition evaluates to {@code false},
 * so that the detector's slot in the result list is preserved without producing a false signal.
 *
 * <p>Example usage inside a custom {@link Detector}:
 * <pre>{@code
 * public Detected detect(String deviceId) {
 *     boolean onBlocklist = blocklistService.contains(deviceId);
 *     return new DetectedStatus(name(),
 *             onBlocklist ? Status.DETECTED : Status.NOT_DETECTED);
 * }
 * }</pre>
 *
 * @param detectorName the name of the {@link Detector} that produced this result; must not be
 *                     {@code null}
 * @param status       the discrete outcome of the detection; must not be {@code null}
 * @author Rizwan Idrees
 * @see Status
 * @see DetectedScore
 * @see ConditionalDetector
 */
public record DetectedStatus(@Nonnull String detectorName, @Nonnull Status status) implements Detected {

    /**
     * The set of discrete outcomes a status-based detector can report.
     */
    public enum Status {

        /**
         * The detector found a fraud signal for the analysed target — for example, the IP address
         * is on a known bad-actor list, or the transaction matches a fraud pattern.
         */
        DETECTED,

        /**
         * The detector found no fraud signal for the analysed target.
         */
        NOT_DETECTED,

        /**
         * The detector was not executed because its runtime condition (as defined by a
         * {@link ConditionalDetector}) evaluated to {@code false}. The result should be
         * treated as absent rather than negative by {@link io.unconquerable.intercept.decide.Decider}
         * implementations.
         */
        SKIPPED
    }
}
