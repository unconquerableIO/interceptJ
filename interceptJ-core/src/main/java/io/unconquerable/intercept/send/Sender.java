package io.unconquerable.intercept.send;

import io.unconquerable.intercept.decide.Decided;
import io.unconquerable.intercept.detect.Detected;
import io.unconquerable.intercept.instrument.InstrumentIdentifier;
import io.unconquerable.intercept.instrument.InstrumentType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for dispatching the complete outcome of an interceptJ pipeline execution.
 *
 * <p>A {@code Sender} receives the full context produced by the pipeline — the typed result,
 * the {@link Decided} verdict, and every {@link Detected} signal collected during detection —
 * and forwards it to an external destination. Typical destinations include HTTP response writers,
 * audit event buses, message queues, monitoring systems, and structured loggers.
 *
 * <p>Because {@code Sender} is a {@link FunctionalInterface} it can be supplied as a lambda:
 *
 * <pre>{@code
 * Sender<ApiResponse> auditSender = (result, decided, detections, identifier, metadata) -> {
 *     auditLog.record(AuditEntry.builder()
 *         .verdict(decided.type())
 *         .detections(detections)
 *         .accountId(identifier != null ? identifier.accountId() : null)
 *         .metadata(metadata)
 *         .response(result)
 *         .build());
 * };
 * }</pre>
 *
 * <p>The {@code result} parameter is {@code @Nullable} because a {@link io.unconquerable.intercept.decide.Decision}
 * handler registered with a {@link Runnable} (rather than a {@link java.util.function.Supplier})
 * produces no return value, leaving {@code result} as {@code null}. Implementations should guard
 * against this when {@code result} is required downstream.
 *
 * <p>Implementations must be stateless or thread-safe if the same instance is reused across
 * multiple concurrent pipeline executions.
 *
 * @param <R> the result type produced by the {@link io.unconquerable.intercept.decide.Decision}
 *            handler; may be {@code null} when a {@link Runnable} handler was used
 * @author Rizwan Idrees
 * @see Decided
 * @see Detected
 * @see io.unconquerable.intercept.decide.Decision
 */
@FunctionalInterface
public interface Sender<R> {

    /**
     * Dispatches the complete pipeline outcome to an external destination.
     *
     * <p>This method is invoked after the {@link io.unconquerable.intercept.decide.Decision}
     * pipeline has been fully evaluated. All three parameters together provide a complete,
     * auditable record of the pipeline execution:
     * <ul>
     *   <li>{@code result} — the value produced by the matching outcome handler, if any</li>
     *   <li>{@code decided} — the verdict and any attached {@link io.unconquerable.intercept.decide.DecisionDetail}</li>
     *   <li>{@code detections} — the individual findings from every registered {@link io.unconquerable.intercept.detect.Detector}</li>
     * </ul>
     *
     * @param result     the typed result produced by the {@link io.unconquerable.intercept.decide.Decision}
     *                   handler; {@code null} when a {@link Runnable} handler was used or when no
     *                   handler was registered for the active verdict
     * @param decided    the verdict reached by the {@link io.unconquerable.intercept.decide.Decider};
     *                   never {@code null}
     * @param detections the complete, ordered list of {@link Detected} results from all registered
     *                   detectors, including any that were skipped by a
     *                   {@link io.unconquerable.intercept.detect.ConditionalDetector}; never {@code null}
     * @param identifier the {@link InstrumentIdentifier} that scopes the pipeline execution to a
     *                   specific tenant, user, and instrument; {@code null} when no identifier was
     *                   provided to the pipeline
     * @param metadata   arbitrary key-value pairs supplied by the caller for enrichment or routing
     *                   (e.g. request headers, trace IDs); {@code null} when not provided
     */
    void send(@Nullable R result,
              @Nonnull Decided decided,
              @Nonnull List<Detected<?>> detections,
              @Nullable InstrumentIdentifier<? extends InstrumentType> identifier,
              @Nullable Map<String, Object> metadata);
}
