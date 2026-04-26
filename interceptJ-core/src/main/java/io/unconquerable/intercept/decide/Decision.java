package io.unconquerable.intercept.decide;

import jakarta.annotation.Nonnull;

import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * The result of an {@link io.unconquerable.intercept.Interceptor} pipeline execution, providing a
 * fluent API for handling each possible {@link Decided} outcome.
 *
 * <p>{@code Decision} is returned by
 * {@link io.unconquerable.intercept.Interceptor#decide(Decider)} and acts as a typed
 * handler dispatcher. Only the handler whose outcome type matches the {@link Decided} verdict
 * will have its {@link Supplier} invoked; all others are no-ops. This makes it safe to register
 * handlers for every possible outcome without conditional branching:
 *
 * <pre>{@code
 * Optional<Response> response = interceptor()
 *     .detect(request.getIpAddress(), ipDetector)
 *     .decide(decider)
 *     .onBlock(()     -> Response.status(403).build())
 *     .onChallenge(() -> Response.status(429).header("X-Challenge", "captcha").build())
 *     .onDefer(()     -> Response.status(202).build())
 *     .onProceed(()   -> service.handle(request))
 *     .result();
 * }</pre>
 *
 * <p>If no handler is registered for the active verdict (or all handlers are omitted),
 * {@link #result()} returns {@link Optional#empty()}.
 *
 * <p>{@code Decision} is not thread-safe and is intended to be used within a single
 * request-handling context.
 *
 * @param <R> the result type produced by outcome handlers; typically a HTTP response, a command
 *            object, or any domain-specific type
 * @author Rizwan Idrees
 * @see Decided
 * @see Decider
 * @see io.unconquerable.intercept.Interceptor
 */
public class Decision<R> {

    private final Decided decided;
    private R result;

    /**
     * Constructs a {@code Decision} wrapping the given verdict.
     *
     * <p>This constructor is called internally by
     * {@link io.unconquerable.intercept.Interceptor#decide(Decider)}.
     *
     * @param decided the verdict produced by the {@link Decider}; must not be {@code null}
     */
    public Decision(@Nonnull Decided decided) {
        this.decided = decided;
    }

    /**
     * Registers a handler to invoke when the verdict is {@link Decided.Type#BLOCK}.
     *
     * <p>The supplier is invoked immediately if the verdict matches; otherwise this call is a
     * no-op. Calling this method does not prevent other handlers from being registered.
     *
     * @param supplier the handler that produces a result when the request is blocked; must not
     *                 be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> onBlock(@Nonnull Supplier<R> supplier) {
        if (decided.toBlock()) {
            result = supplier.get();
        }
        return this;
    }

    /**
     * Registers a handler to invoke when the verdict is {@link Decided.Type#PROCEED}.
     *
     * <p>The supplier is invoked immediately if the verdict matches; otherwise this call is a
     * no-op. Calling this method does not prevent other handlers from being registered.
     *
     * @param supplier the handler that produces a result when the request is allowed to proceed;
     *                 must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> onProceed(@Nonnull Supplier<R> supplier) {
        if (decided.toProceed()) {
            result = supplier.get();
        }
        return this;
    }

    /**
     * Registers a handler to invoke when the verdict is {@link Decided.Type#CHALLENGE}.
     *
     * <p>The supplier is invoked immediately if the verdict matches; otherwise this call is a
     * no-op. Calling this method does not prevent other handlers from being registered.
     *
     * @param supplier the handler that produces a result when additional verification is
     *                 required; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> onChallenge(@Nonnull Supplier<R> supplier) {
        if (decided.toChallenge()) {
            result = supplier.get();
        }
        return this;
    }

    /**
     * Registers a handler to invoke when the verdict is {@link Decided.Type#DEFER}.
     *
     * <p>The supplier is invoked immediately if the verdict matches; otherwise this call is a
     * no-op. Calling this method does not prevent other handlers from being registered.
     *
     * @param supplier the handler that produces a result when the decision is deferred for async
     *                 review; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> onDefer(@Nonnull Supplier<R> supplier) {
        if (decided.toDefer()) {
            result = supplier.get();
        }
        return this;
    }

    /**
     * Returns the result produced by the matching outcome handler, if any.
     *
     * <p>Returns {@link Optional#empty()} when no handler was registered for the active verdict,
     * or when the matched handler's {@link Supplier} returned {@code null}.
     *
     * @return an {@link Optional} containing the handler's return value, or empty
     */
    public Optional<R> result() {
        return ofNullable(result);
    }
}
