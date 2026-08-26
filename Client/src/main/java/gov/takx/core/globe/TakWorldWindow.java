package gov.takx.core.globe;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;

/**
 * Custom {@link WorldWindow} for TAK globe. Exposes some necessary methods for rendering support.
 *
 * @since 3.4
 */
public interface TakWorldWindow extends WorldWindow
{
    PickedObject getCurrentSelection();

    PickedObjectList getCurrentBoxSelection();

    void callRenderingListeners(RenderingEvent renderingEvent);

    void callPositionListeners(PositionEvent positionEvent);

    void callRenderingExceptionListeners(Throwable t);

    void callSelectListeners(SelectEvent selectEvent);
}
