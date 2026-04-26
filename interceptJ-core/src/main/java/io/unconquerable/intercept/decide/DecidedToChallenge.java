package io.unconquerable.intercept.decide;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Optional;

/**
 * A {@link Decided} verdict that requires the requester to complete an additional verification
 * step before the action is permitted.
 *
 * <p>Returned by {@link Decided#decidedToChallenge()} or
 * {@link Decided#decidedToChallenge(DecisionDetail)} when a {@link Decider} determines that
 * fraud signals are elevated but not conclusive enough to block outright — for example, when a
 * risk score falls in an intermediate range. The action is held until the requester successfully
 * completes a CAPTCHA, OTP, or step-up authentication flow. The corresponding handler in a
 * {@link Decision} pipeline is
 * {@link Decision#onChallenge(java.util.function.Supplier) onChallenge()}.
 *
 * @param type   always {@link Decided.Type#CHALLENGE}; present as a record component to satisfy
 *               the canonical constructor but cannot be overridden at the call site
 * @param detail optional metadata describing the type of challenge required or the signals that
 *               triggered it; may be {@code null}
 * @author Rizwan Idrees
 * @see Decided#decidedToChallenge()
 * @see Decided#decidedToChallenge(DecisionDetail)
 * @see Decision#onChallenge(java.util.function.Supplier)
 */
public record DecidedToChallenge(@Nonnull Type type, @Nullable DecisionDetail detail) implements Decided {

    /**
     * Creates a {@code DecidedToChallenge} with an optional {@link DecisionDetail}.
     *
     * @param detail contextual metadata for audit or logging purposes; may be {@code null}
     */
    public DecidedToChallenge(@Nullable DecisionDetail detail) {
        this(Type.CHALLENGE, detail);
    }

    /** {@inheritDoc} */
    @Override
    public Type type() {
        return Type.CHALLENGE;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<DecisionDetail> details() {
        return Optional.ofNullable(detail);
    }
}
