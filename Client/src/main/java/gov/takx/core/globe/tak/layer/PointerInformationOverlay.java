package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.util.Collections2;
import gov.takx.core.globe.tak.layer.opengl.GLPointerInformationOverlay;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Set;

/**
 * {@link com.atakmap.map.layer.Layer} implementation for {@link GLPointerInformationOverlay}.
 *
 * @since 3.4
 */
public class PointerInformationOverlay extends AbstractLayer implements MouseMotionListener
{
    static
    {
        GLLayerFactory.register(GLPointerInformationOverlay.SPI);
    }

    public interface OnPointerLocationUpdateListener
    {
        void onPointerLocationUpdate(Object source, PointerInformationOverlay overlay, float x, float y);
    }

    private Set<OnPointerLocationUpdateListener> listeners = Collections2.newIdentityHashSet();

    public PointerInformationOverlay()
    {
        super("Pointer Information");
    }

    public synchronized void addOnPointerLocationUpdateListener(OnPointerLocationUpdateListener l)
    {
        listeners.add(l);
    }

    public synchronized void removeOnPointerLocationUpdateListener(OnPointerLocationUpdateListener l)
    {
        this.listeners.remove(l);
    }

    @Override
    public synchronized void mouseMoved(MouseEvent e)
    {
        for (OnPointerLocationUpdateListener l : this.listeners)
        {
            l.onPointerLocationUpdate(e.getSource(), this, e.getX(), e.getY());
        }
    }

    @Override
    public synchronized void mouseDragged(MouseEvent e)
    {
        for (OnPointerLocationUpdateListener l : this.listeners)
        {
            l.onPointerLocationUpdate(e.getSource(), this, e.getX(), e.getY());
        }
    }
}
