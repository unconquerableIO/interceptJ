package io.unconquerable.intercept;

import io.unconquerable.intercept.decide.Decided;
import io.unconquerable.intercept.detect.DetectedScore;
import io.unconquerable.intercept.detect.DetectedStatus;
import io.unconquerable.intercept.detect.Detector;
import io.unconquerable.intercept.detect.Detectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.unconquerable.intercept.Interceptor.interceptor;
import static io.unconquerable.intercept.decide.Decided.*;
import static org.junit.jupiter.api.Assertions.*;

class InterceptorTest {

    // --- fixture detectors ---------------------------------------------------

    /** Always returns DETECTED for any input. */
    private static final Detector<String> ALWAYS_DETECTED =
            new NamedStatusDetector("always-detected", DetectedStatus.Status.DETECTED);

    /** Always returns NOT_DETECTED for any input. */
    private static final Detector<String> ALWAYS_CLEAN =
            new NamedStatusDetector("always-clean", DetectedStatus.Status.NOT_DETECTED);

    /** Returns a fixed score of 0.9 (high risk). */
    private static final Detector<String> HIGH_RISK_SCORER =
            new NamedScoreDetector("high-risk", new BigDecimal("0.9"));

    // --- deciders ------------------------------------------------------------

    /** Blocks if any detection is DETECTED; otherwise proceeds. */
    private static final io.unconquerable.intercept.decide.Decider BLOCK_ON_DETECTED =
            detections -> detections.stream()
                    .filter(d -> d instanceof DetectedStatus ds
                            && ds.status() == DetectedStatus.Status.DETECTED)
                    .findFirst()
                    .<Decided>map(_ -> decidedToBlock())
                    .orElse(decidedToProceed());

    /** Blocks if total score > 0.5, challenges if > 0.3, otherwise proceeds. */
    private static final io.unconquerable.intercept.decide.Decider SCORE_DECIDER =
            detections -> {
                BigDecimal total = detections.stream()
                        .filter(d -> d instanceof DetectedScore)
                        .map(d -> ((DetectedScore) d).score())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (total.compareTo(new BigDecimal("0.5")) > 0) return decidedToBlock();
                if (total.compareTo(new BigDecimal("0.3")) > 0) return decidedToChallenge();
                return decidedToProceed();
            };

    // =========================================================================

    @Nested
    class Outcomes {

        @Test
        void block_handler_is_invoked_when_decider_returns_block() {
            Optional<String> result = interceptor()
                    .detect("192.168.1.1", ALWAYS_DETECTED)
                    .<String>decide(BLOCK_ON_DETECTED)
                    .onBlock(() -> "blocked")
                    .onProceed(() -> "proceed")
                    .result();

            assertEquals(Optional.of("blocked"), result);
        }

        @Test
        void proceed_handler_is_invoked_when_decider_returns_proceed() {
            Optional<String> result = interceptor()
                    .detect("192.168.1.1", ALWAYS_CLEAN)
                    .<String>decide(BLOCK_ON_DETECTED)
                    .onBlock(() -> "blocked")
                    .onProceed(() -> "proceed")
                    .result();

            assertEquals(Optional.of("proceed"), result);
        }

        @Test
        void challenge_handler_is_invoked_when_decider_returns_challenge() {
            Optional<String> result = interceptor()
                    .detect("192.168.1.1", new NamedScoreDetector("scorer", new BigDecimal("0.4")))
                    .<String>decide(SCORE_DECIDER)
                    .onBlock(() -> "blocked")
                    .onChallenge(() -> "challenge")
                    .onProceed(() -> "proceed")
                    .result();

            assertEquals(Optional.of("challenge"), result);
        }

        @Test
        void defer_handler_is_invoked_when_decider_returns_defer() {
            Optional<String> result = interceptor()
                    .detect("device-xyz", ALWAYS_CLEAN)
                    .<String>decide(_ -> decidedToDefer())
                    .onBlock(() -> "blocked")
                    .onDefer(() -> "deferred")
                    .onProceed(() -> "proceed")
                    .result();

            assertEquals(Optional.of("deferred"), result);
        }

        @Test
        void result_is_empty_when_no_handler_registered_for_active_verdict() {
            Optional<String> result = interceptor()
                    .detect("192.168.1.1", ALWAYS_DETECTED)
                    .<String>decide(BLOCK_ON_DETECTED)
                    // onBlock intentionally omitted
                    .onProceed(() -> "proceed")
                    .result();

            assertEquals(Optional.empty(), result);
        }
    }

    // =========================================================================

    @Nested
    class MultipleDetectors {

        @Test
        void all_registered_detectors_are_executed() {
            int[] callCount = {0};

            Detector<String> counting = new Detector<>() {
                @Override public String name() { return "counter"; }
                @Override public DetectedStatus detect(String target) {
                    callCount[0]++;
                    return new DetectedStatus(name(), DetectedStatus.Status.NOT_DETECTED);
                }
            };

            interceptor()
                    .detect("input", counting)
                    .detect("input", counting)
                    .detect("input", counting)
                    .<Void>decide(_ -> decidedToProceed())
                    .result();

            assertEquals(3, callCount[0]);
        }

        @Test
        void decider_receives_results_from_all_detectors() {
            interceptor()
                    .detect("ip", ALWAYS_DETECTED)
                    .detect("device", HIGH_RISK_SCORER)
                    .<Void>decide(detections -> {
                        assertEquals(2, detections.size());
                        assertInstanceOf(DetectedStatus.class, detections.get(0));
                        assertInstanceOf(DetectedScore.class, detections.get(1));
                        return decidedToProceed();
                    })
                    .result();
        }

        @Test
        void detectors_are_executed_in_registration_order() {
            var order = new ArrayList<String>();

            Detector<String> first  = new NamedOrderingDetector("first",  order);
            Detector<String> second = new NamedOrderingDetector("second", order);
            Detector<String> third  = new NamedOrderingDetector("third",  order);

            interceptor()
                    .detect("x", first)
                    .detect("x", second)
                    .detect("x", third)
                    .<Void>decide(_ -> decidedToProceed())
                    .result();

            assertEquals(List.of("first", "second", "third"), order);
        }

        @Test
        void score_based_decider_aggregates_scores_from_multiple_detectors() {
            // 0.4 + 0.4 = 0.8 -> should block
            Optional<String> result = interceptor()
                    .detect("tx1", new NamedScoreDetector("s1", new BigDecimal("0.4")))
                    .detect("tx2", new NamedScoreDetector("s2", new BigDecimal("0.4")))
                    .<String>decide(SCORE_DECIDER)
                    .onBlock(() -> "blocked")
                    .onProceed(() -> "proceed")
                    .result();

            assertEquals(Optional.of("blocked"), result);
        }
    }

    // =========================================================================

    @Nested
    class ConditionalDetectors {

        @Test
        void conditional_detector_runs_when_condition_is_true() {
            Optional<String> result = interceptor()
                    .detect("ip", Detectors.detector(ALWAYS_DETECTED)
                            .when(() -> true)
                            .build())
                    .<String>decide(BLOCK_ON_DETECTED)
                    .onBlock(() -> "blocked")
                    .onProceed(() -> "proceed")
                    .result();

            assertEquals(Optional.of("blocked"), result);
        }

        @Test
        void conditional_detector_is_skipped_when_condition_is_false() {
            Optional<String> result = interceptor()
                    .detect("ip", Detectors.detector(ALWAYS_DETECTED)
                            .when(() -> false)
                            .build())
                    .<String>decide(BLOCK_ON_DETECTED)
                    .onBlock(() -> "blocked")
                    .onProceed(() -> "proceed")
                    .result();

            // ALWAYS_DETECTED was skipped, so decider sees SKIPPED -> proceeds
            assertEquals(Optional.of("proceed"), result);
        }

        @Test
        void skipped_detector_produces_skipped_status_in_detection_results() {
            interceptor()
                    .detect("ip", Detectors.detector(ALWAYS_DETECTED)
                            .when(() -> false)
                            .build())
                    .<Void>decide(detections -> {
                        assertEquals(1, detections.size());
                        assertInstanceOf(DetectedStatus.class, detections.getFirst());
                        assertEquals(DetectedStatus.Status.SKIPPED,
                                ((DetectedStatus) detections.getFirst()).status());
                        return decidedToProceed();
                    })
                    .result();
        }

        @Test
        void and_condition_requires_both_conditions_to_be_true() {
            Optional<String> blocked = interceptor()
                    .detect("ip", Detectors.detector(ALWAYS_DETECTED)
                            .when(() -> true).and(() -> true)
                            .build())
                    .<String>decide(BLOCK_ON_DETECTED)
                    .onBlock(() -> "blocked")
                    .onProceed(() -> "proceed")
                    .result();
            assertEquals(Optional.of("blocked"), blocked);

            Optional<String> skipped = interceptor()
                    .detect("ip", Detectors.detector(ALWAYS_DETECTED)
                            .when(() -> true).and(() -> false)
                            .build())
                    .<String>decide(BLOCK_ON_DETECTED)
                    .onBlock(() -> "blocked")
                    .onProceed(() -> "proceed")
                    .result();
            assertEquals(Optional.of("proceed"), skipped);
        }

        @Test
        void or_condition_runs_when_either_condition_is_true() {
            Optional<String> result = interceptor()
                    .detect("ip", Detectors.detector(ALWAYS_DETECTED)
                            .when(() -> false).or(() -> true)
                            .build())
                    .<String>decide(BLOCK_ON_DETECTED)
                    .onBlock(() -> "blocked")
                    .onProceed(() -> "proceed")
                    .result();

            assertEquals(Optional.of("blocked"), result);
        }

        @Test
        void skipped_detector_preserves_detector_name_in_result() {
            interceptor()
                    .detect("ip", Detectors.detector(ALWAYS_DETECTED)
                            .when(() -> false)
                            .build())
                    .<Void>decide(detections -> {
                        assertEquals("always-detected", detections.getFirst().detectorName());
                        return decidedToProceed();
                    })
                    .result();
        }
    }

    // =========================================================================

    @Nested
    class DecisionDetails {

        record FraudDetail(String reason) implements io.unconquerable.intercept.decide.DecisionDetail {}

        @Test
        void decided_carries_detail_when_provided() {
            var detail = new FraudDetail("VELOCITY_EXCEEDED");
            Decided decided = decidedToBlock(detail);

            assertTrue(decided.details().isPresent());
            assertEquals("VELOCITY_EXCEEDED", ((FraudDetail) decided.details().get()).reason());
        }

        @Test
        void decided_details_is_empty_when_no_detail_provided() {
            assertEquals(Optional.empty(), decidedToProceed().details());
            assertEquals(Optional.empty(), decidedToBlock().details());
            assertEquals(Optional.empty(), decidedToChallenge().details());
            assertEquals(Optional.empty(), decidedToDefer().details());
        }
    }

    // =========================================================================

    @Nested
    class DecidedTypeChecks {

        @Test
        void toBlock_is_true_only_for_block_verdict() {
            assertTrue(decidedToBlock().toBlock());
            assertFalse(decidedToBlock().toProceed());
            assertFalse(decidedToBlock().toChallenge());
            assertFalse(decidedToBlock().toDefer());
        }

        @Test
        void toProceed_is_true_only_for_proceed_verdict() {
            assertTrue(decidedToProceed().toProceed());
            assertFalse(decidedToProceed().toBlock());
            assertFalse(decidedToProceed().toChallenge());
            assertFalse(decidedToProceed().toDefer());
        }

        @Test
        void toChallenge_is_true_only_for_challenge_verdict() {
            assertTrue(decidedToChallenge().toChallenge());
            assertFalse(decidedToChallenge().toBlock());
            assertFalse(decidedToChallenge().toProceed());
            assertFalse(decidedToChallenge().toDefer());
        }

        @Test
        void toDefer_is_true_only_for_defer_verdict() {
            assertTrue(decidedToDefer().toDefer());
            assertFalse(decidedToDefer().toBlock());
            assertFalse(decidedToDefer().toProceed());
            assertFalse(decidedToDefer().toChallenge());
        }
    }

    // =========================================================================
    // Fixture helpers
    // =========================================================================

    private record NamedStatusDetector(String name, DetectedStatus.Status status)
            implements Detector<String> {

        @Override
        public DetectedStatus detect(String target) {
            return new DetectedStatus(name, status);
        }
    }

    private record NamedScoreDetector(String name, BigDecimal score)
            implements Detector<String> {

        @Override
        public DetectedScore detect(String target) {
            return new DetectedScore(name, score);
        }
    }

    private record NamedOrderingDetector(String name, List<String> order)
            implements Detector<String> {

        @Override
        public DetectedStatus detect(String target) {
            order.add(name);
            return new DetectedStatus(name, DetectedStatus.Status.NOT_DETECTED);
        }
    }
}