package io.unconquerable.intercept.decide;

import io.unconquerable.intercept.detect.DetectedStatus;
import io.unconquerable.intercept.instrument.InstrumentIdentifier;
import io.unconquerable.intercept.instrument.InstrumentType;
import io.unconquerable.intercept.send.Sender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.unconquerable.intercept.decide.Decided.*;
import static io.unconquerable.intercept.detect.DetectedStatus.Status.NOT_DETECTED;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
class DecisionTest {

    // =========================================================================

    @Nested
    class MatchingHandler {

        @Test
        void onBlock_supplier_is_invoked_for_block_verdict() {
            var result = new Decision<String>(decidedToBlock())
                    .onBlock(() -> "blocked")
                    .result();

            assertEquals(Optional.of("blocked"), result);
        }

        @Test
        void onProceed_supplier_is_invoked_for_proceed_verdict() {
            var result = new Decision<String>(decidedToProceed())
                    .onProceed(() -> "proceed")
                    .result();

            assertEquals(Optional.of("proceed"), result);
        }

        @Test
        void onChallenge_supplier_is_invoked_for_challenge_verdict() {
            var result = new Decision<String>(decidedToChallenge())
                    .onChallenge(() -> "challenge")
                    .result();

            assertEquals(Optional.of("challenge"), result);
        }

        @Test
        void onDefer_supplier_is_invoked_for_defer_verdict() {
            var result = new Decision<String>(decidedToDefer())
                    .onDefer(() -> "deferred")
                    .result();

            assertEquals(Optional.of("deferred"), result);
        }
    }

    // =========================================================================

    @Nested
    class NonMatchingHandlers {

        @Test
        void non_matching_suppliers_are_not_invoked_on_block_verdict() {
            boolean[] called = {false, false, false};

            new Decision<String>(decidedToBlock())
                    .onBlock(() -> "blocked")
                    .onProceed(() -> { called[0] = true; return "proceed"; })
                    .onChallenge(() -> { called[1] = true; return "challenge"; })
                    .onDefer(() -> { called[2] = true; return "deferred"; })
                    .result();

            assertFalse(called[0], "onProceed should not be called");
            assertFalse(called[1], "onChallenge should not be called");
            assertFalse(called[2], "onDefer should not be called");
        }

        @Test
        void non_matching_suppliers_are_not_invoked_on_proceed_verdict() {
            boolean[] called = {false, false, false};

            new Decision<String>(decidedToProceed())
                    .onBlock(() -> { called[0] = true; return "blocked"; })
                    .onProceed(() -> "proceed")
                    .onChallenge(() -> { called[1] = true; return "challenge"; })
                    .onDefer(() -> { called[2] = true; return "deferred"; })
                    .result();

            assertFalse(called[0], "onBlock should not be called");
            assertFalse(called[1], "onChallenge should not be called");
            assertFalse(called[2], "onDefer should not be called");
        }

        @Test
        void non_matching_suppliers_are_not_invoked_on_challenge_verdict() {
            boolean[] called = {false, false, false};

            new Decision<String>(decidedToChallenge())
                    .onBlock(() -> { called[0] = true; return "blocked"; })
                    .onChallenge(() -> "challenge")
                    .onProceed(() -> { called[1] = true; return "proceed"; })
                    .onDefer(() -> { called[2] = true; return "deferred"; })
                    .result();

            assertFalse(called[0], "onBlock should not be called");
            assertFalse(called[1], "onProceed should not be called");
            assertFalse(called[2], "onDefer should not be called");
        }

        @Test
        void non_matching_suppliers_are_not_invoked_on_defer_verdict() {
            boolean[] called = {false, false, false};

            new Decision<String>(decidedToDefer())
                    .onBlock(() -> { called[0] = true; return "blocked"; })
                    .onProceed(() -> { called[1] = true; return "proceed"; })
                    .onChallenge(() -> { called[2] = true; return "challenge"; })
                    .onDefer(() -> "deferred")
                    .result();

            assertFalse(called[0], "onBlock should not be called");
            assertFalse(called[1], "onProceed should not be called");
            assertFalse(called[2], "onChallenge should not be called");
        }
    }

    // =========================================================================

    @Nested
    class EmptyResult {

        @Test
        void result_is_empty_when_no_handler_is_registered() {
            var result = new Decision<String>(decidedToBlock()).result();
            assertEquals(Optional.empty(), result);
        }

        @Test
        void result_is_empty_when_only_non_matching_handlers_are_registered() {
            var result = new Decision<String>(decidedToBlock())
                    .onProceed(() -> "proceed")
                    .onChallenge(() -> "challenge")
                    .onDefer(() -> "deferred")
                    .result();

            assertEquals(Optional.empty(), result);
        }
    }

    // =========================================================================

    @Nested
    class RunnableHandlers {

        @Test
        void onBlock_runnable_is_invoked_for_block_verdict() {
            boolean[] ran = {false};
            new Decision<Void>(decidedToBlock())
                    .onBlock(() -> ran[0] = true)
                    .result();
            assertTrue(ran[0]);
        }

        @Test
        void onProceed_runnable_is_invoked_for_proceed_verdict() {
            boolean[] ran = {false};
            new Decision<Void>(decidedToProceed())
                    .onProceed(() -> ran[0] = true)
                    .result();
            assertTrue(ran[0]);
        }

        @Test
        void onChallenge_runnable_is_invoked_for_challenge_verdict() {
            boolean[] ran = {false};
            new Decision<Void>(decidedToChallenge())
                    .onChallenge(() -> ran[0] = true)
                    .result();
            assertTrue(ran[0]);
        }

        @Test
        void onDefer_runnable_is_invoked_for_defer_verdict() {
            boolean[] ran = {false};
            new Decision<Void>(decidedToDefer())
                    .onDefer(() -> ran[0] = true)
                    .result();
            assertTrue(ran[0]);
        }

        @Test
        void runnable_is_not_invoked_when_verdict_does_not_match() {
            boolean[] ran = {false, false, false};

            new Decision<Void>(decidedToBlock())
                    .onBlock(() -> {})
                    .onProceed(() -> ran[0] = true)
                    .onChallenge(() -> ran[1] = true)
                    .onDefer(() -> ran[2] = true)
                    .result();

            assertFalse(ran[0], "onProceed runnable should not run");
            assertFalse(ran[1], "onChallenge runnable should not run");
            assertFalse(ran[2], "onDefer runnable should not run");
        }

        @Test
        void result_is_empty_when_only_runnable_handler_fires() {
            var result = new Decision<String>(decidedToBlock())
                    .onBlock(() -> {})
                    .result();
            assertEquals(Optional.empty(), result);
        }

        @Test
        void runnable_and_supplier_overloads_can_be_mixed_on_different_verdicts() {
            boolean[] ran = {false};
            var result = new Decision<String>(decidedToBlock())
                    .onBlock(() -> ran[0] = true)
                    .onProceed(() -> "proceed")
                    .result();

            assertTrue(ran[0]);
            assertEquals(Optional.empty(), result);
        }

        @Test
        void runnable_overload_returns_same_decision_instance() {
            var decision = new Decision<Void>(decidedToBlock());
            assertSame(decision, decision.onBlock(() -> {}));
            assertSame(decision, decision.onProceed(() -> {}));
            assertSame(decision, decision.onChallenge(() -> {}));
            assertSame(decision, decision.onDefer(() -> {}));
        }
    }

    // =========================================================================

    @Nested
    class SenderDispatching {

        // Captures all five arguments forwarded by a Sender call
        private record Capture<R>(boolean[] fired, Object[] result, Decided[] decided,
                                  Object[] identifier, Map<?, ?>[] metadata) {

            static <R> Capture<R> empty() {
                return new Capture<>(new boolean[]{false}, new Object[]{null},
                        new Decided[]{null}, new Object[]{null}, new Map[]{null});
            }

            Sender<R> sender() {
                return (result, decided, detections, id, meta) -> {
                    fired[0] = true;
                    this.result[0] = result;
                    this.decided[0] = decided;
                    this.identifier[0] = id;
                    this.metadata[0] = meta;
                };
            }
        }

        // --- send (unconditional) ---

        @Test
        void send_fires_for_every_verdict() {
            for (Decided verdict : List.of(decidedToBlock(), decidedToProceed(), decidedToChallenge(), decidedToDefer())) {
                var capture = Capture.<String>empty();
                new Decision<String>(verdict).send(capture.sender()).result();
                assertTrue(capture.fired[0], "send should fire for " + verdict.type());
            }
        }

        @Test
        void send_receives_the_handler_result() {
            var capture = Capture.<String>empty();
            new Decision<String>(decidedToBlock())
                    .onBlock(() -> "blocked")
                    .send(capture.sender())
                    .result();
            assertEquals("blocked", capture.result[0]);
        }

        @Test
        void send_receives_null_result_when_runnable_handler_fires() {
            var capture = Capture.<String>empty();
            new Decision<String>(decidedToBlock())
                    .onBlock(() -> {})
                    .send(capture.sender())
                    .result();
            assertNull(capture.result[0]);
        }

        @Test
        void send_receives_the_verdict() {
            var capture = Capture.<String>empty();
            new Decision<String>(decidedToBlock()).send(capture.sender()).result();
            assertTrue(capture.decided[0].toBlock());
        }

        @Test
        void send_passes_detections_to_sender() {
            var detection = new DetectedStatus<>("stub", "ip", NOT_DETECTED);
            var detections = List.<io.unconquerable.intercept.detect.Detected<?>>of(detection);
            boolean[] received = {false};

            new Decision<String>(decidedToProceed(), detections)
                    .send((result, decided, d, id, meta) -> received[0] = d.contains(detection))
                    .result();

            assertTrue(received[0]);
        }

        @Test
        void send_can_be_chained_multiple_times() {
            boolean[] first  = {false};
            boolean[] second = {false};

            new Decision<String>(decidedToBlock())
                    .send((r, d, det, id, meta) -> first[0]  = true)
                    .send((r, d, det, id, meta) -> second[0] = true)
                    .result();

            assertTrue(first[0]);
            assertTrue(second[0]);
        }

        @Test
        void send_returns_same_decision_instance() {
            var decision = new Decision<String>(decidedToBlock());
            assertSame(decision, decision.send((r, d, det, id, meta) -> {}));
        }

        // --- sendOnBlock ---

        @Test
        void sendOnBlock_fires_only_for_block_verdict() {
            var capture = Capture.<String>empty();
            new Decision<String>(decidedToBlock()).sendOnBlock(capture.sender()).result();
            assertTrue(capture.fired[0]);
        }

        @Test
        void sendOnBlock_does_not_fire_for_non_block_verdicts() {
            for (Decided verdict : List.of(decidedToProceed(), decidedToChallenge(), decidedToDefer())) {
                var capture = Capture.<String>empty();
                new Decision<String>(verdict).sendOnBlock(capture.sender()).result();
                assertFalse(capture.fired[0], "sendOnBlock should not fire for " + verdict.type());
            }
        }

        // --- sendOnProceed ---

        @Test
        void sendOnProceed_fires_only_for_proceed_verdict() {
            var capture = Capture.<String>empty();
            new Decision<String>(decidedToProceed()).sendOnProceed(capture.sender()).result();
            assertTrue(capture.fired[0]);
        }

        @Test
        void sendOnProceed_does_not_fire_for_non_proceed_verdicts() {
            for (Decided verdict : List.of(decidedToBlock(), decidedToChallenge(), decidedToDefer())) {
                var capture = Capture.<String>empty();
                new Decision<String>(verdict).sendOnProceed(capture.sender()).result();
                assertFalse(capture.fired[0], "sendOnProceed should not fire for " + verdict.type());
            }
        }

        // --- sendOnChallenge ---

        @Test
        void sendOnChallenge_fires_only_for_challenge_verdict() {
            var capture = Capture.<String>empty();
            new Decision<String>(decidedToChallenge()).sendOnChallenge(capture.sender()).result();
            assertTrue(capture.fired[0]);
        }

        @Test
        void sendOnChallenge_does_not_fire_for_non_challenge_verdicts() {
            for (Decided verdict : List.of(decidedToBlock(), decidedToProceed(), decidedToDefer())) {
                var capture = Capture.<String>empty();
                new Decision<String>(verdict).sendOnChallenge(capture.sender()).result();
                assertFalse(capture.fired[0], "sendOnChallenge should not fire for " + verdict.type());
            }
        }

        // --- sendOnDefer ---

        @Test
        void sendOnDefer_fires_only_for_defer_verdict() {
            var capture = Capture.<String>empty();
            new Decision<String>(decidedToDefer()).sendOnDefer(capture.sender()).result();
            assertTrue(capture.fired[0]);
        }

        @Test
        void sendOnDefer_does_not_fire_for_non_defer_verdicts() {
            for (Decided verdict : List.of(decidedToBlock(), decidedToProceed(), decidedToChallenge())) {
                var capture = Capture.<String>empty();
                new Decision<String>(verdict).sendOnDefer(capture.sender()).result();
                assertFalse(capture.fired[0], "sendOnDefer should not fire for " + verdict.type());
            }
        }

        // --- sendUnlessBlocked ---

        @Test
        void sendUnlessBlocked_fires_for_proceed_challenge_and_defer() {
            for (Decided verdict : List.of(decidedToProceed(), decidedToChallenge(), decidedToDefer())) {
                var capture = Capture.<String>empty();
                new Decision<String>(verdict).sendUnlessBlocked(capture.sender()).result();
                assertTrue(capture.fired[0], "sendUnlessBlocked should fire for " + verdict.type());
            }
        }

        @Test
        void sendUnlessBlocked_does_not_fire_for_block_verdict() {
            var capture = Capture.<String>empty();
            new Decision<String>(decidedToBlock()).sendUnlessBlocked(capture.sender()).result();
            assertFalse(capture.fired[0]);
        }

        // --- sendUnlessProceed ---

        @Test
        void sendUnlessProceed_fires_for_block_challenge_and_defer() {
            for (Decided verdict : List.of(decidedToBlock(), decidedToChallenge(), decidedToDefer())) {
                var capture = Capture.<String>empty();
                new Decision<String>(verdict).sendUnlessProceed(capture.sender()).result();
                assertTrue(capture.fired[0], "sendUnlessProceed should fire for " + verdict.type());
            }
        }

        @Test
        void sendUnlessProceed_does_not_fire_for_proceed_verdict() {
            var capture = Capture.<String>empty();
            new Decision<String>(decidedToProceed()).sendUnlessProceed(capture.sender()).result();
            assertFalse(capture.fired[0]);
        }

        // --- sendUnlessDefer ---

        @Test
        void sendUnlessDefer_fires_for_block_proceed_and_challenge() {
            for (Decided verdict : List.of(decidedToBlock(), decidedToProceed(), decidedToChallenge())) {
                var capture = Capture.<String>empty();
                new Decision<String>(verdict).sendUnlessDefer(capture.sender()).result();
                assertTrue(capture.fired[0], "sendUnlessDefer should fire for " + verdict.type());
            }
        }

        @Test
        void sendUnlessDefer_does_not_fire_for_defer_verdict() {
            var capture = Capture.<String>empty();
            new Decision<String>(decidedToDefer()).sendUnlessDefer(capture.sender()).result();
            assertFalse(capture.fired[0]);
        }
    }

    // =========================================================================

    @Nested
    class SenderWithContext {

        record TestInstrument(String type) implements InstrumentType {}
        record TestIdentifier(String accountId, String userId, TestInstrument instrument)
                implements InstrumentIdentifier<TestInstrument> {}

        private static final TestIdentifier ID  = new TestIdentifier("acct-1", "user-1", new TestInstrument("test"));
        private static final Map<String, Object> META = Map.of("traceId", "abc-123");

        // Captures identifier and metadata forwarded by the sender
        private record ContextCapture(Object[] identifier, Map<?, ?>[] metadata) {
            static ContextCapture empty() {
                return new ContextCapture(new Object[]{null}, new Map[]{null});
            }

            <R> Sender<R> sender() {
                return (result, decided, detections, id, meta) -> {
                    identifier[0] = id;
                    metadata[0]   = meta;
                };
            }
        }

        // ---- send overloads ----

        @Test
        void send_with_identifier_forwards_identifier_and_null_metadata() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).send(ctx.sender(), () -> ID).result();
            assertSame(ID, ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        @Test
        void send_with_metadata_forwards_metadata_and_null_identifier() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).send(ctx.sender(), META).result();
            assertNull(ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void send_with_identifier_and_metadata_forwards_both() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).send(ctx.sender(), () -> ID, META).result();
            assertSame(ID, ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        // ---- sendOnBlock overloads ----

        @Test
        void sendOnBlock_with_identifier_forwards_context_on_block_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendOnBlock(ctx.sender(), () -> ID).result();
            assertSame(ID, ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        @Test
        void sendOnBlock_with_identifier_does_not_evaluate_supplier_on_non_block_verdict() {
            boolean[] evaluated = {false};
            new Decision<String>(decidedToProceed())
                    .sendOnBlock((r, d, det, id, meta) -> {}, () -> { evaluated[0] = true; return ID; })
                    .result();
            assertFalse(evaluated[0], "identifier supplier should not be called when verdict is not BLOCK");
        }

        @Test
        void sendOnBlock_with_metadata_forwards_context_on_block_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendOnBlock(ctx.sender(), META).result();
            assertNull(ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendOnBlock_with_identifier_and_metadata_forwards_both_on_block_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendOnBlock(ctx.sender(), () -> ID, META).result();
            assertSame(ID, ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendOnBlock_with_context_does_not_fire_for_non_block_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToProceed()).sendOnBlock(ctx.sender(), () -> ID, META).result();
            assertNull(ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        // ---- sendOnProceed overloads ----

        @Test
        void sendOnProceed_with_identifier_forwards_context_on_proceed_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToProceed()).sendOnProceed(ctx.sender(), () -> ID).result();
            assertSame(ID, ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        @Test
        void sendOnProceed_with_identifier_does_not_evaluate_supplier_on_non_proceed_verdict() {
            boolean[] evaluated = {false};
            new Decision<String>(decidedToBlock())
                    .sendOnProceed((r, d, det, id, meta) -> {}, () -> { evaluated[0] = true; return ID; })
                    .result();
            assertFalse(evaluated[0], "identifier supplier should not be called when verdict is not PROCEED");
        }

        @Test
        void sendOnProceed_with_metadata_forwards_context_on_proceed_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToProceed()).sendOnProceed(ctx.sender(), META).result();
            assertNull(ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendOnProceed_with_identifier_and_metadata_forwards_both_on_proceed_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToProceed()).sendOnProceed(ctx.sender(), () -> ID, META).result();
            assertSame(ID, ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendOnProceed_with_context_does_not_fire_for_non_proceed_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendOnProceed(ctx.sender(), () -> ID, META).result();
            assertNull(ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        // ---- sendOnChallenge overloads ----

        @Test
        void sendOnChallenge_with_identifier_forwards_context_on_challenge_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToChallenge()).sendOnChallenge(ctx.sender(), () -> ID).result();
            assertSame(ID, ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        @Test
        void sendOnChallenge_with_identifier_does_not_evaluate_supplier_on_non_challenge_verdict() {
            boolean[] evaluated = {false};
            new Decision<String>(decidedToBlock())
                    .sendOnChallenge((r, d, det, id, meta) -> {}, () -> { evaluated[0] = true; return ID; })
                    .result();
            assertFalse(evaluated[0], "identifier supplier should not be called when verdict is not CHALLENGE");
        }

        @Test
        void sendOnChallenge_with_metadata_forwards_context_on_challenge_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToChallenge()).sendOnChallenge(ctx.sender(), META).result();
            assertNull(ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendOnChallenge_with_identifier_and_metadata_forwards_both_on_challenge_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToChallenge()).sendOnChallenge(ctx.sender(), () -> ID, META).result();
            assertSame(ID, ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendOnChallenge_with_context_does_not_fire_for_non_challenge_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendOnChallenge(ctx.sender(), () -> ID, META).result();
            assertNull(ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        // ---- sendOnDefer overloads ----

        @Test
        void sendOnDefer_with_identifier_forwards_context_on_defer_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToDefer()).sendOnDefer(ctx.sender(), () -> ID).result();
            assertSame(ID, ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        @Test
        void sendOnDefer_with_identifier_does_not_evaluate_supplier_on_non_defer_verdict() {
            boolean[] evaluated = {false};
            new Decision<String>(decidedToBlock())
                    .sendOnDefer((r, d, det, id, meta) -> {}, () -> { evaluated[0] = true; return ID; })
                    .result();
            assertFalse(evaluated[0], "identifier supplier should not be called when verdict is not DEFER");
        }

        @Test
        void sendOnDefer_with_metadata_forwards_context_on_defer_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToDefer()).sendOnDefer(ctx.sender(), META).result();
            assertNull(ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendOnDefer_with_identifier_and_metadata_forwards_both_on_defer_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToDefer()).sendOnDefer(ctx.sender(), () -> ID, META).result();
            assertSame(ID, ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendOnDefer_with_context_does_not_fire_for_non_defer_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendOnDefer(ctx.sender(), () -> ID, META).result();
            assertNull(ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        // ---- sendUnlessBlocked overloads ----

        @Test
        void sendUnlessBlocked_with_identifier_forwards_context_on_non_block_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToProceed()).sendUnlessBlocked(ctx.sender(), () -> ID).result();
            assertSame(ID, ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        @Test
        void sendUnlessBlocked_with_metadata_forwards_context_on_non_block_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToProceed()).sendUnlessBlocked(ctx.sender(), META).result();
            assertNull(ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendUnlessBlocked_with_identifier_and_metadata_forwards_both_on_non_block_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToProceed()).sendUnlessBlocked(ctx.sender(), () -> ID, META).result();
            assertSame(ID, ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendUnlessBlocked_with_context_does_not_fire_for_block_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendUnlessBlocked(ctx.sender(), () -> ID, META).result();
            assertNull(ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        // ---- sendUnlessProceed overloads ----

        @Test
        void sendUnlessProceed_with_identifier_forwards_context_on_non_proceed_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendUnlessProceed(ctx.sender(), () -> ID).result();
            assertSame(ID, ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        @Test
        void sendUnlessProceed_with_metadata_forwards_context_on_non_proceed_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendUnlessProceed(ctx.sender(), META).result();
            assertNull(ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendUnlessProceed_with_identifier_and_metadata_forwards_both_on_non_proceed_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendUnlessProceed(ctx.sender(), () -> ID, META).result();
            assertSame(ID, ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendUnlessProceed_with_context_does_not_fire_for_proceed_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToProceed()).sendUnlessProceed(ctx.sender(), () -> ID, META).result();
            assertNull(ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        // ---- sendUnlessDefer overloads ----

        @Test
        void sendUnlessDefer_with_identifier_forwards_context_on_non_defer_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendUnlessDefer(ctx.sender(), () -> ID).result();
            assertSame(ID, ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }

        @Test
        void sendUnlessDefer_with_metadata_forwards_context_on_non_defer_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendUnlessDefer(ctx.sender(), META).result();
            assertNull(ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendUnlessDefer_with_identifier_and_metadata_forwards_both_on_non_defer_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToBlock()).sendUnlessDefer(ctx.sender(), () -> ID, META).result();
            assertSame(ID, ctx.identifier[0]);
            assertSame(META, ctx.metadata[0]);
        }

        @Test
        void sendUnlessDefer_with_context_does_not_fire_for_defer_verdict() {
            var ctx = ContextCapture.empty();
            new Decision<String>(decidedToDefer()).sendUnlessDefer(ctx.sender(), () -> ID, META).result();
            assertNull(ctx.identifier[0]);
            assertNull(ctx.metadata[0]);
        }
    }

    // =========================================================================

    @Nested
    class Chaining {

        @Test
        void each_handler_method_returns_the_same_decision_instance() {
            var decision = new Decision<String>(decidedToBlock());

            assertSame(decision, decision.onBlock(() -> "blocked"));
            assertSame(decision, decision.onProceed(() -> "proceed"));
            assertSame(decision, decision.onChallenge(() -> "challenge"));
            assertSame(decision, decision.onDefer(() -> "deferred"));
        }

        @Test
        void result_is_stable_across_multiple_calls() {
            var decision = new Decision<String>(decidedToBlock())
                    .onBlock(() -> "blocked");

            assertEquals(decision.result(), decision.result());
        }
    }
}