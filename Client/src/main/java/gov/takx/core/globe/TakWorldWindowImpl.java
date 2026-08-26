package gov.takx.core.globe;

import com.jogamp.opengl.GLContext;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWindowImpl;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.BasicGpuResourceCache;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;

/**
 * Custom {@link WorldWindowImpl} for TAK globe. Exposes some necessary methods for rendering support.
 *
 * @since 3.4
 */
public final class TakWorldWindowImpl extends WorldWindowImpl implements TakWorldWindow
{
    @Override
    public GLContext getContext()
    {
        return null;
    }

    @Override
    public boolean isEnableGpuCacheReinitialization()
    {
        return true;  // default is true
    }

    @Override
    public void setEnableGpuCacheReinitialization(boolean enableGpuCacheReinitialization)
    {
    }

    @Override
    public void callRenderingListeners(RenderingEvent event)
    {
        super.callRenderingListeners(event);
    }

    @Override
    public void callPositionListeners(PositionEvent event)
    {
        super.callPositionListeners(event);
    }

    @Override
    public PickedObject getCurrentSelection()
    {
        return super.getCurrentSelection();
    }

    @Override
    public PickedObjectList getCurrentBoxSelection()
    {
        return super.getCurrentBoxSelection();
    }

    @Override
    public void callRenderingExceptionListeners(Throwable exception)
    {
        super.callRenderingExceptionListeners(exception);
    }

    @Override
    public void callSelectListeners(SelectEvent event)
    {
        super.callSelectListeners(event);
    }

    private static final long FALLBACK_TEXTURE_CACHE_SIZE = 60_000_000;

    public static GpuResourceCache createGpuResourceCache()
    {
        long cacheSize = Configuration.getLongValue(AVKey.TEXTURE_CACHE_SIZE, FALLBACK_TEXTURE_CACHE_SIZE);
        return new BasicGpuResourceCache((long) (0.8 * cacheSize), cacheSize);
    }
}
