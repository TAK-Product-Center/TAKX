package gov.takx.core.view.gui.breadcrumb;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.Marker;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.takserver.TakServerConnectionClosed;
import gov.takx.api.event.takserver.TakServerConnectionOpened;
import gov.takx.api.notification.INotificationService;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@link TrackSearchPanelController}.
 *
 * @since 5.4
 */
@PseudoBean
public class TrackSearchPanelControllerTest extends AContainerTest
{
    @Mocked
    private IBreadcrumbManager breadcrumbManager;
    @Mocked
    private INotificationService notificationService;
    private TrackSearchPanelController controller;
    private TrackSearchPanel panel;

    @BeforeEach
    void beforeEach()
    {
        controller = new TrackSearchPanelController(breadcrumbManager, notificationService);
        panel = (TrackSearchPanel) controller.getPanel();
    }

    @Test
    public void takServerConnectionChanges_updatePanelModel(@Mocked(allowCaching = false) ITakServerConnection connection1,
                                                            @Mocked(allowCaching = false) ITakServerConnection connection2)
    {
        initTakServerConnections(connection1, connection2);

        controller.takServerConnectionOpened(new TakServerConnectionOpened(connection1));
        controller.takServerConnectionOpened(new TakServerConnectionOpened(connection2));
        SwingTestUtils.waitForEdtEvents();

        assertThat(panel.serverModel.getSize()).isEqualTo(2);

        controller.takServerConnectionClosed(new TakServerConnectionClosed(connection1));

        assertThat(panel.serverModel.getSize()).isEqualTo(1);
    }

    @Test
    public void onMapObjectAdded_updatesPanelAndConfiguresDeletes(@Mocked(allowCaching = false) Marker marker1,
                                                                  @Mocked(allowCaching = false) Marker marker2)
    {
        initMarkers(marker1, marker2);

        controller.onMapObjectAdded(new MapObjectAdded(marker1), false);
        controller.onMapObjectAdded(new MapObjectAdded(marker2), false);

        //noinspection unchecked
        final ArgumentCaptor<Consumer<MapItem>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(marker1).addDeletionHandler(captor.capture());

        SwingTestUtils.waitForEdtEvents();

        assertThat(panel.markers.size()).isEqualTo(2);

        // Use captured listeners to trigger a delete
        captor.getValue().accept(marker1);
        SwingTestUtils.waitForEdtEvents();

        assertThat(panel.markers.size()).isEqualTo(1);
    }

    private void initMarkers(Marker... markers)
    {
        int i = 1;

        for (Marker marker : markers)
        {
            when(marker.getUid()).thenReturn("uid" + i);
            when(marker.getName()).thenReturn("Marker " + i++);
        }
    }

    private void initTakServerConnections(ITakServerConnection... connections)
    {
        int i = 1;

        for (ITakServerConnection connection : connections)
        {
            when(connection.getDisplayName()).thenReturn("server" + i);
            when(connection.isEnabled()).thenReturn(true);
        }
    }
}
