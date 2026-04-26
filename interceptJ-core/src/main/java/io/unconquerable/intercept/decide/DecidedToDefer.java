package io.unconquerable.intercept.decide;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Optional;

/**
 * A {@link Decided} verdict that holds the intercepted request or action for asynchronous review.
 *
 * <p>Returned by {@link Decided#decidedToDefer()} or
 * {@link Decided#decidedToDefer(DecisionDetail)} when a {@link Decider} cannot reach a
 * confident decision immediately — for example, when signals are ambiguous, when a manual fraud
 * review queue is the appropriate escalation path, or when enrichment data from an external
 * system is still pending. The action is typically accepted but held until a human analyst or
 * asynchronous process resolves the verdict. The corresponding handler in a {@link Decision}
 * pipeline is {@link Decision#onDefer(java.util.function.Supplier) onDefer()}.
 *
 * @param type   always {@link Decided.Type#DEFER}; present as a record component to satisfy
 *               the canonical constructor but cannot be overridden at the call site
 * @param detail optional metadata describing why a decision was deferred or which review queue
 *               it was sent to; may be {@code null}
 * @author Rizwan Idrees
 * @see Decided#decidedToDefer()
 * @see Decided#decidedToDefer(DecisionDetail)
 * @see Decision#onDefer(java.util.function.Supplier)
 */
public record DecidedToDefer(@Nonnull Type type, @Nullable DecisionDetail detail) implements Decided {

    /**
     * Creates a {@code DecidedToDefer} with an optional {@link DecisionDetail}.
     *
     * @param detail contextual metadata for audit or logging purposes; may be {@code null}
     */
    public DecidedToDefer(@Nullable DecisionDetail detail) {
        this(Type.DEFER, detail);
    }

    /** {@inheritDoc} */
    @Override
    public Type type() {
        return Type.DEFER;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<DecisionDetail> details() {
        return Optional.ofNullable(detail);
    }
}
