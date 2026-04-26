package io.unconquerable.intercept.decide;

/**
 * Marker interface for attaching contextual metadata to a {@link Decided} verdict.
 *
 * <p>{@code DecisionDetail} allows {@link Decider} implementations to surface structured
 * audit information alongside a verdict — for example, which fraud rule was triggered, which
 * detector produced the decisive signal, or which risk threshold was breached. This metadata
 * travels with the verdict through the {@link Decision} pipeline and is available to outcome
 * handlers via {@link Decided#details()}.
 *
 * <p>Implement this interface with a record or class that carries the domain-specific fields
 * relevant to your decision logic:
 * <pre>{@code
 * public record FraudRuleDetail(String ruleId, String detectorName) implements DecisionDetail {}
 *
 * // Inside a Decider:
 * return Decided.decidedToBlock(new FraudRuleDetail("VELOCITY_EXCEEDED", "velocity-detector"));
 *
 * // Inside an outcome handler:
 * decision.onBlock(() -> {
 *     decided.details()
 *            .filter(d -> d instanceof FraudRuleDetail)
 *            .map(d -> (FraudRuleDetail) d)
 *            .ifPresent(detail -> auditLog.record(detail.ruleId()));
 *     return Response.status(403).build();
 * });
 * }</pre>
 *
 * @author Rizwan Idrees
 * @see Decided
 * @see Decision
 */
public interface DecisionDetail {
}
