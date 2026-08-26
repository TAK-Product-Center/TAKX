package gov.takx.core.globe.tak;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.awt.GLJPanel;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.event.NoOpInputHandler;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingExceptionListener;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.util.PerformanceStatistic;
import gov.tak.api.engine.map.RenderSurface2;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.core.globe.TakWorldWindow;
import gov.takx.core.globe.TakWorldWindowImpl;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight implementation of the TAK globe rendering in a {@link javax.swing.JPanel}.
 *
 * @since 3.4
 */
public final class GlobePanel extends GLJPanel implements GlobeComponent, IWorldWindowGL, TakWorldWindow, RenderSurface2
{
    private final TakWorldWindow wwd;
    final Map<OnSizeChangedListener, ComponentListener> listeners = new IdentityHashMap<>();

    public GlobePanel(GLCapabilities glCapabilities)
    {
        super(glCapabilities);
        wwd = new TakWorldWindowImpl();
    }

    @Override
    public Component getComponent()
    {
        return this;
    }

    @Override
    public double getDpi()
    {
        return 96d;
    }

    @Override
    public void addOnSizeChangedListener(OnSizeChangedListener l)
    {
        ComponentListener cl;
        synchronized (listeners)
        {
            if (listeners.containsKey(l)) return;
            cl = new ComponentAdapter()
            {
                @Override
                public void componentResized(ComponentEvent e)
                {
                    l.onSizeChanged(GlobePanel.this, getSurfaceWidth(), getSurfaceHeight());
                }
            };
            listeners.put(l, cl);
        }
        addComponentListener(cl);
    }

    @Override
    public void removeOnSizeChangedListener(OnSizeChangedListener l)
    {
        ComponentListener cl;
        synchronized (listeners)
        {
            cl = listeners.remove(l);
            if (cl == null) return;
        }
        removeComponentListener(cl);
    }

    @Override
    public void shutdown()
    {
        // Don't draw anything else during any subsequent display() calls.
        ((ApplicationSceneController) wwd.getSceneController()).stopAllDrawing();
        invoke(false, glAutoDrawable -> {
            wwd.shutdown();
            return false;
        });
        // Fire queued call to wwd.shutdown()
        // Note that this doesn't get called if the globe window is already closed
        display();
    }

    @Override
    public boolean isEnableGpuCacheReinitialization()
    {
        return wwd.isEnableGpuCacheReinitialization();
    }

    @Override
    public void setEnableGpuCacheReinitialization(boolean enableGpuCacheReinitialization)
    {
        wwd.setEnableGpuCacheReinitialization(enableGpuCacheReinitialization);
    }

    @Override
    public InputHandler getInputHandler()
    {
        return wwd.getInputHandler();
    }

    @Override
    public void setInputHandler(InputHandler inputHandler)
    {
        if (wwd.getInputHandler() != null) wwd.getInputHandler().setEventSource(null);

        wwd.setInputHandler(inputHandler != null ? inputHandler : new NoOpInputHandler());
        if (inputHandler != null) inputHandler.setEventSource(this);
    }

    @Override
    public SceneController getSceneController()
    {
        return wwd.getSceneController();
    }

    @Override
    public void setSceneController(SceneController sceneController)
    {
        wwd.setSceneController(sceneController);
    }

    @Override
    public GpuResourceCache getGpuResourceCache()
    {
        return wwd.getGpuResourceCache();
    }

    @Override
    public void redraw()
    {
        repaint();
    }

    @Override
    public void redrawNow()
    {
        display();
    }

    @Override
    public void setModel(Model model)
    {
        wwd.setModel(model);
    }

    @Override
    public Model getModel()
    {
        return wwd.getModel();
    }

    @Override
    public void setView(View view)
    {
        if (view != null) wwd.setView(view);
    }

    @Override
    public View getView()
    {
        return wwd.getView();
    }

    @Override
    public void setModelAndView(Model model, View view)
    {
        setModel(model);
        setView(view);
    }

    @Override
    public void addRenderingListener(RenderingListener listener)
    {
        wwd.addRenderingListener(listener);
    }

    @Override
    public void removeRenderingListener(RenderingListener listener)
    {
        wwd.removeRenderingListener(listener);
    }

    @Override
    public void addSelectListener(SelectListener listener)
    {
        wwd.getInputHandler().addSelectListener(listener);
        wwd.addSelectListener(listener);
    }

    @Override
    public void removeSelectListener(SelectListener listener)
    {
        wwd.getInputHandler().removeSelectListener(listener);
        wwd.removeSelectListener(listener);
    }

    @Override
    public void addPositionListener(PositionListener listener)
    {
        wwd.addPositionListener(listener);
    }

    @Override
    public void removePositionListener(PositionListener listener)
    {
        wwd.removePositionListener(listener);
    }

    @Override
    public void addRenderingExceptionListener(RenderingExceptionListener listener)
    {
        wwd.addRenderingExceptionListener(listener);
    }

    @Override
    public void removeRenderingExceptionListener(RenderingExceptionListener listener)
    {
        wwd.removeRenderingExceptionListener(listener);
    }

    @Override
    public Position getCurrentPosition()
    {
        return wwd.getCurrentPosition();
    }

    @Override
    public PickedObjectList getObjectsAtCurrentPosition()
    {
        return wwd.getSceneController() != null ? wwd.getSceneController().getPickedObjectList() : null;
    }

    @Override
    public PickedObjectList getObjectsInSelectionBox()
    {
        return wwd.getSceneController() != null ? wwd.getSceneController().getObjectsInPickRectangle() : null;
    }

    @Override
    public Object setValue(String key, Object value)
    {
        return wwd.setValue(key, value);
    }

    @Override
    public AVList setValues(AVList avList)
    {
        return wwd.setValues(avList);
    }

    @Override
    public Object getValue(String key)
    {
        return wwd.getValue(key);
    }

    @Override
    public Collection<Object> getValues()
    {
        return wwd.getValues();
    }

    @Override
    public Set<Map.Entry<String, Object>> getEntries()
    {
        return wwd.getEntries();
    }

    @Override
    public String getStringValue(String key)
    {
        return wwd.getStringValue(key);
    }

    @Override
    public boolean hasKey(String key)
    {
        return wwd.hasKey(key);
    }

    @Override
    public Object removeKey(String key)
    {
        return wwd.removeKey(key);
    }

    @Override
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public void firePropertyChange(PropertyChangeEvent propertyChangeEvent)
    {
        wwd.firePropertyChange(propertyChangeEvent);
    }

    @Override
    public AVList copy()
    {
        return wwd.copy();
    }

    @Override
    public AVList clearList()
    {
        return wwd.clearList();
    }

    @Override
    public void setPerFrameStatisticsKeys(Set<String> keys)
    {
        wwd.setPerFrameStatisticsKeys(keys);
    }

    @Override
    public Collection<PerformanceStatistic> getPerFrameStatistics()
    {
        return wwd.getPerFrameStatistics();
    }

    @Override
    public PickedObject getCurrentSelection()
    {
        return wwd.getCurrentSelection();
    }

    @Override
    public PickedObjectList getCurrentBoxSelection()
    {
        return wwd.getCurrentBoxSelection();
    }

    @Override
    public void callRenderingListeners(RenderingEvent renderingEvent)
    {
        wwd.callRenderingListeners(renderingEvent);
    }

    @Override
    public void callPositionListeners(PositionEvent positionEvent)
    {
        wwd.callPositionListeners(positionEvent);
    }

    @Override
    public void callRenderingExceptionListeners(Throwable t)
    {
        wwd.callRenderingExceptionListeners(t);
    }

    @Override
    public void callSelectListeners(SelectEvent selectEvent)
    {
        wwd.callSelectListeners(selectEvent);
    }
}
