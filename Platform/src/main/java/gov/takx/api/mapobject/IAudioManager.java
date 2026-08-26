package gov.takx.api.mapobject;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.commons.constants.State;

import java.io.File;

/**
 * Plugin available functionality from the AudioManager
 *
 * @since 2.33
 */
@InjectableService
@NoPluginImplementations
public interface IAudioManager
{
    /**
     * Get the path of the default audio file to play for a specified alarm state
     *
     * @param state the alarm state
     * @return the path of the default audio file to play for the given state
     */
    File getAlertFile(final State state);
}
