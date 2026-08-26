package gov.takx.api.plugin.device;

import java.util.EventListener;

/**
 * Receives notification of lock click or toggle events.
 *
 * @since 1.4
 */
public interface IConfigurationLockListener extends EventListener
{
    /**
     * Notifies the listener that the lock icon has been single-clicked by the user.
     */
    void onClick();

    /**
     * Provides the listener the opportunity to authenticate prior to enabling the engineering mode panels.  Returning
     * false from from this method from any listener denies the user access to the engineering mode panels.
     *
     * @return true, to vote to grant the user access to the engineering mode, false to vote against it.
     */
    boolean authenticateEngineeringMode();
}
