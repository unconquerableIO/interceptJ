package io.unconquerable.intercept.instrument;

/**
 * Identifies the target subject or instrument of an interception in a multi-tenant context.
 *
 * <p>An {@code InstrumentIdentifier} bundles together the three pieces of information
 * needed to route a detection request: which tenant ({@link #accountId}), which user
 * ({@link #userId}), and which domain-specific instrument ({@link #instrument}) is
 * being evaluated. Implementations are typically lightweight records.
 *
 * <p>Example:
 * <pre>{@code
 * record PaymentIdentifier(String accountId, String userId, CreditCard instrument)
 *         implements InstrumentIdentifier<CreditCard> {}
 * }</pre>
 *
 * @param <T> the {@link InstrumentType} carried by this identifier
 * @author Rizwan Idrees
 */
public interface InstrumentIdentifier<T extends InstrumentType> {

    /**
     * Returns the tenant key used to scope this request within a multi-tenant deployment.
     *
     * @return a non-null, non-empty account identifier
     */
    String accountId();

    /**
     * Returns the identifier of the user who owns or initiated the instrument.
     *
     * @return a non-null, non-empty user identifier
     */
    String userId();

    /**
     * Returns the domain-specific instrument being evaluated (e.g. a credit card, IP address).
     *
     * @return the instrument; never {@code null}
     */
    T instrument();
}



