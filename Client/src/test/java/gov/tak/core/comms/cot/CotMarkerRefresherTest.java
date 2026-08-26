package gov.tak.core.comms.cot;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.comms.roles.RolesManager;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@link CotMarkerRefresher}.
 *
 * @since 5.1
 */
public class CotMarkerRefresherTest extends AContainerTest
{
    private static final int AUTO_STALE_DURATION_SEC = 10;
    private static final String TEAM = "Red";

    private CotMarkerRefresher cotMarkerRefresher;
    private RolesManager rolesManager;
    private AttributeSet attributeSet;

    @Mocked
    private Marker marker;

    @BeforeAll
    public void setup()
    {
        cotMarkerRefresher = getBean(CotMarkerRefresher.class);
        rolesManager = getBean(RolesManager.class);
        startService(RolesManager.class);
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(cotMarkerRefresher));
    }

    @BeforeEach
    public void beforeEach()
    {
        // Clear state and initialize
        clearInvocations(marker);
        attributeSet = new AttributeSet();
        prepareMarker(marker, attributeSet);

        startService(CotMarkerRefresher.class);
        cotMarkerRefresher.onClientMapObjectAdded(new MapObjectAdded(marker));
    }

    @AfterEach
    public void afterEach()
    {
        cotMarkerRefresher.onClientMapObjectRemoved(new MapObjectRemoved(marker));
        stopService(CotMarkerRefresher.class);
    }

    @Test
    public void forceStale_makesMarkerStaleAndUpdatesIcon()
    {
        attributeSet.setAttribute(StandardAttributeNames.FORCE_STALE, true);

        cotMarkerRefresher.checkMarkers(); // force check

        assertThat(attributeSet.containsAttribute(StandardAttributeNames.FORCE_STALE)).isFalse(); // flag should be cleared
        assertThat(attributeSet.getBooleanAttribute(StandardAttributeNames.STALE)).isTrue(); // marker should be marked stale
        verify(marker).setExternalIconImage(any()); // icon should be updated
    }

    @Test
    public void lastUpdateTime_whenOlderThatAutoStaleDuration_makesMarkerStale()
    {
        // Set last update time to time in past that is 5 seconds longer than the auto stale duration and run check
        attributeSet.setAttribute(StandardAttributeNames.LAST_UPDATE_TIME,
                CoordinatedTime.currentTimeMillis() - TimeUnit.SECONDS.toMillis(AUTO_STALE_DURATION_SEC + 5));

        cotMarkerRefresher.checkMarkers(); // force check

        // Marker should be set to stale and icon should change
        assertThat(attributeSet.getBooleanAttribute(StandardAttributeNames.STALE)).isTrue();

        // Set last update time to current time and run check
        attributeSet.setAttribute(StandardAttributeNames.LAST_UPDATE_TIME, CoordinatedTime.currentTimeMillis());

        cotMarkerRefresher.checkMarkers(); // force check

        // Marker should revert to non-stale state and icon should be restored
        assertThat(attributeSet.getBooleanAttribute(StandardAttributeNames.STALE)).isFalse();

        // setExternalIconImage should be called twice, when set to stale and when set back to active
        verify(marker, times(2)).setExternalIconImage(any());
    }

    @Test
    public void lastUpdateTime_whenNewerThatAutoStaleDuration_doesNotMakeMarkerStale()
    {
        // Set last update time to time in past that is only 1/2 the auto stale duration and run check
        attributeSet.setAttribute(StandardAttributeNames.LAST_UPDATE_TIME,
                CoordinatedTime.currentTimeMillis() - TimeUnit.SECONDS.toMillis(AUTO_STALE_DURATION_SEC / 2));

        cotMarkerRefresher.checkMarkers(); // force check

        // Marker should not be marked stale and icon should not change
        assertThat(attributeSet.getBooleanAttribute(StandardAttributeNames.STALE)).isFalse();
        verify(marker, times(0)).setExternalIconImage(any());
    }

    private void prepareMarker(Marker marker, AttributeSet attributeSet)
    {
        when(marker.getFamily()).thenReturn(IMarkerMapObject.MARKER_FAMILY);
        when(marker.getType()).thenReturn(IMarkerMapObject.MARKER_TYPE);
        when(marker.getCotType()).thenReturn(CotTypes.UNKNOWN_GROUND);
        when(marker.getUid()).thenReturn(UUID.randomUUID().toString());

        when(marker.getMetaString(anyString(), anyString()))
                .thenAnswer(invocation -> attributeSet.getStringAttribute(invocation.getArgument(0), invocation.getArgument(1)));
        when(marker.getMetaBoolean(anyString(), anyBoolean()))
                .thenAnswer(invocation -> attributeSet.getBooleanAttribute(invocation.getArgument(0), invocation.getArgument(1)));
        when(marker.hasMetaValue(anyString())).thenAnswer(invocation -> attributeSet.containsAttribute(invocation.getArgument(0)));

        attributeSet.setAttribute(StandardAttributeNames.STALE, false);
        attributeSet.setAttribute(StandardAttributeNames.TEAM, TEAM);
        attributeSet.setAttribute(StandardAttributeNames.ADAPT_MARKER_ICON, true);
        //noinspection DataFlowIssue - cannot be null as long as the rolesManager has been initialized
        attributeSet.setAttribute(StandardAttributeNames.ATAK_ROLE_TYPE, rolesManager.getRole("Forward Observer", "FO").getName());
        attributeSet.setAttribute(StandardAttributeNames.LAST_UPDATE_TIME, CoordinatedTime.currentTimeMillis());
        attributeSet.setAttribute(StandardAttributeNames.AUTO_STALE_DURATION_MS, TimeUnit.SECONDS.toMillis(AUTO_STALE_DURATION_SEC));
        when(marker.getAttributeSet()).thenReturn(attributeSet);
    }
}
