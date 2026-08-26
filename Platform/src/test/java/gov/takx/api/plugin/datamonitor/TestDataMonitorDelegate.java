package gov.takx.api.plugin.datamonitor;

import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.globe.IFavoritesService;
import gov.takx.api.globe.IStateIconManager;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.ping.IGeoPingService;
import gov.takx.api.query.IQueryService;
import gov.takx.api.timer.ITimerService;
import gov.takx.commons.constants.State;

import java.net.URL;
import java.util.Date;

/**
 * Stub delegate so we can avoid spinning up a full integration test.
 *
 * @since 1.48
 */
public class TestDataMonitorDelegate implements IDataMonitorDelegate
{
    @Override
    public void setCustomIcon(URL imagePathURL)
    {
    }

    @Override
    public void logAlarmEvent(String message, String details)
    {
    }

    @Override
    public void logStateChangeEvent(Date logTime, IMapObject mapObject, State curState, String sDetails)
    {
    }

    @Override
    public void logDataMonitorStateChangeEvent(IDataMonitor dataMonitor, State curState, String sDetails)
    {
    }

    @Override
    public IQueryService getQueryService()
    {
        return null;
    }

    @Override
    public IStateIconManager getStateIconManager()
    {
        return null;
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystem()
    {
        return null;
    }

    @Override
    public IFavoritesService getFavoritesService()
    {
        return null;
    }

    @Override
    public IGeoPingService getGeoPingService()
    {
        return null;
    }

    @Override
    public ITimerService getTimerService()
    {
        return null;
    }
}
