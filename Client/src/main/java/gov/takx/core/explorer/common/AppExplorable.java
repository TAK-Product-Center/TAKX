package gov.takx.core.explorer.common;

import gov.takx.api.apps.IAppInstance;
import gov.takx.api.explorer.ADataMonitorServicesExplorable;
import gov.takx.api.explorer.IAppExplorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.plugin.app.IImmutableAppInstance;
import jakarta.annotation.Nonnull;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Set;

/**
 * Explorable that represents a running app plugin.
 *
 * @since 2.16
 */
public class AppExplorable extends ADataMonitorServicesExplorable
        implements IAppExplorable
{
    /**
     * The set of key-value pairs supported by this {@link IExplorable}.
     */
    protected static final Set<String> KEY_SET = Collections.singleton(DATA_MONITORS_KEY);

    /**
     * Reference to the app that this {@link IExplorable} represents.
     */
    private IAppInstance appInstance;

    /**
     * Constructor.
     *
     * @param appInstance The app.
     */
    public AppExplorable(IAppInstance appInstance)
    {
        super(appInstance, null, appInstance.getUniqueKey(), false, appInstance.getAppDelegate().getAppDataMonitorServices());

        this.appInstance = appInstance;
    }

    @Override
    public boolean hasKey(String key)
    {
        return KEY_SET.contains(key);
    }

    @Override
    public BufferedImage getIconImage()
    {
        return dataMonitorServices.getIconImage();
    }

    @Override
    public Object getValue(String key)
    {
        return DATA_MONITORS_KEY.equals(key) ? getDataMonitors() : null;
    }

    @Override
    public IImmutableAppInstance getApp()
    {
        return appInstance;
    }

    @Nonnull
    @Override
    public String getDisplayName()
    {
        return appInstance.getName();
    }
}
