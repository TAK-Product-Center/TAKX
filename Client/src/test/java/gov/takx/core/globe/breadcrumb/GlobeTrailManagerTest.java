package gov.takx.core.globe.breadcrumb;

import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbAdded;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbUpdated;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbsRemoved;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbsUpdated;
import gov.takx.api.breadcrumb.event.TrailColorUpdated;
import gov.takx.api.breadcrumb.event.TrailDeleted;
import gov.takx.api.breadcrumb.event.TrailVisibilityChanged;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@link GlobeTrailManager}.
 *
 * @since 5.3
 */
@PseudoBean
public class GlobeTrailManagerTest extends AContainerTest
{
    @Mocked
    private FeatureDataStore4 featureDataStore;
    @Mocked
    private IBreadcrumbManager breadcrumbManager;

    @Inject
    private GlobeTrailManager globeTrailManager;

    @BeforeAll
    void beforeAll()
    {
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(globeTrailManager));
    }

    @BeforeEach
    void beforeEach() throws DataStoreException
    {
        globeTrailManager.onProjectClosed(null);
        clearInvocations(featureDataStore);

        // anything besides 0, which we use to indicate a FSID value is uninitialized
        when(featureDataStore.insertFeatureSet(any())).thenReturn(1L);
    }

    @Test
    public void trailBreadcrumbAdded_createsFeatureSetAndFeatures(@Mocked(allowCaching = false) IBreadcrumb breadcrumb1,
                                                                  @Mocked(allowCaching = false) IBreadcrumb breadcrumb2)
            throws DataStoreException
    {
        initializeBreadcrumbs(breadcrumb1, breadcrumb2);

        globeTrailManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded("markerUid", breadcrumb1, 0));
        globeTrailManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded("markerUid", breadcrumb2, 0));

        verify(featureDataStore).insertFeatureSet(any());
        verify(featureDataStore, times(2)).insertFeature(any());
    }

    @Test
    public void trailBreadcrumbUpdated_updatesFeature(@Mocked IBreadcrumb breadcrumb)
            throws DataStoreException
    {
        initializeBreadcrumbs(breadcrumb);

        globeTrailManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded("markerUid", breadcrumb, 0));
        globeTrailManager.trailBreadcrumbUpdated(new TrailBreadcrumbUpdated("markerUid", breadcrumb));

        verify(featureDataStore).updateFeature(anyLong(), anyInt(), anyString(), any(), any(), any(), anyInt());
    }

    @Test
    public void trailBreadcrumbRemoved_deletesFeature(@Mocked IBreadcrumb breadcrumb)
            throws DataStoreException
    {
        initializeBreadcrumbs(breadcrumb);

        globeTrailManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded("markerUid", breadcrumb, 0));
        globeTrailManager.trailBreadcrumbsRemoved(new TrailBreadcrumbsRemoved("markerUid", List.of(breadcrumb)));

        verify(featureDataStore).deleteFeature(anyLong());
    }

    @Test
    public void trailDeleted_deletesFeatureSet(@Mocked IBreadcrumb breadcrumb) throws DataStoreException
    {
        initializeBreadcrumbs(breadcrumb);

        globeTrailManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded("markerUid", breadcrumb, 0));
        globeTrailManager.trailDeleted(new TrailDeleted("markerUid"));

        verify(featureDataStore).deleteFeatureSet(anyLong());
    }

    @Test
    public void trailColorUpdated_updatesFeature(@Mocked IBreadcrumb breadcrumb, @Mocked FeatureCursor featureCursor)
            throws DataStoreException
    {
        final var markerUid = "markerUid";
        initializeBreadcrumbs(breadcrumb);
        when(breadcrumbManager.getTrailBreadcrumbs(markerUid)).thenReturn(List.of(breadcrumb));
        when(featureDataStore.queryFeatures(any())).thenReturn(featureCursor);
        when(featureCursor.moveToNext()).thenReturn(true, false);
        when(featureCursor.get()).thenReturn(new Feature(1, "name", null, // can't mock Feature b/c it's final
                new CompositeStyle(new Style[]{new IconPointStyle(0, "")}), null));

        globeTrailManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded(markerUid, breadcrumb, 0));

        clearInvocations(featureDataStore);

        globeTrailManager.trailColorUpdated(new TrailColorUpdated(markerUid, 0));

        verify(featureDataStore).updateFeature(anyLong(),
                intThat(propMask -> propMask == FeatureDataStore2.PROPERTY_FEATURE_STYLE), anyString(),
                isNull(), any(), isNull(), intThat(attrUpdateType -> attrUpdateType == 0));
    }

    @Test
    public void trailVisibilityChanged_setFeatureSetVisibility(@Mocked IBreadcrumb breadcrumb)
            throws DataStoreException
    {
        initializeBreadcrumbs(breadcrumb);

        globeTrailManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded("markerUid", breadcrumb, 0));
        globeTrailManager.trailVisibilityChanged(new TrailVisibilityChanged("markerUid", false));

        verify(featureDataStore).setFeatureSetVisible(anyLong(), booleanThat(visible -> !visible));
    }

    @Test
    public void trailChanged_queriesDataStoreAndUpdatesFeature(@Mocked(allowCaching = false) IBreadcrumb breadcrumb1,
                                                               @Mocked(allowCaching = false) IBreadcrumb breadcrumb2,
                                                               @Mocked(allowCaching = false) IBreadcrumb breadcrumb3,
                                                               @Mocked FeatureCursor featureCursor)
            throws DataStoreException
    {
        final var originalMaxBreadcrumbs = PreferenceEntry.BREADCRUMBS_MAX_BREADCRUMBS.getIntegerValue();
        PreferenceEntry.BREADCRUMBS_MAX_BREADCRUMBS.putAndSave(3);

        when(featureDataStore.queryFeatures(any())).thenReturn(featureCursor);
        when(featureCursor.moveToNext()).thenReturn(true, true, true, false);
        when(featureCursor.get()).thenReturn(new Feature(1, "name", null, // can't mock Feature b/c it's final
                new CompositeStyle(new Style[]{new IconPointStyle(0, "")}), null));

        initializeBreadcrumbs(breadcrumb1, breadcrumb2, breadcrumb3);

        globeTrailManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded("markerUid", breadcrumb1, 0));
        globeTrailManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded("markerUid", breadcrumb2, 0));
        globeTrailManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded("markerUid", breadcrumb3, 0));

        globeTrailManager.trailBreadcrumbsUpdated(new TrailBreadcrumbsUpdated("markerUid"));

        verify(featureDataStore).queryFeatures(any());
        verify(featureDataStore, times(3))
                .updateFeature(anyLong(), intThat(i -> i == FeatureDataStore2.PROPERTY_FEATURE_STYLE), anyString(),
                        isNull(), any(), isNull(), anyInt());

        PreferenceEntry.BREADCRUMBS_MAX_BREADCRUMBS.putAndSave(originalMaxBreadcrumbs);
    }

    private void initializeBreadcrumbs(@Nonnull IBreadcrumb... breadcrumbs)
    {
        final var breadcrumbList = List.of(breadcrumbs);
        int breadcrumbUid = 0;

        for (var breadcrumb : breadcrumbList) when(breadcrumb.getUid()).thenReturn("uid" + breadcrumbUid++);
    }
}
