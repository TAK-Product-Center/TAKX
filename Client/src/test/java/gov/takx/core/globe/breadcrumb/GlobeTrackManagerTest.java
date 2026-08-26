package gov.takx.core.globe.breadcrumb;

import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.breadcrumb.event.TrackBreadcrumbsUpdated;
import gov.takx.api.breadcrumb.event.TrackDeleted;
import gov.takx.api.breadcrumb.event.TrackUpdated;
import gov.takx.api.breadcrumb.event.TrackVisibilityChanged;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@link GlobeTrackManager}.
 *
 * @since 5.3
 */
@PseudoBean
public class GlobeTrackManagerTest extends AContainerTest
{
    @Mocked
    private FeatureDataStore4 featureDataStore;
    @Mocked
    private IBreadcrumbManager breadcrumbManager;
    @Mocked
    private IMapObjectFinder mapObjectFinder;
    @Mocked
    private IMapObject mapObject;

    @Inject
    private GlobeTrackManager globeTrackManager;

    @BeforeAll
    void beforeAll()
    {
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(globeTrackManager));
    }

    @BeforeEach
    void beforeEach() throws DataStoreException
    {
        globeTrackManager.onProjectClosed(null);
        clearInvocations(featureDataStore);

        when(mapObject.isVisible()).thenReturn(true);
        when(mapObjectFinder.findMapObjectByUid(anyString())).thenReturn(mapObject);

        // anything besides 0, which we use to indicate a FSID value is uninitialized
        when(featureDataStore.insertFeatureSet(any())).thenReturn(1L);
    }

    @Test
    public void trackVisible_createsFeatures(@Mocked ITrack track,
                                             @Mocked(allowCaching = false) IBreadcrumb breadcrumb1,
                                             @Mocked(allowCaching = false) IBreadcrumb breadcrumb2)
            throws DataStoreException
    {
        initializeTrackAndBreadcrumbs(track, breadcrumb1, breadcrumb2);

        // First time track is visible, it should be initialized
        globeTrackManager.trackBreadcrumbsUpdated(new TrackBreadcrumbsUpdated(track, List.of(breadcrumb1, breadcrumb2)));

        // insert 3 feature sets: endpoints, path, breadcrumbs
        verify(featureDataStore, times(3)).insertFeatureSet(any());

        // insert 5 features: start, end, path, 2 breadcrumbs
        verify(featureDataStore, times(5)).insertFeature(any());
    }

    @Test
    public void trackNotVisible_updatesFeatureSetVisibility(@Mocked ITrack track, @Mocked IBreadcrumb breadcrumb)
            throws DataStoreException
    {
        initializeTrackAndBreadcrumbs(track, breadcrumb);

        // Add the track
        globeTrackManager.trackBreadcrumbsUpdated(new TrackBreadcrumbsUpdated(track, List.of(breadcrumb)));
        clearInvocations(featureDataStore);

        // Set visibility
        when(track.isVisible()).thenReturn(true);
        globeTrackManager.trackVisibilityChanged(new TrackVisibilityChanged(track));

        // 2 feature sets visible: endpoints, breadcrumbs
        verify(featureDataStore, times(2)).setFeatureSetVisible(anyLong(), eq(true));

        // 1 feature set invisible: line
        verify(featureDataStore, times(1)).setFeatureSetVisible(anyLong(), eq(false));
    }

    @Test
    public void trackUpdated_updatesFeatures(@Mocked ITrack track,
                                             @Mocked(allowCaching = false) IBreadcrumb breadcrumb)
            throws DataStoreException
    {
        initializeTrackAndBreadcrumbs(track, breadcrumb);

        // Add the track
        globeTrackManager.trackBreadcrumbsUpdated(new TrackBreadcrumbsUpdated(track, List.of(breadcrumb)));
        clearInvocations(featureDataStore);

        when(track.getStyle()).thenReturn(ITrack.Style.Arrow);

        globeTrackManager.trackUpdated(new TrackUpdated(track));

        // 4 times: breadcrumb, path, start point, end point
        verify(featureDataStore, times(4)).updateFeature(anyLong(),
                intThat(propMask -> propMask == FeatureDataStore2.PROPERTY_FEATURE_STYLE), anyString(),
                isNull(), any(), isNull(), intThat(attrUpdateType -> attrUpdateType == 0));

        // Make the track visible
        when(track.isVisible()).thenReturn(true);
        globeTrackManager.trackVisibilityChanged(new TrackVisibilityChanged(track));

        clearInvocations(featureDataStore);

        when(track.getStyle()).thenReturn(ITrack.Style.Line);

        globeTrackManager.trackUpdated(new TrackUpdated(track));

        // Arrow feature set hidden, path feature set visible
        verify(featureDataStore).setFeatureSetVisible(anyLong(), booleanThat(b -> !b));
        verify(featureDataStore).setFeatureSetVisible(anyLong(), booleanThat(b -> b));
    }

    @Test
    public void trackBreadcrumbUpdated_updatesFeatures(@Mocked ITrack track,
                                                       @Mocked(allowCaching = false) IBreadcrumb breadcrumb1,
                                                       @Mocked(allowCaching = false) IBreadcrumb breadcrumb2)
            throws DataStoreException
    {
        initializeTrackAndBreadcrumbs(track, breadcrumb1, breadcrumb2);

        // Add the track
        globeTrackManager.trackBreadcrumbsUpdated(new TrackBreadcrumbsUpdated(track, List.of(breadcrumb1, breadcrumb2)));
        clearInvocations(featureDataStore);

        globeTrackManager.trackBreadcrumbsUpdated(new TrackBreadcrumbsUpdated(track, List.of(breadcrumb2)));

        // Update path and endpoint features
        verify(featureDataStore, times(1)).updateFeature(anyLong(),
                intThat(propMask -> propMask == FeatureDataStore2.PROPERTY_FEATURE_STYLE), anyString(),
                isNull(), any(), isNull(), intThat(attrUpdateType -> attrUpdateType == 0));
    }

    @Test
    public void trackDeleted_deletesFeatureSets(@Mocked ITrack track, @Mocked IBreadcrumb breadcrumb)
            throws DataStoreException
    {
        initializeTrackAndBreadcrumbs(track, breadcrumb);

        // Add the track
        globeTrackManager.trackBreadcrumbsUpdated(new TrackBreadcrumbsUpdated(track, List.of(breadcrumb)));
        clearInvocations(featureDataStore);

        // Delete the track
        globeTrackManager.trackDeleted(new TrackDeleted(track));

        // delete 3 feature sets: endpoints, path and breadcrumbs
        verify(featureDataStore, times(3)).deleteFeatureSet(anyLong());
    }

    private void initializeTrackAndBreadcrumbs(@Nullable ITrack track, @Nonnull IBreadcrumb... breadcrumbs)
    {
        final var trackUid = "trackUid";
        final var breadcrumbList = List.of(breadcrumbs);
        int breadcrumbUid = 0;

        for (var breadcrumb : breadcrumbList)
        {
            if (track != null) when(breadcrumb.getTrackUid()).thenReturn(trackUid);
            when(breadcrumb.getUid()).thenReturn("uid" + breadcrumbUid++);
        }

        if (track != null)
        {
            when(breadcrumbManager.getTrackBreadcrumbs(track)).thenReturn(breadcrumbList);
            when(track.getUid()).thenReturn(trackUid);
            when(track.getMarkerUid()).thenReturn("markerUid");
            when(track.getBreadcrumbCount()).thenReturn(breadcrumbList.size());
            when(track.isVisible()).thenReturn(true);
        }
    }
}
