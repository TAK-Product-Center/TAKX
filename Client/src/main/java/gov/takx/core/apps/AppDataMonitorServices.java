package gov.takx.core.apps;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.plugin.app.IAppPluginDelegate;
import gov.takx.commons.constants.State;
import gov.takx.core.datamonitor.ADataMonitorServices;

import java.awt.image.BufferedImage;
import java.util.Date;

/**
 * Manages app data monitors.
 *
 * @since 2.16
 */
@PseudoBean
public class AppDataMonitorServices extends ADataMonitorServices<IAppPluginDelegate>
{
    public AppDataMonitorServices(IAppPluginDelegate delegate, String family, String type, String name, BufferedImage iconImage)
    {
        super(delegate, family, type, name, delegate.getAppId(), iconImage);
    }

    @Override
    protected DataMonitorDelegate makeDataMonitorDelegate(IAppPluginDelegate delegate)
    {
        return new DataMonitorDelegate(delegate);
    }

    /**
     * Provides app data monitors with additional functionality.
     */
    protected class DataMonitorDelegate extends ADataMonitorDelegate
    {
        public DataMonitorDelegate(IAppPluginDelegate delegate)
        {
            super(delegate, delegate::getQueryService);
        }

        @Override
        public void logAlarmEvent(String message, String details)
        {
            eventLogManager.logPluginEvent(new Date(), family, type, name, message, details);
        }

        @Override
        public void logDataMonitorStateChangeEvent(IDataMonitor dataMonitor, State curState, String sDetails)
        {
            eventLogManager.logDataMonitorStateChange(new Date(), family, type, name, dataMonitor, curState, sDetails);
        }
    }
}
