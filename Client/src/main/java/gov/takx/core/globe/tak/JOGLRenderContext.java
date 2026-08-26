package gov.takx.core.globe.tak;

import com.atakmap.map.RenderContext;
import com.atakmap.map.RenderSurface;
import com.atakmap.map.opengl.GLMapSurface;
import com.atakmap.util.ReadWriteLock;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import gov.nasa.worldwind.WorldWindow;
import gov.tak.api.engine.map.IRenderContextSpi;
import gov.takx.platform.preferences.PreferenceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;

/**
 * TAK globe render context.
 *
 * @since 3.4
 */
public final class JOGLRenderContext implements RenderContext, GLEventListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public final static IRenderContextSpi SPI = opaque -> opaque instanceof GlobeComponent globeComponent ? new JOGLRenderContext(globeComponent) : null;
    GLAutoDrawable impl;
    RenderSurface surface;
    Thread glThread;
    final ReadWriteLock eventQueueLock = new ReadWriteLock();
    final LinkedList<Runnable> eventQueue = new LinkedList<>();

    public JOGLRenderContext(GlobeComponent impl)
    {
        this.impl = impl;
        surface = impl;

        impl.addGLEventListener(this);
    }

    @Override
    public boolean isRenderThread()
    {
        // XXX - added JOGL 2.2
        //return impl.isThreadGLCapable();
        return (Thread.currentThread() == glThread);
    }

    @Override
    public void queueEvent(Runnable r)
    {
        eventQueueLock.acquireWrite();
        try
        {
            eventQueue.addLast(r);
        } finally
        {
            eventQueueLock.releaseWrite();
        }
        // Make sure event gets fired
        ((WorldWindow) impl).redraw();
    }

    @Override
    public void requestRefresh()
    {
        ((WorldWindow) impl).redraw();
    }

    @Override
    public void setFrameRate(float rate)
    {
        // XXX - no control on framerate
    }

    @Override
    public float getFrameRate()
    {
        return 0;
    }

    @Override
    public void setContinuousRenderEnabled(boolean enabled)
    {
        PreferenceEntry.DEV_MODE_CONTINUOUS_RENDERING_ENABLED.putAndSave(enabled);
    }

    @Override
    public boolean isContinuousRenderEnabled()
    {
        return PreferenceEntry.DEV_MODE_CONTINUOUS_RENDERING_ENABLED.getBooleanValue();
    }

    @Override
    public boolean supportsChildContext()
    {
        // XXX - not yet implemented
        return false;
    }

    @Override
    public RenderContext createChildContext()
    {
        // XXX - not yet implemented
        return null;
    }

    @Override
    public void destroyChildContext(gov.tak.api.engine.map.RenderContext child)
    {

    }

    @Override
    public boolean isAttached()
    {
        return isRenderThread() && impl.getContext().isCurrent();
    }

    @Override
    public boolean attach()
    {
        if (!isRenderThread()) return false;
        impl.getContext().makeCurrent();
        return impl.getContext().isCurrent();
    }

    @Override
    public boolean detach()
    {
        if (!isRenderThread()) return false;
        if (impl.getContext().isCurrent()) impl.getContext().release();
        return isAttached();
    }

    @Override
    public boolean isMainContext()
    {
        return true;
    }

    @Override
    public RenderSurface getRenderSurface()
    {
        return surface;
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        glThread = Thread.currentThread();
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        glThread = Thread.currentThread();
        GLMapSurface.setMainGLThread();

        while (true)
        {
            Runnable r;
            eventQueueLock.acquireRead();
            try
            {
                if (eventQueue.isEmpty()) break;
                r = eventQueue.removeFirst();
            } finally
            {
                eventQueueLock.releaseRead();
            }

            try
            {
                r.run();
            } catch (Throwable t)
            {
                logger.error("Exception during call to GL event queue", t);
            }
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
    }
}
