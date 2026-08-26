package gov.tak.platform.events;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.BasicView;
import gov.tak.api.events.IGlobeEvent;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 2.15
 */
@SuppressWarnings("unused")
public class NwwGlobeEventsSourceTest extends AIntegrationTest
{
    @Mocked
    private WorldWindow nwwEventSource;

    @Mocked
    private Component component;

    @BeforeEach
    public void setupComponent()
    {
        when(component.getLocationOnScreen()).thenReturn(new Point());
    }

    @Test
    public void testEventPress()
    {
        TestNwwGlobeEventsSource source = new TestNwwGlobeEventsSource(nwwEventSource, GlobeEvent.PRESS);
        source.mousePressed(new MouseEvent(component, 1, 1, 0, 0, 0, 0, false, MouseEvent.BUTTON1));
        assertThat(source.getInvokeCount()).isEqualTo(1);
    }

    @Test
    public void testEventRelease()
    {
        TestNwwGlobeEventsSource source = new TestNwwGlobeEventsSource(nwwEventSource, GlobeEvent.RELEASE);
        source.mouseReleased(new MouseEvent(component, 1, 1, 0, 0, 0, 0, false, MouseEvent.BUTTON1));
        assertThat(source.getInvokeCount()).isEqualTo(1);
    }

    @Test
    public void testEventClick()
    {
        TestNwwGlobeEventsSource source = new TestNwwGlobeEventsSource(nwwEventSource, GlobeEvent.CLICK);
        source.mouseClicked(new MouseEvent(component, 1, 1, 0, 0, 0, 1, false, MouseEvent.BUTTON1));
        assertThat(source.getInvokeCount()).isEqualTo(1);
    }

    @Test
    public void testEventDoubleClick()
    {
        TestNwwGlobeEventsSource source = new TestNwwGlobeEventsSource(nwwEventSource, GlobeEvent.DOUBLE_CLICK);
        source.mouseClicked(new MouseEvent(component, 1, 1, 0, 0, 0, 2, false, MouseEvent.BUTTON1));
        assertThat(source.getInvokeCount()).isEqualTo(1);
    }

    @Test
    public void testEventPointerEnter()
    {
        TestNwwGlobeEventsSource source = new TestNwwGlobeEventsSource(nwwEventSource, GlobeEvent.POINTER_ENTER, false);
        source.moved(new PositionEvent(nwwEventSource, new Point(0, 0), null, Position.ZERO));
        assertThat(source.getInvokeCount()).isEqualTo(1);
    }

    @Test
    public void testEventPointerLeave()
    {
        TestNwwGlobeEventsSource source = new TestNwwGlobeEventsSource(nwwEventSource, GlobeEvent.POINTER_EXIT, true);
        source.moved(new PositionEvent(nwwEventSource, new Point(0, 0), null, null));
        assertThat(source.getInvokeCount()).isEqualTo(1);
    }

    @Test
    public void testEventPointerHover()
    {
        TestNwwGlobeEventsSource source = new TestNwwGlobeEventsSource(nwwEventSource, GlobeEvent.POINTER_HOVER, true);
        source.moved(new PositionEvent(nwwEventSource, new Point(0, 0), null, Position.ZERO));
        assertThat(source.getInvokeCount()).isEqualTo(1);
    }

    @Test
    public void testEventViewChanged()
    {
        when(nwwEventSource.getView()).thenReturn(new BasicView());
        when(nwwEventSource.getInputHandler()).thenReturn(mock(InputHandler.class));

        TestNwwGlobeEventsSource source = new TestNwwGlobeEventsSource(nwwEventSource, GlobeEvent.VIEW_CHANGING, true);
        source.start();
        nwwEventSource.getView().firePropertyChange(AVKey.VIEW, "old", "new");
        source.stop();
        assertThat(source.getInvokeCount()).isEqualTo(1);
    }

    private static class TestNwwGlobeEventsSource extends NwwGlobeEventsSource
    {
        final TestRaiseEventDelegate delegate;

        int getInvokeCount()
        {
            return delegate.invoked;
        }

        public TestNwwGlobeEventsSource(WorldWindow nwwEventSource, String eventType)
        {
            this(nwwEventSource, eventType, true);
        }

        public TestNwwGlobeEventsSource(WorldWindow nwwEventSource, String eventType, boolean startInsideGlobe)
        {
            this(nwwEventSource, eventType, startInsideGlobe, new TestRaiseEventDelegate(eventType));
        }

        private TestNwwGlobeEventsSource(WorldWindow nwwEventSource, String eventType, boolean startInsideGlobe, TestRaiseEventDelegate delegate)
        {
            super(nwwEventSource, 1, delegate);
            this.delegate = delegate;
            this.moved(new PositionEvent(nwwEventSource, new Point(0, 0), null, startInsideGlobe ? Position.ZERO : null));
            delegate.ready = true;
        }
    }

    private static class TestRaiseEventDelegate implements NwwGlobeEventsSource.RaiseEventDelegate
    {
        private final String eventType;
        private boolean ready = false;
        private int invoked = 0;

        public TestRaiseEventDelegate(String eventType)
        {
            this.eventType = eventType;
        }

        @Override
        public void raiseGlobeEvent(IGlobeEvent globeEvent)
        {
            if (ready)
            {
                assertThat(eventType).isEqualTo(globeEvent.getType());
                invoked++;
            }
        }
    }
}
