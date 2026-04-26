package io.unconquerable.intercept.decide;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Optional;

/**
 * A {@link Decided} verdict that permits the intercepted request or action to continue.
 *
 * <p>Returned by {@link Decided#decidedToProceed()} or
 * {@link Decided#decidedToProceed(DecisionDetail)} when a {@link Decider} determines that all
 * fraud signals are within acceptable bounds and the action should be allowed without additional
 * friction. The corresponding handler in a {@link Decision} pipeline is
 * {@link Decision#onProceed(java.util.function.Supplier) onProceed()}.
 *
 * @param type   always {@link Decided.Type#PROCEED}; present as a record component to satisfy
 *               the canonical constructor but cannot be overridden at the call site
 * @param detail optional metadata explaining the reason for the proceed verdict; may be
 *               {@code null}
 * @author Rizwan Idrees
 * @see Decided#decidedToProceed()
 * @see Decided#decidedToProceed(DecisionDetail)
 * @see Decision#onProceed(java.util.function.Supplier)
 */
public record DecidedToProceed(@Nonnull Type type, @Nullable DecisionDetail detail) implements Decided {

    /**
     * Creates a {@code DecidedToProceed} with an optional {@link DecisionDetail}.
     *
     * @param detail contextual metadata for audit or logging purposes; may be {@code null}
     */
    public DecidedToProceed(@Nullable DecisionDetail detail) {
        this(Type.PROCEED, detail);
    }

    /** {@inheritDoc} */
    @Override
    public Decided.Type type() {
        return Type.PROCEED;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<DecisionDetail> details() {
        return Optional.ofNullable(detail);
    }
}
