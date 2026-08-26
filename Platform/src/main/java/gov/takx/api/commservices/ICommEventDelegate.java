package gov.takx.api.commservices;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.platform.commservices.ACommIn;
import gov.takx.platform.commservices.ACommOut;
import jakarta.annotation.Nonnull;

/**
 * Delegate for use with custom comm services that want to be able to publish comm in and comm out events.  Inject
 * into implementations of {@link ACommIn} and {@link ACommOut}.
 *
 */
@InjectableService
@NoPluginImplementations
public interface ICommEventDelegate
{
    /**
     * Publish a comm in event, resulting in stimulating the RX LED.
     *
     * @param source source of the Comm In, generally something uniquely describing the comm service
     * @since 3.1 (signature change)
     */
    void publishCommIn(@Nonnull String source);

    /**
     * Publish a comm out event, resulting in stimulating the TX LED.
     *
     * @param source source of the Comm Out, generally something uniquely describing the comm service
     * @since 3.1 (signature change)
     */
    void publishCommOut(@Nonnull String source);
}
