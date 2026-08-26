package gov.takx.core.globe.layers.spiderfy;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Processes view change events to notify listeners of changes to the zoom from the user's view of the globe.
 *
 * @since 2.26
 */
public class ZoomEventManager implements PropertyChangeListener
{
    private double lastZoomElevation;

    private WorldWindow worldWindow;

    private Collection<IZoomListener> zoomListeners = new CopyOnWriteArrayList<>();

    ZoomEventManager(@Nonnull WorldWindow worldWindow)
    {
        this.worldWindow = worldWindow;

        lastZoomElevation = worldWindow.getView().getCurrentEyePosition().elevation;
        worldWindow.getView().addPropertyChangeListener(AVKey.VIEW, this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        double currentElevation = worldWindow.getView().getCurrentEyePosition().elevation;

        double percentChange = Math.abs((currentElevation - lastZoomElevation) / currentElevation);

        if (percentChange > 0.01)
        {
            zoomListeners.forEach(zoomListener -> zoomListener.zoomChange(worldWindow));

            lastZoomElevation = currentElevation;
        }
    }

    /**
     * Registers the parameter for updates of changes to the zoom from the user's view of the globe.
     *
     * @param zoomListener The object to register for zoom events.
     */
    void addZoomListener(@Nonnull IZoomListener zoomListener)
    {
        zoomListeners.add(zoomListener);
    }

    /**
     * Unregisters the parameter for updates of changes to the zoom from the user's view of the globe.
     *
     * @param zoomListener The object to register for zoom events.
     */
    void removeZoomListener(@Nonnull IZoomListener zoomListener)
    {
        zoomListeners.remove(zoomListener);
    }
}
