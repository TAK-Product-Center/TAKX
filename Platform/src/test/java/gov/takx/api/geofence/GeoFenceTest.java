package gov.takx.api.geofence;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @since 5.3
 */
public class GeoFenceTest
{
    public static final String SHAPE_UID = "shapeUid";
    public static final Set<String> CUSTOM_UIDS = Set.of("uid1", "uid2", "uid3", "uid4");

    @Test
    void builderDefaults_asExpected()
    {
        final GeoFence geoFence = GeoFence.builder().shapeUid(SHAPE_UID).build();
        assertThat(geoFence).isNotNull();
        assertThat(geoFence.shapeUid()).isEqualTo(SHAPE_UID);
        assertThat(geoFence.trigger()).isEqualTo(Trigger.Entry);
        assertThat(geoFence.monitoredType()).isEqualTo(MonitoredType.All);
        assertThat(geoFence.tracking()).isTrue();
        assertThat(geoFence.minElevation()).isNaN();
        assertThat(geoFence.maxElevation()).isNaN();
        assertThat(geoFence.customUids()).isEmpty();
    }

    @Test
    void builderSetValues_asSpecified()
    {
        final GeoFence geoFence = makeGeoFenceWithAllValuesSet();

        assertThat(geoFence).isNotNull();
        assertThat(geoFence.shapeUid()).isEqualTo(SHAPE_UID);
        assertThat(geoFence.trigger()).isEqualTo(Trigger.Both);
        assertThat(geoFence.monitoredType()).isEqualTo(MonitoredType.Custom);
        assertThat(geoFence.tracking()).isFalse();
        assertThat(geoFence.minElevation()).isEqualTo(10);
        assertThat(geoFence.maxElevation()).isEqualTo(100);
        assertThat(geoFence.customUids()).isEqualTo(CUSTOM_UIDS);
    }

    @Test
    void builderFromSourceGeoFence_hasSameValues()
    {
        final GeoFence geoFence = GeoFence.builder(makeGeoFenceWithAllValuesSet()).build();

        assertThat(geoFence).isNotNull();
        assertThat(geoFence.shapeUid()).isEqualTo(SHAPE_UID);
        assertThat(geoFence.trigger()).isEqualTo(Trigger.Both);
        assertThat(geoFence.monitoredType()).isEqualTo(MonitoredType.Custom);
        assertThat(geoFence.tracking()).isFalse();
        assertThat(geoFence.minElevation()).isEqualTo(10);
        assertThat(geoFence.maxElevation()).isEqualTo(100);
        assertThat(geoFence.customUids()).isEqualTo(CUSTOM_UIDS);
    }

    @Test
    void builderFromSourceWithOverrides_hasSetValues()
    {
        final GeoFence geoFence = GeoFence.builder(makeGeoFenceWithAllValuesSet())
                .trigger(Trigger.Entry)
                .monitoredType(MonitoredType.TAKUsers)
                .tracking(true)
                .build();

        assertThat(geoFence).isNotNull();
        assertThat(geoFence.trigger()).isEqualTo(Trigger.Entry);
        assertThat(geoFence.monitoredType()).isEqualTo(MonitoredType.TAKUsers);
        assertThat(geoFence.tracking()).isTrue();
    }

    @Test
    void elevationMixedNaN_isSupported()
    {
        GeoFence geoFence = GeoFence.builder().shapeUid(SHAPE_UID).minElevation(100).build();
        assertThat(geoFence.minElevation()).isEqualTo(100);
        assertThat(geoFence.maxElevation()).isNaN();

        geoFence = GeoFence.builder().shapeUid(SHAPE_UID).maxElevation(100).build();
        assertThat(geoFence.minElevation()).isNaN();
        assertThat(geoFence.maxElevation()).isEqualTo(100);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void customUids_cannotBeModified()
    {
        final GeoFence geoFence = GeoFence.builder()
                .shapeUid(SHAPE_UID)
                .monitoredType(MonitoredType.Custom)
                .customUids(CUSTOM_UIDS)
                .build();

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> geoFence.customUids().clear());
    }

    @Test
    void noShapeUid_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> GeoFence.builder().build())
                .withMessageContaining("shapeUid");
    }

    @Test
    void minGreaterThanMaxElevation_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> GeoFence.builder().shapeUid(SHAPE_UID).elevationRange(100, 10).build())
                .withMessageContaining("minElevation");
    }

    private GeoFence makeGeoFenceWithAllValuesSet()
    {
        return GeoFence.builder()
                .shapeUid(SHAPE_UID)
                .trigger(Trigger.Both)
                .monitoredType(MonitoredType.Custom)
                .tracking(false)
                .elevationRange(10, 100)
                .customUids(CUSTOM_UIDS)
                .build();
    }
}
