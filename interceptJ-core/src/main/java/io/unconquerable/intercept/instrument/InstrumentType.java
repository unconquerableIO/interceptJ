package io.unconquerable.intercept.instrument;

/**
 * Marker interface for domain-specific instruments that can be evaluated by a detector.
 *
 * <p>Implement this interface on any record or class that represents a subject of fraud
 * detection or bot-protection analysis — for example a credit card, an IP address, or a
 * device fingerprint. The {@link #type()} method provides a stable, human-readable label
 * that can be used for logging, routing, or serialization.
 *
 * <p>Example:
 * <pre>{@code
 * record CreditCard(String number, String holder) implements InstrumentType {
 *     public String type() { return "credit-card"; }
 * }
 * }</pre>
 *
 * @author Rizwan Idrees
 */
public interface InstrumentType {

    /**
     * Returns a stable, human-readable label that identifies the kind of instrument.
     *
     * <p>The value should be constant for a given implementation (e.g. {@code "credit-card"},
     * {@code "ip-address"}) and suitable for use as a log field or routing key.
     *
     * @return a non-null, non-empty type label
     */
    String type();
}