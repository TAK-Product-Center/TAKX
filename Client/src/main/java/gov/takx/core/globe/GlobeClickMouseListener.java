package gov.takx.core.globe;

import gov.nasa.worldwind.WorldWindow;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import jakarta.inject.Inject;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Listener for clicks on the globe.
 *
 * @since 3.4.1 (moved and renamed from PointDropperMouseListener)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeClickMouseListener extends MouseAdapter
{
    private boolean armed;
    private boolean isDrag;
    private final Consumer<ILocation> callback;

    @Inject
    private IExtendedGlobeManager globeManager;

    /**
     * @param callback The callback to be notified when the mouse is pressed while listener is armed
     */
    public GlobeClickMouseListener(Consumer<ILocation> callback)
    {
        this.callback = callback;
    }

    /**
     * Arms or disarms the mouse listener for globe clicks.
     */
    public void setArmed(boolean armed)
    {
        this.armed = armed;
        globeManager.setSelectListenersActive(!armed);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        // Prevent other listeners from responding to the event; otherwise, AbstractViewInputHandler will update its
        // state but won't be cleared b/c we consume the mouseReleased event
        e.consume();
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent)
    {
        if (!mouseEvent.isConsumed()) isDrag = true;
    }

    /**
     * This is the event we care about.  If it is a left button press, then determine the location of the click and
     * invoke the action method.
     */
    @Override
    public void mouseReleased(MouseEvent mouseEvent)
    {
        if (!mouseEvent.isConsumed())
        {
            if (armed && !isDrag && mouseEvent.getButton() == MouseEvent.BUTTON1)
            {
                final WorldWindow wwd = (WorldWindow) mouseEvent.getSource();
                final ILocation location = globeManager.getGlobeForComponent(wwd).getGlobeLocationFromPoint(mouseEvent.getPoint());
                if (location != null) callback.accept(location); // Location may be null if user clicks outside of globe
                mouseEvent.consume();
            }

            isDrag = false;
        }
    }
}
