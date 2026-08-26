package gov.takx.core.globe.breadcrumb;

import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbAdded;
import gov.takx.api.breadcrumb.event.TrailVisibilityChanged;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
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
 * Tests {@link GlobePersistentPathManager}
 * @since 5.5
 */
@PseudoBean
public class GlobePersistentPathManagerTest extends AContainerTest
{
    @Mocked
    private FeatureDataStore4 featureDataStore;
    @Mocked
    private IMapObject selfMarker;

    @Inject
    private GlobePersistentPathManager globePersistentPathManager;

    @BeforeAll
    void beforeAll()
    {
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(globePersistentPathManager));
    }

    @BeforeEach
    void beforeEach()
    {
        globePersistentPathManager.onProjectClosed(null);
        clearInvocations(featureDataStore);

        when(selfMarker.isVisible()).thenReturn(true);
        when(selfMarker.isSelfMarker()).thenReturn(true);
        when(selfMarker.getUid()).thenReturn("uid");
    }

    @Test
    public void trailBreadcrumbAdded_whenNotEnabled_skipsFeatureUpdate(@Mocked(allowCaching = false) IBreadcrumb breadcrumb)
            throws DataStoreException
    {
        final var markerUid = selfMarker.getUid();

        globePersistentPathManager.mapObjectAdded(new MapObjectAdded(selfMarker));
        globePersistentPathManager.trailVisibilityChanged(new TrailVisibilityChanged(markerUid, true));

        var event = new TrailBreadcrumbAdded(markerUid, breadcrumb, 0);
        globePersistentPathManager.trailBreadcrumbAdded(event);
        verifyUpdateFeatureCalls(0); // confirm no feature update occurred
    }

    @Test
    public void trailBreadcrumbAdded_whenNotVisible_skipsFeatureUpdate(@Mocked(allowCaching = false) IBreadcrumb breadcrumb1)
            throws DataStoreException
    {
        final var markerUid = selfMarker.getUid();

        globePersistentPathManager.mapObjectAdded(new MapObjectAdded(selfMarker));
        globePersistentPathManager.setEnabled(true);

        var event = new TrailBreadcrumbAdded(markerUid, breadcrumb1, 0);
        globePersistentPathManager.trailBreadcrumbAdded(event);
        verifyUpdateFeatureCalls(0);
    }

    @Test
    public void trailBreadcrumbAdded_whenEnabledAndVisible_updatesFeature(@Mocked(allowCaching = false) IBreadcrumb breadcrumb)
            throws DataStoreException
    {
        final var markerUid = selfMarker.getUid();

        // Initialize with self marker, enable persistent path and set trail visible
        globePersistentPathManager.mapObjectAdded(new MapObjectAdded(selfMarker));
        globePersistentPathManager.setEnabled(true);
        globePersistentPathManager.trailVisibilityChanged(new TrailVisibilityChanged(markerUid, true));

        clearInvocations(featureDataStore);

        final int count = 5;

        for (int i = 0; i < count; i++)
        {
            globePersistentPathManager.trailBreadcrumbAdded(new TrailBreadcrumbAdded(markerUid, breadcrumb, 0));
        }

        verifyUpdateFeatureCalls(count);
    }

    @Test
    public void trailVisibilityChanged_updatesFeatureSetVisibility() throws DataStoreException
    {
        final var markerUid = selfMarker.getUid();

        // Initialize with self marker and enable persistent path
        globePersistentPathManager.mapObjectAdded(new MapObjectAdded(selfMarker));
        globePersistentPathManager.setEnabled(true);

        clearInvocations(featureDataStore);

        globePersistentPathManager.trailVisibilityChanged(new TrailVisibilityChanged(markerUid, true));
        verify(featureDataStore).setFeatureSetVisible(anyLong(), booleanThat(visible -> visible));

        globePersistentPathManager.trailVisibilityChanged(new TrailVisibilityChanged(markerUid, false));
        verify(featureDataStore).setFeatureSetVisible(anyLong(), booleanThat(visible -> !visible));
    }

    @Test
    public void setEnabled_updatesFeatureSetVisibility() throws DataStoreException
    {
        final var markerUid = selfMarker.getUid();

        // Initialize with self marker and set trail visible
        globePersistentPathManager.mapObjectAdded(new MapObjectAdded(selfMarker));
        globePersistentPathManager.trailVisibilityChanged(new TrailVisibilityChanged(markerUid, true));

        clearInvocations(featureDataStore);

        globePersistentPathManager.setEnabled(true);
        verify(featureDataStore).setFeatureSetVisible(anyLong(), booleanThat(visible -> visible));

        globePersistentPathManager.setEnabled(false);
        verify(featureDataStore).setFeatureSetVisible(anyLong(), booleanThat(visible -> !visible));
    }

    private void verifyUpdateFeatureCalls(int count) throws DataStoreException
    {
        verify(featureDataStore, times(count)).updateFeature(anyLong(),
                intThat(propMask -> propMask == FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY), anyString(),
                any(), isNull(), isNull(), intThat(type -> type == 0));
    }
}
