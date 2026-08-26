package gov.takx.core.view.gui.takserver;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.core.view.gui.takserver.timeline.DataFeedTimelinePanel;
import gov.takx.core.view.gui.takserver.timeline.TimelineEntry;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.SwingUtilities;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DataFeedTimelinePanel}
 *
 * @since 5.0
 */
@SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
public class DataFeedTimelinePanelTest extends AIntegrationTest
{
    private ITakServerConnection connection;

    @BeforeAll
    void setup(@Mocked ITakServerConnection connection) throws URISyntaxException
    {
        Mockito.when(connection.getUri()).thenReturn(new URI("http://localhost:8089/"));
        this.connection = connection;
    }

    @Test
    public void setSelected_selectsFeed()
    {
        final String feed1Name = "feed1";
        final String feed2Name = "feed2";

        DataFeedTimelinePanel panel = new DataFeedTimelinePanel((c, n) -> Collections.emptyList());

        panel.addFeed(connection, feed1Name);
        panel.addFeed(connection, feed2Name);

        assertThat(panel.isFeedSelected(connection, feed1Name)).isFalse();
        assertThat(panel.isFeedSelected(connection, feed2Name)).isFalse();

        panel.setSelectedFeed(connection, feed1Name);
        assertThat(panel.isFeedSelected(connection, feed1Name)).isTrue();
        assertThat(panel.isFeedSelected(connection, feed2Name)).isFalse();

        panel.setSelectedFeed(connection, feed2Name);
        assertThat(panel.isFeedSelected(connection, feed1Name)).isFalse();
        assertThat(panel.isFeedSelected(connection, feed2Name)).isTrue();
    }

    @Test
    public void removeFeed_removesFeed()
    {
        final String feedName = "feed";

        DataFeedTimelinePanel panel = new DataFeedTimelinePanel((c, n) -> null);

        panel.addFeed(connection, feedName);
        panel.removeFeed(connection, feedName);

        SwingUtilities.invokeLater(() -> assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> panel.showTimeline(connection, feedName)));
    }

    @Test
    public void removeFeed_clearsSelection_whenRemovedFeedIsSelected()
    {
        final String feed1Name = "feed1";
        final String feed2Name = "feed2";

        DataFeedTimelinePanel panel = new DataFeedTimelinePanel((c, n) -> Collections.emptyList());

        panel.addFeed(connection, feed1Name);
        panel.addFeed(connection, feed2Name);

        panel.setSelectedFeed(connection, feed2Name);
        panel.removeFeed(connection, feed2Name);

        assertThat(panel.isFeedSelected(connection, feed1Name)).isFalse();
        assertThat(panel.isFeedSelected(connection, feed2Name)).isFalse();
    }

    @Test
    public void removeFeed_keepsSelection_whenDifferentFeedIsSelected()
    {
        final String feed1Name = "feed1";
        final String feed2Name = "feed2";

        DataFeedTimelinePanel panel = new DataFeedTimelinePanel((c, n) -> Collections.emptyList());

        panel.addFeed(connection, feed1Name);
        panel.addFeed(connection, feed2Name);

        panel.setSelectedFeed(connection, feed2Name);
        panel.removeFeed(connection, feed1Name);

        assertThat(panel.isFeedSelected(connection, feed1Name)).isFalse();
        assertThat(panel.isFeedSelected(connection, feed2Name)).isTrue();
    }

    @Test
    public void setSelected_retrievesCorrectTimeline(@Mocked BiFunction<ITakServerConnection, String, List<TimelineEntry>> timelineRetrievalFunction)
    {
        when(timelineRetrievalFunction.apply(any(), anyString())).thenReturn(Collections.emptyList());

        final String feed1Name = "feed1";
        final String feed2Name = "feed2";

        DataFeedTimelinePanel panel = new DataFeedTimelinePanel(timelineRetrievalFunction);

        panel.addFeed(connection, feed1Name);
        panel.addFeed(connection, feed2Name);

        panel.setSelectedFeed(connection, feed1Name);
        panel.setSelectedFeed(connection, feed2Name);

        SwingTestUtils.waitForEdtEvents();

        verify(timelineRetrievalFunction).apply(any(), matches(feed1Name));
        verify(timelineRetrievalFunction).apply(any(), matches(feed2Name));
    }

    @Test
    public void showTimeline_throwsIllegalStateException_whenFeedHasNotBeenAdded()
    {
        final DataFeedTimelinePanel panel = new DataFeedTimelinePanel((c, n) -> null);

        SwingUtilities.invokeLater(() -> assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> panel.showTimeline(connection, "feed")));
    }
}