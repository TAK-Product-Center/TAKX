package gov.takx.api.explorer;

import gov.takx.api.plugin.app.IImmutableAppInstance;

/**
 * @since 2.16
 */
public interface IAppExplorable extends IExplorable
{
    /**
     * Return the app instance.
     *
     * @return The app instance.
     */
    IImmutableAppInstance getApp();
}
