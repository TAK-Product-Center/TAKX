package gov.takx.api.configuration;

import gov.takx.api.annotation.NoPluginImplementations;

import java.util.UUID;

/**
 * Interface for representing this running application instance.
 *
 * @since 2.0.0
 */
@NoPluginImplementations
public interface IApplicationInstance
{
    /**
     * @return This node's UUID.
     */
    UUID getNodeUUID();

    /**
     * @return This node's call sign.
     * @since 3.0
     */
    String getCallSign();

    /**
     * @return The instance number of this application instance. The initial/default instance will have an instance
     * number of 0.
     */
    int getInstanceNumber();

    /**
     * @return {@code true} if this is the default instance of the application, indicated by a instance number of 0.
     */
    boolean isDefaultInstance();
}
