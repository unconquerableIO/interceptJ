package io.unconquerable.intercept.decide;

import java.util.Optional;

/**
 * Represents the verdict reached by a {@link Decider} after evaluating all fraud signals.
 *
 * <p>A {@code Decided} encapsulates both the {@link Type outcome type} and optional
 * {@link DecisionDetail} metadata, giving downstream code everything it needs to act on the
 * decision. Verdicts are consumed by {@link Decision}, which maps each outcome type to a
 * caller-supplied handler.
 *
 * <p>Instances are created through the static factory methods on this interface:
 * <pre>{@code
 * // Simple verdicts
 * Decided block     = Decided.decidedToBlock();
 * Decided proceed   = Decided.decidedToProceed();
 * Decided challenge = Decided.decidedToChallenge();
 * Decided defer     = Decided.decidedToDefer();
 *
 * // Verdicts with audit detail
 * Decided block = Decided.decidedToBlock(new FraudRuleDetail("VELOCITY_EXCEEDED"));
 * }</pre>
 *
 * <p>The convenience boolean methods ({@link #toBlock()}, {@link #toProceed()}, etc.) allow
 * {@link Decider} implementations to branch on prior verdicts when composing multiple decision
 * strategies.
 *
 * @author Rizwan Idrees
 * @see Decider
 * @see Decision
 * @see DecisionDetail
 */
public interface Decided {

    /**
     * The set of possible outcomes that a {@link Decider} can reach.
     */
    enum Type {

        /**
         * The request or action should be unconditionally rejected. Use when fraud signals
         * exceed the acceptable risk threshold and no further verification is possible or
         * warranted.
         */
        BLOCK,

        /**
         * The request or action should be allowed to continue without additional friction.
         * Use when all fraud signals are within acceptable bounds.
         */
        PROCEED,

        /**
         * The requester should be presented with an additional verification step — for example,
         * a CAPTCHA, OTP, or step-up authentication — before the action is permitted.
         */
        CHALLENGE,

        /**
         * A decision cannot be reached immediately and must be deferred to an asynchronous
         * review process or a human analyst. The action is typically held pending review.
         */
        DEFER
    }

    /**
     * Returns the outcome type of this verdict.
     *
     * @return a non-null {@link Type}
     */
    Type type();

    /**
     * Returns {@code true} if this verdict is {@link Type#BLOCK}.
     *
     * @return {@code true} when the action should be rejected
     */
    default boolean toBlock() {
        return type() == Type.BLOCK;
    }

    /**
     * Returns {@code true} if this verdict is {@link Type#PROCEED}.
     *
     * @return {@code true} when the action should be allowed
     */
    default boolean toProceed() {
        return type() == Type.PROCEED;
    }

    /**
     * Returns {@code true} if this verdict is {@link Type#CHALLENGE}.
     *
     * @return {@code true} when the requester should be challenged
     */
    default boolean toChallenge() {
        return type() == Type.CHALLENGE;
    }

    /**
     * Returns {@code true} if this verdict is {@link Type#DEFER}.
     *
     * @return {@code true} when the decision should be deferred for async review
     */
    default boolean toDefer() {
        return type() == Type.DEFER;
    }

    /**
     * Returns optional metadata that provides additional context about why this verdict was
     * reached, such as which rule was triggered or which detector produced the decisive signal.
     *
     * @return an {@link Optional} containing a {@link DecisionDetail}, or empty if no detail
     *         was provided
     */
    Optional<DecisionDetail> details();

    /**
     * Creates a {@link DecidedToBlock} verdict with no additional detail.
     *
     * @return a new {@link DecidedToBlock}
     */
    static DecidedToBlock decidedToBlock() {
        return decidedToBlock(null);
    }

    /**
     * Creates a {@link DecidedToBlock} verdict with optional audit detail.
     *
     * @param detail contextual metadata explaining why the request was blocked; may be
     *               {@code null}
     * @return a new {@link DecidedToBlock}
     */
    static DecidedToBlock decidedToBlock(DecisionDetail detail) {
        return new DecidedToBlock(detail);
    }

    /**
     * Creates a {@link DecidedToProceed} verdict with no additional detail.
     *
     * @return a new {@link DecidedToProceed}
     */
    static DecidedToProceed decidedToProceed() {
        return decidedToProceed(null);
    }

    /**
     * Creates a {@link DecidedToProceed} verdict with optional audit detail.
     *
     * @param detail contextual metadata explaining why the request was allowed; may be
     *               {@code null}
     * @return a new {@link DecidedToProceed}
     */
    static DecidedToProceed decidedToProceed(DecisionDetail detail) {
        return new DecidedToProceed(detail);
    }

    /**
     * Creates a {@link DecidedToChallenge} verdict with no additional detail.
     *
     * @return a new {@link DecidedToChallenge}
     */
    static DecidedToChallenge decidedToChallenge() {
        return decidedToChallenge(null);
    }

    /**
     * Creates a {@link DecidedToChallenge} verdict with optional audit detail.
     *
     * @param detail contextual metadata explaining why a challenge is required; may be
     *               {@code null}
     * @return a new {@link DecidedToChallenge}
     */
    static DecidedToChallenge decidedToChallenge(DecisionDetail detail) {
        return new DecidedToChallenge(detail);
    }

    /**
     * Creates a {@link DecidedToDefer} verdict with no additional detail.
     *
     * @return a new {@link DecidedToDefer}
     */
    static DecidedToDefer decidedToDefer() {
        return decidedToDefer(null);
    }

    /**
     * Creates a {@link DecidedToDefer} verdict with optional audit detail.
     *
     * @param detail contextual metadata explaining why the decision is being deferred; may be
     *               {@code null}
     * @return a new {@link DecidedToDefer}
     */
    static DecidedToDefer decidedToDefer(DecisionDetail detail) {
        return new DecidedToDefer(detail);
    }
}
