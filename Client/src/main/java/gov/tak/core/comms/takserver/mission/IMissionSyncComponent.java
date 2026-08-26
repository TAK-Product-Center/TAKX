package gov.tak.core.comms.takserver.mission;

import gov.tak.api.comms.takserver.ITakServerConnection;
import jakarta.annotation.Nonnull;

/**
 * Common outline for data-sync mission API components.
 *
 * @since 5.7
 */
public interface IMissionSyncComponent
{
    /**
     * The target connection for this component is assumed to always be the same.
     * Same in the sense of the target server. We may recreate a connection instance,
     * but it should still be pointing to the same place.
     *
     * @return Target connection for this component.
     */
    @Nonnull
    ITakServerConnection getConnection();

    /**
     * @param connection Connection to assign to this component.
     */
    void setConnection(@Nonnull ITakServerConnection connection);
}
