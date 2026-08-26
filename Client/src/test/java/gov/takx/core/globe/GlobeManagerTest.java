package gov.takx.core.globe;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.globemessage.HideGlobeMessageEvent;
import gov.takx.api.event.globemessage.ShowGlobeMessageEvent;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.test.AIntegrationTest;
import jakarta.enterprise.event.Observes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.3
 */
public class GlobeManagerTest extends AIntegrationTest
{
    private IGlobeManager globeManager;

    @Override
    protected void postProjectOpen()
    {
        globeManager = getBean(IGlobeManager.class);
    }

    @Test
    void showGlobeMessage_firesEvent()
    {
        final EventListener eventListener = new EventListener();
        final var globe = globeManager.getPrimaryGlobeDelegate();

        globeManager.showGlobeMessage("test");
        assertThat(eventListener.showGlobeMessageEvent).isNotNull();
        assertThat(eventListener.showGlobeMessageEvent.getMessageText()).isEqualTo("test");
        assertThat(eventListener.showGlobeMessageEvent.getGlobe()).isNull();

        eventListener.reset();
        globeManager.showGlobeMessage("test2", null);
        assertThat(eventListener.showGlobeMessageEvent).isNotNull();
        assertThat(eventListener.showGlobeMessageEvent.getMessageText()).isEqualTo("test2");
        assertThat(eventListener.showGlobeMessageEvent.getGlobe()).isNull();

        eventListener.reset();
        globeManager.showGlobeMessage("test3", globe);
        assertThat(eventListener.showGlobeMessageEvent).isNotNull();
        assertThat(eventListener.showGlobeMessageEvent.getMessageText()).isEqualTo("test3");
        assertThat(eventListener.showGlobeMessageEvent.getGlobe()).isSameAs(globe);

        eventListener.dispose();
    }

    @Test
    void hideGlobeMessage_firesEvent()
    {
        final EventListener eventListener = new EventListener();
        final var globe = globeManager.getPrimaryGlobeDelegate();

        globeManager.hideGlobeMessage();
        assertThat(eventListener.hideGlobeMessageEvent).isNotNull();
        assertThat(eventListener.hideGlobeMessageEvent.getGlobe()).isNull();

        eventListener.reset();
        globeManager.hideGlobeMessage(globe);
        assertThat(eventListener.hideGlobeMessageEvent).isNotNull();
        assertThat(eventListener.hideGlobeMessageEvent.getGlobe()).isSameAs(globe);

        eventListener.dispose();
    }

    /**
     * Simple observer to test events.
     */
    @PseudoBean
    private static final class EventListener
    {
        ShowGlobeMessageEvent showGlobeMessageEvent = null;
        HideGlobeMessageEvent hideGlobeMessageEvent = null;

        private void onEvent(@Observes ShowGlobeMessageEvent showGlobeMessageEvent)
        {
            this.showGlobeMessageEvent = showGlobeMessageEvent;
        }

        private void onEvent(@Observes HideGlobeMessageEvent hideGlobeMessageEvent)
        {
            this.hideGlobeMessageEvent = hideGlobeMessageEvent;
        }

        void reset()
        {
            showGlobeMessageEvent = null;
            hideGlobeMessageEvent = null;
        }

        @RemoveObserver
        public void dispose()
        {
        }
    }
}
