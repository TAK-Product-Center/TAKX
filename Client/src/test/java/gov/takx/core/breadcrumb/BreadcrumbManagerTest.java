package gov.takx.core.breadcrumb;

import gov.tak.api.maps.Marker;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.breadcrumb.event.TrackBreadcrumbsUpdated;
import gov.takx.api.breadcrumb.event.TrackCreated;
import gov.takx.api.breadcrumb.event.TrackDeleted;
import gov.takx.api.breadcrumb.event.TrackUpdated;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbAdded;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbUpdated;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbsRemoved;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbsUpdated;
import gov.takx.api.breadcrumb.event.TrailColorUpdated;
import gov.takx.api.breadcrumb.event.TrailDeleted;
import gov.takx.api.breadcrumb.event.TrailVisibilityChanged;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.core.persist.breadcrumb.Breadcrumb;
import gov.takx.core.persist.breadcrumb.BreadcrumbPersist;
import gov.takx.core.persist.breadcrumb.TrackPersist;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.AIntegrationTest;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.beans.PropertyChangeEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
public class BreadcrumbManagerTest extends AIntegrationTest
{
    private static final String markerUid = "markerUid";

    @Mocked
    private TrackPersist trackPersist;
    @Mocked
    private BreadcrumbPersist breadcrumbPersist;
    @Mocked
    private IMapObjectFinder mapObjectFinder;
    @Mocked(allowCaching = false)
    private Event<TrackCreated> trackCreatedEvent;
    @Mocked(allowCaching = false)
    private Event<TrackBreadcrumbsUpdated> trackBreadcrumbsUpdatedEvent;
    @Mocked(allowCaching = false)
    private Event<TrackUpdated> trackUpdatedEvent;
    @Mocked(allowCaching = false)
    private Event<TrackDeleted> trackDeletedEvent;
    @Mocked(allowCaching = false)
    private Event<TrailBreadcrumbAdded> trailBreadcrumbAddedEvent;
    @Mocked(allowCaching = false)
    private Event<TrailBreadcrumbUpdated> trailBreadcrumbUpdatedEvent;
    @Mocked(allowCaching = false)
    private Event<TrailBreadcrumbsRemoved> trailBreadcrumbRemovedEvent;
    @Mocked(allowCaching = false)
    private Event<TrailBreadcrumbsUpdated> trailChangedEvent;
    @Mocked(allowCaching = false)
    private Event<TrailVisibilityChanged> trailVisibilityChangedEvent;
    @Mocked(allowCaching = false)
    private Event<TrailColorUpdated> trailUpdatedEvent;
    @Mocked(allowCaching = false)
    private Event<TrailDeleted> trailDeletedEvent;
    @Mocked(allowCaching = false)
    private Marker marker;
    @Mocked
    private ServerTrackQueryService serverTrackQueryService;

    private BreadcrumbManager breadcrumbManager;

    @BeforeEach
    void beforeEach()
    {
        clearInvocations(trackPersist, breadcrumbPersist, trackBreadcrumbsUpdatedEvent, trackCreatedEvent, trackUpdatedEvent,
                trackDeletedEvent, trailBreadcrumbAddedEvent, trailBreadcrumbUpdatedEvent, trailVisibilityChangedEvent,
                trailUpdatedEvent, trailBreadcrumbRemovedEvent, mapObjectFinder);

        when(marker.getUid()).thenReturn(markerUid);
        when(marker.isTransient()).thenReturn(false);
        when(mapObjectFinder.findMapObjectByUid(markerUid)).thenReturn(marker);

        breadcrumbManager = new BreadcrumbManager(trackPersist, breadcrumbPersist, trackBreadcrumbsUpdatedEvent,
                trackCreatedEvent, trackDeletedEvent, trackUpdatedEvent, trailBreadcrumbAddedEvent,
                trailBreadcrumbUpdatedEvent, trailBreadcrumbRemovedEvent, trailChangedEvent, trailVisibilityChangedEvent,
                trailUpdatedEvent, trailDeletedEvent, mapObjectFinder, serverTrackQueryService, true);
    }

    @Test
    public void whenSave_trackPersistedAndEventFired()
    {
        // Create and retrieve a track
        breadcrumbManager.startBreadcrumbs(markerUid, null);
        final var track = breadcrumbManager.getCurrentTrack(markerUid);
        assertThat(track).isNotNull();

        clearInvocations(trackPersist);

        breadcrumbManager.save(track);

        verify(trackPersist).persist(track);
        verify(trackUpdatedEvent).fire(any());
    }

    @Test
    public void whenStartNewTrack_trackCreatedAndEventFired()
    {
        breadcrumbManager.startNewTrack(markerUid, null, null);
        breadcrumbManager.startNewTrack(markerUid, null, null);
        breadcrumbManager.startNewTrack(markerUid, null, null);

        verify(trackPersist, times(3)).persist(any());
        verify(trackCreatedEvent, times(3)).fire(any());

        assertThat(breadcrumbManager.getTrackCount(markerUid)).isEqualTo(3);
    }

    @Test
    public void whenStartBreadcrumbs_andNoTrackExists_newTrackCreated()
    {
        // This should trigger the creation of a new track if no track exists
        breadcrumbManager.startBreadcrumbs(markerUid, null);

        verify(trackPersist).persist(any());
        verify(trackCreatedEvent).fire(any());

        assertThat(breadcrumbManager.getTrackCount(markerUid)).isEqualTo(1);
    }

    @Test
    public void whenStartBreadcrumbs_andCurrentTrackExists_noTrackCreated()
    {
        breadcrumbManager.startNewTrack(markerUid, null, null);
        breadcrumbManager.stopBreadcrumbs(markerUid);

        clearInvocations(trackPersist, trackCreatedEvent);

        // This should *not* trigger the creation of a new track if a current track exists
        breadcrumbManager.startBreadcrumbs(markerUid, null);

        verify(trackPersist, never()).persist(any());
        verify(trackCreatedEvent, never()).fire(any());

        assertThat(breadcrumbManager.getTrackCount(markerUid)).isEqualTo(1);
    }

    @Test
    public void whenLocationPropertyChange_breadcrumbCreated()
    {
        final GeoPointMetaData geoPointMetaData = GeoPointMetaData.wrap(GeoPoint.ZERO_POINT);
        when(marker.getGeoPointMetaData()).thenReturn(geoPointMetaData);

        assertThat(breadcrumbManager.getTracks(markerUid)).isEmpty();

        breadcrumbManager.startBreadcrumbs(markerUid, null);
        breadcrumbManager.propertyChange(new PropertyChangeEvent(marker, IMapObject.LOCATION, null, geoPointMetaData.get()));

        final var argumentCaptor = ArgumentCaptor.forClass(Breadcrumb.class);

        verify(breadcrumbPersist).persist(argumentCaptor.capture());
        verify(trackPersist, times(2)).persist(any());
        verify(trackBreadcrumbsUpdatedEvent).fire(any());

        final ITrack track = breadcrumbManager.getCurrentTrack(markerUid);
        assertThat(track).isNotNull();
        final String trackUid = track.getUid();

        final Breadcrumb breadcrumb = argumentCaptor.getValue();
        assertThat(breadcrumb.getGeoPointMetaData().get()).isEqualTo(geoPointMetaData.get());
        assertThat(breadcrumb.getTrackUid()).isEqualTo(trackUid);
    }

    @Test
    public void getTrackBreadcrumbs_returnsCorrectResults()
    {
        breadcrumbManager.startNewTrack(markerUid, null, null);

        // Create 3 breadcrumbs
        for (int i = 0; i < 3; i++) generateBreadcrumb(i);

        // Capture the breadcrumbs passed to the breadcrumb persist mock
        final var argumentCaptor = ArgumentCaptor.forClass(Breadcrumb.class);

        // 5 invocations: 3 new breadcrumbs and 2 updated breadcrumbs
        verify(breadcrumbPersist, times(5)).persist(argumentCaptor.capture());

        final var track = breadcrumbManager.getCurrentTrack(markerUid);
        assertThat(track).isNotNull();

        // Configure the breadcrumb persist mock to return the breadcrumbs that were passed to the persist mock
        when(breadcrumbPersist.getBreadcrumbs(track.getUid()))
                .thenReturn(argumentCaptor.getAllValues().stream().map(b -> (IBreadcrumb) b).toList());

        // Confirm count
        final var breadcrumbsByTrack = breadcrumbManager.getTrackBreadcrumbs(track);
        assertThat(breadcrumbsByTrack).isNotNull();
        assertThat(breadcrumbsByTrack.size()).isEqualTo(3);
    }

    @Test
    public void delete_deletesTrack()
    {
        breadcrumbManager.startNewTrack(markerUid, null, null);
        assertThat(breadcrumbManager.getTrackCount(markerUid)).isEqualTo(1);

        final ITrack track = breadcrumbManager.getCurrentTrack(markerUid);
        assertThat(track).isNotNull();

        breadcrumbManager.stopBreadcrumbs(markerUid);
        breadcrumbManager.deleteTrack(track);

        verify(trackPersist).delete(track);
        assertThat(breadcrumbManager.getTrackCount(markerUid)).isEqualTo(0);
        assertThat(breadcrumbManager.areBreadcrumbsEnabled(markerUid)).isFalse();
        assertThat(breadcrumbManager.getCurrentTrack(markerUid)).isNull();
    }

    @Test
    public void delete_whenRecordingBreadcrumbs_deletesTrackAndCreatesNewTrack()
    {
        breadcrumbManager.startNewTrack(markerUid, null, null);
        assertThat(breadcrumbManager.getTrackCount(markerUid)).isEqualTo(1);

        final ITrack track = breadcrumbManager.getCurrentTrack(markerUid);
        assertThat(track).isNotNull();

        breadcrumbManager.deleteTrack(track);

        verify(trackPersist).delete(track);
        assertThat(breadcrumbManager.getTrackCount(markerUid)).isEqualTo(1);
        assertThat(breadcrumbManager.areBreadcrumbsEnabled(markerUid)).isTrue();
        assertThat(breadcrumbManager.getCurrentTrack(markerUid)).isNotNull();
    }

    @Test
    public void maxBreadcrumbs_whenBreadcrumbMaxLowered_breadcrumbCountReduced()
    {
        final var originalMax = PreferenceEntry.BREADCRUMBS_MAX_BREADCRUMBS.getIntegerValue();

        int latitude = 0;
        int max = 4;

        PreferenceEntry.BREADCRUMBS_MAX_BREADCRUMBS.putAndSave(max);

        breadcrumbManager.startBreadcrumbs(markerUid, null);

        // Generate more location events than the max allowable breadcrumbs and confirm the max is respected
        for (int i = 0; i < max + 1; i++) generateBreadcrumb(latitude++);
        assertThat(breadcrumbManager.getTrailBreadcrumbs(markerUid)).hasSize(max);

        // Lower the max
        PreferenceEntry.BREADCRUMBS_MAX_BREADCRUMBS.putAndSave(max = 2);

        // Generate one additional location event and confirm the max is respected
        generateBreadcrumb(latitude);
        assertThat(breadcrumbManager.getTrailBreadcrumbs(markerUid)).hasSize(max);

        PreferenceEntry.BREADCRUMBS_MAX_BREADCRUMBS.putAndSave(originalMax);
    }

    @Test
    public void onServicesStarted_whenSelfMarkerAdded_noTrackCreated(@Mocked(allowCaching = false) Marker selfMarker)
    {
        final var uid = "self";

        when(selfMarker.getUid()).thenReturn(uid);
        when(selfMarker.isSelfMarker()).thenReturn(true);
        when(selfMarker.hasLocation()).thenReturn(false);
        when(mapObjectFinder.findMapObjectByUid(uid)).thenReturn(selfMarker);

        breadcrumbManager.onMapObjectAdded(new MapObjectAdded(selfMarker));

        // No track should be created if the self marker does not have a location
        verify(trackCreatedEvent, never()).fire(any());
    }

    @Test
    public void miscOperations_whenMarkerTransient_noPersistsOccur()
    {
        when(marker.isTransient()).thenReturn(true);

        // Start trail and create breadcrumb
        final GeoPointMetaData geoPointMetaData = GeoPointMetaData.wrap(GeoPoint.ZERO_POINT);
        when(marker.getGeoPointMetaData()).thenReturn(geoPointMetaData);
        breadcrumbManager.startBreadcrumbs(markerUid, null);
        breadcrumbManager.propertyChange(new PropertyChangeEvent(marker, IMapObject.LOCATION, null, geoPointMetaData.get()));

        // Events should still fire
        verify(trackBreadcrumbsUpdatedEvent).fire(any());

        // Trail should exist with one breadcrumb
        final var trailBreadcrumbs = breadcrumbManager.getTrailBreadcrumbs(markerUid);
        assertThat(trailBreadcrumbs).isNotNull();
        assertThat(trailBreadcrumbs).hasSize(1);

        // Track should exist with one breadcrumb
        final var track = breadcrumbManager.getCurrentTrack(markerUid);
        assertThat(track).isNotNull();
        final var breadcrumbs = breadcrumbManager.getTrackBreadcrumbs(track);
        assertThat(breadcrumbs).hasSize(1);

        breadcrumbManager.stopBreadcrumbs(markerUid);
        breadcrumbManager.deleteTrack(track);

        // No persist calls should have been made
        verify(breadcrumbPersist, never()).persist(any());
        verify(trackPersist, never()).persist(any());
    }

    private void generateBreadcrumb(int latitude)
    {
        when(marker.getGeoPointMetaData()).thenReturn(GeoPointMetaData.wrap(new GeoPoint(latitude, 0)));
        breadcrumbManager.propertyChange(new PropertyChangeEvent(marker, IMapObject.LOCATION, null, null));
    }
}
