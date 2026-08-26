package gov.tak.platform.events;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.event.MessageListener;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.Position;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.engine.map.IGlobe;
import gov.tak.api.engine.map.ILayer;
import gov.tak.api.events.IGlobeEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.platform.graphics.PointF;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Internal class representing globe interaction event source based on a Nasa World Wind WorldWindow
 *
 * @since 3.3.0
 */
class NwwGlobeEventsSource implements PositionListener, MouseListener,
        MessageListener
{
    private final WorldWindow eventSource;
    private GeoPoint currentGlobeCoordinate;
    private Point lastScreenPoint;
    private final int globeId;
    private final RaiseEventDelegate delegate;
    private volatile boolean started;
    private final IGlobe globeInst = new GlobeInstance();

    private final PropertyChangeListener viewPropertyChangeListener = new PropertyChangeListener()
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            IGlobeEvent globeEvent = createGlobeEvent(IGlobeEvent.VIEW_CHANGING, 0, 0, 0, null);
            delegate.raiseGlobeEvent(globeEvent);
        }
    };

    /**
     * Callback for handling translated globe events
     */
    public interface RaiseEventDelegate
    {
        /**
         * Called on a newly translated globe event
         * @param globeEvent the globe event
         */
        void raiseGlobeEvent(IGlobeEvent globeEvent);
    }

    /**
     * Create a WorldWindow event source
     *
     * @param nwwEventSource the WorldWindow providing the events
     * @param coveredGlobeId the globe id of the produced IGlobeEvents
     * @param raiseEventDelegate the callback that handles translated globe events
     */
    public NwwGlobeEventsSource(@NonNull WorldWindow nwwEventSource, int coveredGlobeId, @NonNull RaiseEventDelegate raiseEventDelegate)
    {
        globeId = coveredGlobeId;
        delegate = raiseEventDelegate;
        eventSource = nwwEventSource;
    }

    /**
     * Start this event source translating source events into globe events
     */
    public void start()
    {
        if (!started)
        {
            eventSource.getInputHandler().addMouseListener(this);
            eventSource.addPositionListener(this);
            eventSource.getView().addPropertyChangeListener(AVKey.VIEW, viewPropertyChangeListener);

            // Tap into VIEW_STOPPED messages if the model is configured to be MessageTapModel. Otherwise, VIEW_SETTLED
            // events are not supported
            if (eventSource.getModel() instanceof MessageTapModel messageTap)
            {
                messageTap.addMessageListener(this);
            }

            started = true;
        }
    }

    /**
     * Stop this event source from translating source events into globe events
     */
    public void stop()
    {
        eventSource.getInputHandler().removeMouseListener(this);
        eventSource.removePositionListener(this);
        eventSource.getView().removePropertyChangeListener(AVKey.VIEW, viewPropertyChangeListener);

        if (eventSource.getModel() instanceof MessageTapModel messageTap)
        {
            messageTap.removeMessageListener(this);
        }

        started = false;
    }

    /**
     * Get the event source object used for globe event source
     *
     * @return the event source object
     */
    public Object getEventSource()
    {
        return eventSource;
    }

    @Override
    @SuppressWarnings("PMD.NullAssignment") // assignment to null is essential to implementation
    public void moved(PositionEvent positionEvent)
    {
        Point screenPoint = positionEvent.getScreenPoint();
        Position position = positionEvent.getPosition();

        if (position != null && screenPoint != null)
        {
            double lat = position.getLatitude().getDegrees();
            double lng = position.getLongitude().getDegrees();
            double alt = position.getAltitude();

            String type = currentGlobeCoordinate == null ? IGlobeEvent.POINTER_ENTER : IGlobeEvent.POINTER_HOVER;
            // even though this has alt reference of HAE, this refers to the getAltitude representation
            currentGlobeCoordinate = new GeoPoint(lat, lng, alt, GeoPoint.AltitudeReference.HAE);

            IGlobeEvent ge = createGlobeEvent(type, screenPoint.x, screenPoint.y, 0, currentGlobeCoordinate);
            delegate.raiseGlobeEvent(ge);
        } else
        {
            // moving off the globe without passing through open space will not provide a screenPoint
            if (screenPoint == null)
            {
                screenPoint = lastScreenPoint;
            }

            // raise the exit event
            if (currentGlobeCoordinate != null && screenPoint != null)
            {
                IGlobeEvent ge = createGlobeEvent(IGlobeEvent.POINTER_EXIT, screenPoint.x, screenPoint.y, 0, currentGlobeCoordinate);
                delegate.raiseGlobeEvent(ge);
            }

            currentGlobeCoordinate = null;
        }

        lastScreenPoint = screenPoint;
    }

    private int getButtonFlags(MouseEvent mouseEvent)
    {
        return switch (mouseEvent.getButton())
        {
            case MouseEvent.BUTTON1 -> IGlobeEvent.BUTTON_LEFT;
            case MouseEvent.BUTTON2 -> IGlobeEvent.BUTTON_MIDDLE;
            case MouseEvent.BUTTON3 -> IGlobeEvent.BUTTON_RIGHT;
            default -> 0;
        };
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (currentGlobeCoordinate != null)
        {
            String event = e.getClickCount() == 1 ? IGlobeEvent.CLICK : IGlobeEvent.DOUBLE_CLICK;
            int buttons = getButtonFlags(e);
            IGlobeEvent globeEvent = createGlobeEvent(event, e.getX(), e.getY(), buttons, currentGlobeCoordinate);
            delegate.raiseGlobeEvent(globeEvent);
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if (currentGlobeCoordinate != null)
        {
            int buttons = getButtonFlags(e);
            IGlobeEvent globeEvent = createGlobeEvent(IGlobeEvent.PRESS, e.getX(), e.getY(), buttons, currentGlobeCoordinate);
            delegate.raiseGlobeEvent(globeEvent);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (currentGlobeCoordinate != null)
        {
            int buttons = getButtonFlags(e);
            IGlobeEvent globeEvent = createGlobeEvent(IGlobeEvent.RELEASE, e.getX(), e.getY(), buttons, currentGlobeCoordinate);
            delegate.raiseGlobeEvent(globeEvent);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        // unused
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        // unused
    }

    @Override
    public void onMessage(Message message)
    {
        if (View.VIEW_STOPPED.equals(message.getName()))
        {
            IGlobeEvent globeEvent = createGlobeEvent(IGlobeEvent.VIEW_CHANGING, 0, 0, 0, null);
            delegate.raiseGlobeEvent(globeEvent);
        }
    }

    private IGlobeEvent createGlobeEvent(String type, int x, int y, int buttons, GeoPoint globeCoord)
    {
        PointF viewPosition = new PointF(x, y);

        Object[] targets = {globeInst};
        GeoPoint[] targetCoordinates = {globeCoord};

        return new GlobeEvent(type, eventSource, globeId, IGlobeEvent.MAIN_POINTER_ID, buttons, globeCoord, viewPosition, targets,
                targetCoordinates);
    }

    /**
     * A unique IGlobe instance used as the event target. All operations unsupported at this time.
     * When TAKX migrates to takkernel IGlobe implementation, this will be eliminated
     */
    private static class GlobeInstance implements IGlobe
    {
        @Override
        public void getLayers(List<ILayer> layers)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public ILayer getLayerAt(int position)
        {
            return null;
        }

        @Override
        public int getNumLayers()
        {
            return 0;
        }

        @Override
        public void setLayerPosition(ILayer layer, int position)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAllLayers()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeLayer(ILayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addLayer(int position, ILayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addLayer(ILayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addOnLayersChangedListener(OnLayersChangedListener l)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeOnLayersChangedListener(OnLayersChangedListener l)
        {
            throw new UnsupportedOperationException();
        }
    };
}
