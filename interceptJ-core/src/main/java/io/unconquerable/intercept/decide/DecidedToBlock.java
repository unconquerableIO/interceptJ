package io.unconquerable.intercept.decide;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Optional;

/**
 * A {@link Decided} verdict that unconditionally rejects the intercepted request or action.
 *
 * <p>Returned by {@link Decided#decidedToBlock()} or {@link Decided#decidedToBlock(DecisionDetail)}
 * when a {@link Decider} determines that fraud signals exceed the acceptable risk threshold and
 * the action must not be permitted. The corresponding handler in a {@link Decision} pipeline is
 * {@link Decision#onBlock(java.util.function.Supplier) onBlock()}.
 *
 * @param type   always {@link Decided.Type#BLOCK}; present as a record component to satisfy the
 *               canonical constructor but cannot be overridden at the call site
 * @param detail optional metadata explaining the reason for the block verdict; may be
 *               {@code null}
 * @author Rizwan Idrees
 * @see Decided#decidedToBlock()
 * @see Decided#decidedToBlock(DecisionDetail)
 * @see Decision#onBlock(java.util.function.Supplier)
 */
public record DecidedToBlock(@Nonnull Decided.Type type, @Nullable DecisionDetail detail) implements Decided {

    /**
     * Creates a {@code DecidedToBlock} with an optional {@link DecisionDetail}.
     *
     * @param detail contextual metadata for audit or logging purposes; may be {@code null}
     */
    public DecidedToBlock(@Nullable DecisionDetail detail) {
        this(Type.BLOCK, detail);
    }

    /** {@inheritDoc} */
    @Override
    public Decided.Type type() {
        return Type.BLOCK;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<DecisionDetail> details() {
        return Optional.ofNullable(detail);
    }
}
