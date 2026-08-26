package gov.takx.core.view.gui.chat;

import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.contact.IContact;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;

/**
 * Holds info on either a local {@link IContact} or a server contact (endpoint, server streaming id).
 *
 * @since 5.8
 */
record LocalOrServerContact(@Nullable IContact localContact, @Nullable ClientEndpoint clientEndpoint, @Nullable String streamingId, @Nullable Icon icon)
{
    public LocalOrServerContact {
        if (localContact == null && (clientEndpoint == null || streamingId == null))
        {
            throw new IllegalArgumentException("Either localContact or clientEndpoint/streamingId must be provided");
        }
    }

    @Nonnull
    public String callsign()
    {
        //noinspection DataFlowIssue
        return localContact != null ? localContact.getName() : clientEndpoint.callsign();
    }

    @Nonnull
    public String username()
    {
        //noinspection DataFlowIssue
        return localContact != null ? "" : clientEndpoint.username();
    }
}
