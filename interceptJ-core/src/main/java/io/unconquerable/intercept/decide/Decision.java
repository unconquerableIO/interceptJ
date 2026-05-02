package io.unconquerable.intercept.decide;

import io.unconquerable.intercept.detect.Detected;
import io.unconquerable.intercept.instrument.InstrumentIdentifier;
import io.unconquerable.intercept.instrument.InstrumentType;
import io.unconquerable.intercept.send.Sender;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
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
    private final List<Detected<?>> detections;
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
        this(decided, List.of());
    }


    /**
     * Constructs a {@code Decision} wrapping the given verdict and the full list of detection
     * results produced during the pipeline execution.
     *
     * <p>The {@code detections} list is forwarded to any {@link Sender} registered via
     * {@link #send(Sender)} and its conditional variants, giving senders access to the raw
     * per-detector signals alongside the final verdict.
     *
     * <p>This constructor is called internally by
     * {@link io.unconquerable.intercept.Interceptor#decide(Decider)}.
     *
     * @param decided    the verdict produced by the {@link Decider}; must not be {@code null}
     * @param detections the ordered list of {@link Detected} results from all registered
     *                   detectors; must not be {@code null}
     */
    public Decision(@Nonnull Decided decided, @Nonnull List<Detected<?>> detections) {
        this.decided = decided;
        this.detections = detections;
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
     * Registers a side-effect handler to invoke when the verdict is {@link Decided.Type#BLOCK}.
     *
     * <p>Use this overload when the block outcome requires only a side effect — such as writing
     * an audit log entry, incrementing a metric, or publishing an event — and no return value
     * is needed. Because {@link Runnable} produces no value, {@link #result()} will return
     * {@link Optional#empty()} even when this handler fires.
     *
     * <p>To both perform a side effect and produce a result, use
     * {@link #onBlock(Supplier)} instead.
     *
     * @param runnable the side-effect handler to run when the request is blocked; must not be
     *                 {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> onBlock(@Nonnull Runnable runnable) {
        if (decided.toBlock()) {
            runnable.run();
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
     * Registers a side-effect handler to invoke when the verdict is {@link Decided.Type#PROCEED}.
     *
     * <p>Use this overload when the proceed outcome requires only a side effect — such as
     * recording a successful check, emitting a trace span, or updating a cache — and no return
     * value is needed. Because {@link Runnable} produces no value, {@link #result()} will return
     * {@link Optional#empty()} even when this handler fires.
     *
     * <p>To both perform a side effect and produce a result, use
     * {@link #onProceed(Supplier)} instead.
     *
     * @param runnable the side-effect handler to run when the request is allowed to proceed;
     *                 must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> onProceed(@Nonnull Runnable runnable) {
        if (decided.toProceed()) {
            runnable.run();
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
     * Registers a side-effect handler to invoke when the verdict is {@link Decided.Type#CHALLENGE}.
     *
     * <p>Use this overload when the challenge outcome requires only a side effect — such as
     * triggering a CAPTCHA session, publishing a challenge event, or incrementing a friction
     * counter — and no return value is needed. Because {@link Runnable} produces no value,
     * {@link #result()} will return {@link Optional#empty()} even when this handler fires.
     *
     * <p>To both perform a side effect and produce a result, use
     * {@link #onChallenge(Supplier)} instead.
     *
     * @param runnable the side-effect handler to run when a challenge is required; must not be
     *                 {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> onChallenge(@Nonnull Runnable runnable) {
        if (decided.toChallenge()) {
            runnable.run();
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
     * Registers a side-effect handler to invoke when the verdict is {@link Decided.Type#DEFER}.
     *
     * <p>Use this overload when the defer outcome requires only a side effect — such as
     * enqueuing a review task, notifying an analyst queue, or emitting a deferral metric —
     * and no return value is needed. Because {@link Runnable} produces no value,
     * {@link #result()} will return {@link Optional#empty()} even when this handler fires.
     *
     * <p>To both perform a side effect and produce a result, use
     * {@link #onDefer(Supplier)} instead.
     *
     * @param runnable the side-effect handler to run when the decision is deferred; must not be
     *                 {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> onDefer(@Nonnull Runnable runnable) {
        if (decided.toDefer()) {
            runnable.run();
        }
        return this;
    }

    /**
     * Dispatches the complete pipeline context to the given {@link Sender}, unconditionally.
     *
     * <p>The sender receives the handler result (which may be {@code null} if a {@link Runnable}
     * handler was used or no handler fired), the {@link Decided} verdict, and the full list of
     * {@link io.unconquerable.intercept.detect.Detected} signals collected during detection.
     * Multiple senders may be chained by calling this method more than once.
     *
     * <p>This is the right choice for unconditional concerns such as audit logging, metrics
     * emission, or event publishing that must fire regardless of the verdict.
     *
     * @param sender the recipient that will process the pipeline outcome; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     * @see #sendOnBlock(Sender)
     * @see #sendOnProceed(Sender)
     * @see #sendOnChallenge(Sender)
     * @see #sendOnDefer(Sender)
     */
    public Decision<R> send(Sender<R> sender) {
        sender.send(result, decided, detections, null, null);
        return this;
    }

    /**
     * Dispatches the complete pipeline context to the given {@link Sender}, unconditionally,
     * enriched with the instrument identifier that scoped the pipeline execution.
     *
     * <p>The {@code identifier} supplier is evaluated lazily at call time. Use this overload when
     * the instrument context (tenant, user, and instrument data) should be forwarded to the sender
     * alongside the verdict and detections.
     *
     * @param sender     the recipient that will process the pipeline outcome; must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> send(Sender<R> sender,
                            Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier) {
        sender.send(result, decided, detections, identifier.get(), null);
        return this;
    }

    /**
     * Dispatches the complete pipeline context to the given {@link Sender}, unconditionally,
     * enriched with caller-supplied metadata.
     *
     * <p>Use this overload to attach arbitrary key-value pairs — such as request headers, trace
     * IDs, or feature flags — that the sender can use for routing or enrichment.
     *
     * @param sender   the recipient that will process the pipeline outcome; must not be {@code null}
     * @param metaData arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> send(Sender<R> sender,
                            Map<String, Object> metaData) {
        sender.send(result, decided, detections, null, metaData);
        return this;
    }

    /**
     * Dispatches the complete pipeline context to the given {@link Sender}, unconditionally,
     * enriched with both the instrument identifier and caller-supplied metadata.
     *
     * <p>This is the full-context overload. Use it when the sender requires both the instrument
     * identifier (for tenant/user scoping) and additional metadata (for routing or enrichment).
     *
     * @param sender     the recipient that will process the pipeline outcome; must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @param metaData   arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> send(Sender<R> sender,
                            Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier,
                            Map<String, Object> metaData) {
        sender.send(result, decided, detections, identifier.get(), metaData);
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#BLOCK}.
     *
     * <p>Use this when a downstream system — such as a fraud alert queue or a block-specific
     * audit trail — should only receive events for blocked requests.
     *
     * @param sender the recipient that will process the pipeline outcome on a block verdict;
     *               must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnBlock(Sender<R> sender) {
        if (decided.toBlock()) {
            sender.send(result, decided, detections, null, null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#BLOCK}, enriched with the instrument identifier.
     *
     * @param sender     the recipient that will process the pipeline outcome on a block verdict;
     *                   must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnBlock(Sender<R> sender,
                                   Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier) {
        if (decided.toBlock()) {
            sender.send(result, decided, detections, identifier.get(), null);
        }

        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#BLOCK}, enriched with caller-supplied metadata.
     *
     * @param sender   the recipient that will process the pipeline outcome on a block verdict;
     *                 must not be {@code null}
     * @param metaData arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnBlock(Sender<R> sender, Map<String, Object> metaData) {
        if (decided.toBlock()) {
            sender.send(result, decided, detections, null, metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#BLOCK}, enriched with both the instrument identifier and metadata.
     *
     * @param sender     the recipient that will process the pipeline outcome on a block verdict;
     *                   must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @param metaData   arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnBlock(Sender<R> sender,
                                   Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier,
                                   Map<String, Object> metaData) {
        if (decided.toBlock()) {
            sender.send(result, decided, detections, identifier.get(), metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#PROCEED}.
     *
     * <p>Use this when a downstream system should only receive events for requests that were
     * allowed to proceed — for example, a success-path analytics sink.
     *
     * @param sender the recipient that will process the pipeline outcome on a proceed verdict;
     *               must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnProceed(Sender<R> sender) {
        if (decided.toProceed()) {
            sender.send(result, decided, detections, null, null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#PROCEED}, enriched with the instrument identifier.
     *
     * @param sender     the recipient that will process the pipeline outcome on a proceed verdict;
     *                   must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnProceed(Sender<R> sender,
                                     Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier) {
        if (decided.toProceed()) {
            sender.send(result, decided, detections, identifier.get(), null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#PROCEED}, enriched with caller-supplied metadata.
     *
     * @param sender   the recipient that will process the pipeline outcome on a proceed verdict;
     *                 must not be {@code null}
     * @param metaData arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnProceed(Sender<R> sender, Map<String, Object> metaData) {
        if (decided.toProceed()) {
            sender.send(result, decided, detections, null, metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#PROCEED}, enriched with both the instrument identifier and metadata.
     *
     * @param sender     the recipient that will process the pipeline outcome on a proceed verdict;
     *                   must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @param metaData   arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnProceed(Sender<R> sender,
                                     Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier,
                                     Map<String, Object> metaData) {
        if (decided.toProceed()) {
            sender.send(result, decided, detections, identifier.get(), metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#CHALLENGE}.
     *
     * <p>Use this when a downstream system should only receive events for requests that require
     * additional verification — for example, a CAPTCHA orchestration service.
     *
     * @param sender the recipient that will process the pipeline outcome on a challenge verdict;
     *               must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnChallenge(Sender<R> sender) {
        if (decided.toChallenge()) {
            sender.send(result, decided, detections, null, null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#CHALLENGE}, enriched with the instrument identifier.
     *
     * @param sender     the recipient that will process the pipeline outcome on a challenge verdict;
     *                   must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnChallenge(Sender<R> sender,
                                       Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier) {
        if (decided.toChallenge()) {
            sender.send(result, decided, detections, identifier.get(), null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#CHALLENGE}, enriched with caller-supplied metadata.
     *
     * @param sender   the recipient that will process the pipeline outcome on a challenge verdict;
     *                 must not be {@code null}
     * @param metaData arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnChallenge(Sender<R> sender,
                                       Map<String, Object> metaData) {
        if (decided.toChallenge()) {
            sender.send(result, decided, detections, null, metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#CHALLENGE}, enriched with both the instrument identifier and metadata.
     *
     * @param sender     the recipient that will process the pipeline outcome on a challenge verdict;
     *                   must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @param metaData   arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnChallenge(Sender<R> sender,
                                       Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier,
                                       Map<String, Object> metaData) {
        if (decided.toChallenge()) {
            sender.send(result, decided, detections, identifier.get(), metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#DEFER}.
     *
     * <p>Use this when a downstream system should only receive events for requests that have been
     * sent for asynchronous review — for example, a manual review queue.
     *
     * @param sender the recipient that will process the pipeline outcome on a defer verdict;
     *               must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnDefer(Sender<R> sender) {
        if (decided.toDefer()) {
            sender.send(result, decided, detections, null, null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#DEFER}, enriched with the instrument identifier.
     *
     * @param sender     the recipient that will process the pipeline outcome on a defer verdict;
     *                   must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnDefer(Sender<R> sender,
                                   Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier) {
        if (decided.toDefer()) {
            sender.send(result, decided, detections, identifier.get(), null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#DEFER}, enriched with caller-supplied metadata.
     *
     * @param sender   the recipient that will process the pipeline outcome on a defer verdict;
     *                 must not be {@code null}
     * @param metaData arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnDefer(Sender<R> sender, Map<String, Object> metaData) {
        if (decided.toDefer()) {
            sender.send(result, decided, detections, null, metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} only when the verdict is
     * {@link Decided.Type#DEFER}, enriched with both the instrument identifier and metadata.
     *
     * @param sender     the recipient that will process the pipeline outcome on a defer verdict;
     *                   must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @param metaData   arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendOnDefer(Sender<R> sender,
                                   Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier,
                                   Map<String, Object> metaData) {
        if (decided.toDefer()) {
            sender.send(result, decided, detections, identifier.get(), metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#BLOCK}.
     *
     * <p>Use this when a downstream system should receive all non-blocked outcomes — for example,
     * a downstream service that should only be notified when a request is not outright rejected.
     *
     * @param sender the recipient that will process the pipeline outcome unless the verdict is
     *               BLOCK; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessBlocked(Sender<R> sender) {
        if (!decided.toBlock()) {
            sender.send(result, decided, detections, null, null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#BLOCK}, enriched with the instrument identifier.
     *
     * @param sender     the recipient that will process the pipeline outcome unless the verdict is
     *                   BLOCK; must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessBlocked(Sender<R> sender,
                                         Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier) {
        if (!decided.toBlock()) {
            sender.send(result, decided, detections, identifier.get(), null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#BLOCK}, enriched with caller-supplied metadata.
     *
     * @param sender   the recipient that will process the pipeline outcome unless the verdict is
     *                 BLOCK; must not be {@code null}
     * @param metaData arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessBlocked(Sender<R> sender,
                                         Map<String, Object> metaData) {
        if (!decided.toBlock()) {
            sender.send(result, decided, detections, null, metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#BLOCK}, enriched with both the instrument identifier
     * and metadata.
     *
     * @param sender     the recipient that will process the pipeline outcome unless the verdict is
     *                   BLOCK; must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @param metaData   arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessBlocked(Sender<R> sender,
                                         Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier,
                                         Map<String, Object> metaData) {
        if (!decided.toBlock()) {
            sender.send(result, decided, detections, identifier.get(), metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#PROCEED}.
     *
     * <p>Use this when a downstream system should only be notified about non-clean outcomes —
     * for example, an anomaly tracker that is only interested in blocks, challenges, and deferrals.
     *
     * @param sender the recipient that will process the pipeline outcome unless the verdict is
     *               PROCEED; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessProceed(Sender<R> sender) {
        if (!decided.toProceed()) {
            sender.send(result, decided, detections, null, null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#PROCEED}, enriched with the instrument identifier.
     *
     * @param sender     the recipient that will process the pipeline outcome unless the verdict is
     *                   PROCEED; must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessProceed(Sender<R> sender,
                                         Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier) {
        if (!decided.toProceed()) {
            sender.send(result, decided, detections, identifier.get(), null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#PROCEED}, enriched with caller-supplied metadata.
     *
     * @param sender   the recipient that will process the pipeline outcome unless the verdict is
     *                 PROCEED; must not be {@code null}
     * @param metaData arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessProceed(Sender<R> sender, Map<String, Object> metaData) {
        if (!decided.toProceed()) {
            sender.send(result, decided, detections, null, metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#PROCEED}, enriched with both the instrument identifier
     * and metadata.
     *
     * @param sender     the recipient that will process the pipeline outcome unless the verdict is
     *                   PROCEED; must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @param metaData   arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessProceed(Sender<R> sender,
                                         Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier,
                                         Map<String, Object> metaData) {
        if (!decided.toProceed()) {
            sender.send(result, decided, detections, identifier.get(), metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#DEFER}.
     *
     * <p>Use this when a downstream system should receive all immediately-resolved outcomes —
     * BLOCK, PROCEED, and CHALLENGE — but not requests that are still pending asynchronous review.
     *
     * @param sender the recipient that will process the pipeline outcome unless the verdict is
     *               DEFER; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessDefer(Sender<R> sender) {
        if (!decided.toDefer()) {
            sender.send(result, decided, detections, null, null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#DEFER}, enriched with the instrument identifier.
     *
     * @param sender     the recipient that will process the pipeline outcome unless the verdict is
     *                   DEFER; must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessDefer(Sender<R> sender,
                                       Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier) {
        if (!decided.toDefer()) {
            sender.send(result, decided, detections, identifier.get(), null);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#DEFER}, enriched with caller-supplied metadata.
     *
     * @param sender   the recipient that will process the pipeline outcome unless the verdict is
     *                 DEFER; must not be {@code null}
     * @param metaData arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessDefer(Sender<R> sender, Map<String, Object> metaData) {
        if (!decided.toDefer()) {
            sender.send(result, decided, detections, null, metaData);
        }
        return this;
    }

    /**
     * Dispatches the pipeline context to the given {@link Sender} for every verdict
     * <em>except</em> {@link Decided.Type#DEFER}, enriched with both the instrument identifier
     * and metadata.
     *
     * @param sender     the recipient that will process the pipeline outcome unless the verdict is
     *                   DEFER; must not be {@code null}
     * @param identifier supplier of the {@link InstrumentIdentifier} to attach; must not be {@code null}
     * @param metaData   arbitrary key-value pairs to forward to the sender; must not be {@code null}
     * @return this {@code Decision} for fluent chaining
     */
    public Decision<R> sendUnlessDefer(Sender<R> sender,
                                       Supplier<? extends InstrumentIdentifier<? extends InstrumentType>> identifier,
                                       Map<String, Object> metaData) {
        if (!decided.toDefer()) {
            sender.send(result, decided, detections, identifier.get(), metaData);
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