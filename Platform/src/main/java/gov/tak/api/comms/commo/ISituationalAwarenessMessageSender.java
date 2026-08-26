package gov.tak.api.comms.commo;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.cot.event.CotDetail;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Contract for the Situational Awareness CoT message sender service.
 *
 * @since 5.8
 */
@NoPluginImplementations
@InjectableService
@IncubatingApi(since = "5.8")
public interface ISituationalAwarenessMessageSender
{
    /**
     * Add a detail to be added to each outgoing Situational Awareness (SA/PLI) CoT message.  Adding the same detail
     * (based on object identity) more than once has no effect.
     *
     * @param detail Detail to add
     *
     * @implNote The {@code detail} reference is stored directly, a copy is not made, so any modifications made to the
     * given detail after calling this method <em>will</em> affect subsequent SA messages.
     *
     * @throws IllegalArgumentException If the given detail's name is one of the standard details used in SA messages:
     * {@code "contact", "__group", "status", "takv", "track", "uid", or "vmf"}
     */
    void addAdditionalDetail(@Nonnull CotDetail detail) throws IllegalArgumentException;

    /**
     * Remove a previously added detail.
     *
     * @param detail Detail to add
     */
    void removeAdditionalDetail(@Nonnull CotDetail detail);
}
