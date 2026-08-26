package gov.tak.core.icons;

import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.Role;
import gov.tak.api.icons.IconAdapter;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.comms.roles.RolesManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.dataservices.persist.MarkerMapObject;
import gov.takx.platform.ui.UIResources;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.function.Consumer;

import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link MarkerIconAdapter} functionality.
 *
 * @since 3.0
 */
@PseudoBean
public class MarkerIconAdapterTest extends AIntegrationTest
{
    private final MarkerIconAdapter markerIconAdapter;
    private final RolesManager rolesManager;

    MarkerIconAdapterTest()
    {
        markerIconAdapter = getBean(MarkerIconAdapter.class);
        rolesManager = getBean(RolesManager.class);
        startService(RolesManager.class);
    }

    @AfterEach
    public void cleanup()
    {
        stopService(MarkerIconAdapter.class);
    }

    @Test
    public void test_marker_with_no_cot_type_is_not_adapted()
    {
        assertThat(markerIconAdapter.adapt(makeMarker(CotTypes.EMPTY))).isFalse();
    }

    @Test
    public void test_registered_adapter_is_called()
    {
        final TestAdapter adapter = new TestAdapter();
        markerIconAdapter.addIconAdapter(adapter);

        final Marker marker = makeMarker("a-f");
        assertThat(markerIconAdapter.adapt(marker)).isTrue();
        assertThat(adapter.adaptCalled).isTrue();
        assertThat(marker.getIconImageURI().toString()).endsWith("no_icon.png");
    }

    @Test
    public void test_adapter_is_not_called_after_unregistering()
    {
        final TestAdapter adapter = new TestAdapter();
        markerIconAdapter.addIconAdapter(adapter);
        markerIconAdapter.removeIconAdapter(adapter);

        assertThat(markerIconAdapter.adapt(makeMarker("a-f"))).isTrue();
        assertThat(adapter.adaptCalled).isFalse();
    }

    @Test
    public void test_first_adapter_to_adapt_stops_calls()
    {
        final TestAdapter adapter1 = new TestAdapter();
        final TestAdapter adapter2 = new TestAdapter();
        final TestAdapter adapter3 = new TestAdapter();

        adapter1.shouldAdapt = false;

        markerIconAdapter.addIconAdapter(adapter1);
        markerIconAdapter.addIconAdapter(adapter2);
        markerIconAdapter.addIconAdapter(adapter3);

        assertThat(markerIconAdapter.adapt(makeMarker("a-f"))).isTrue();
        assertThat(adapter1.adaptCalled).isTrue();
        assertThat(adapter2.adaptCalled).isTrue();
        assertThat(adapter3.adaptCalled).isFalse();
    }

    @Test
    public void test_dispose_is_called_on_service_stop()
    {
        final TestAdapter adapter = new TestAdapter();
        markerIconAdapter.addIconAdapter(adapter);

        stopService(MarkerIconAdapter.class);
        assertThat(adapter.disposeCalled).isTrue();
    }

    @Test
    public void test_adapt_by_readiness()
    {
        final Consumer<Marker> setup = marker -> marker.setMetaBoolean(StandardAttributeNames.READINESS, false);

        verifyAdapted("a-f", "damaged.png", setup);
        verifyAdapted("a-n", "damaged_neutral.png", setup);
        verifyAdapted("a-h", "damaged_hostile.png", setup);
        verifyAdapted("a-u", "damaged_unknown.png", setup);
    }

    @Test
    public void test_adapt_by_team()
    {
        verifyAdaptedByTeamRole("unknown role", null, "team");
        verifyAdaptedByTeamRole("unknown role", "unknown_how", "team");
        verifyAdaptedByTeamRole("unknown role", CotConstants.HOW_HUMAN_PREFIX, "team_human");
        verifyAdaptedByTeamRole("unknown role", CotConstants.HOW_GPS_LOST_PREFIX, "team_nogps");

        for (Role role : rolesManager.getRoles().get(0).getChildren())
        {
            final String roleName = role.getName();
            final String baseIconName = role.getName().equalsIgnoreCase("Team Member") ? // team member is unique
                    "team" : roleName.toLowerCase(Locale.ROOT).replace(" ", ""); // Strip spaces on everything else

            verifyAdaptedByTeamRole(roleName, null, baseIconName);
            verifyAdaptedByTeamRole(roleName, "unknown_how", baseIconName);
            verifyAdaptedByTeamRole(roleName, CotConstants.HOW_HUMAN_PREFIX, baseIconName + "_human");
            verifyAdaptedByTeamRole(roleName, CotConstants.HOW_GPS_LOST_PREFIX, baseIconName + "_nogps");
        }
    }

    @Test
    public void test_predefined_icon_adapter()
    {
        // Just test a sampling of types
        verifyAdapted("a-f-G-E-S-t", "ttl-icon-f.png");
        verifyAdapted("b-i-x-i", "camera.png");
        verifyAdapted("b-a-o-tbl", "alarm-trouble.png");
        verifyAdapted("b-m-p-s-p-op", "binos.png");
        verifyAdapted("stack", "cas_stack.png");
    }

    @Test
    public void test_adapt_as_unknown()
    {
        // The new takkernel symbology provider apparently doesn't validate the Affiliation, and it will default
        // to "S*U*" even if no battle dimension is specified.  So, we specify an unknown battle dimension
        // to force a failure.

        verifyAdapted("a-f-X-unknown", "unknown-type-f.png");
        verifyAdapted("a-a-X-unknown", "unknown-type-f.png");

        verifyAdapted("a-n-X-unknown", "unknown-type-n.png");

        verifyAdapted("a-h-X-unknown", "unknown-type-h.png");
        verifyAdapted("a-j-X-unknown", "unknown-type-h.png");
        verifyAdapted("a-k-X-unknown", "unknown-type-h.png");
        verifyAdapted("a-s-X-unknown", "unknown-type-h.png");

        verifyAdapted("a-u-X-unknown", "unknown-type-u.png");
        verifyAdapted("a-p-X-unknown", "unknown-type-u.png");

        verifyAdapted("a-X-X-unknown", "unknown-type.png");
    }

    @Test
    public void test_adding_map_object_calls_adapt()
    {
        final TestAdapter adapter = new TestAdapter();
        adapter.shouldAdapt = false; // Don't interfere, just record being called
        markerIconAdapter.addIconAdapter(adapter);

        final Marker marker = makeMarker("a-f");

        // This initial call should result in a call to adapt the marker
        markerIconAdapter.onClientMapObjectAdded(new MapObjectAdded(marker));
        assertThat(adapter.adaptCalled).isTrue();
        assertThat(marker.getIconImageURI().toString()).endsWith("unknown-type-f.png");
    }

    @Test
    public void test_changing_cot_type_causes_adapt()
    {
        final TestAdapter adapter = new TestAdapter();
        adapter.shouldAdapt = false; // Don't interfere, just record being called
        markerIconAdapter.addIconAdapter(adapter);

        final Marker marker = makeMarker("a-f");
        markerIconAdapter.adapt(marker);
        assertThat(adapter.adaptCalled).isTrue();
        assertThat(marker.getIconImageURI().toString()).endsWith("unknown-type-f.png");

        markerIconAdapter.onClientMapObjectAdded(new MapObjectAdded(marker));

        adapter.adaptCalled = false;
        marker.setCotType("b-i-x-i");
        assertThat(adapter.adaptCalled).isTrue();
        assertThat(marker.getIconImageURI().toString()).endsWith("camera.png");

        adapter.adaptCalled = false;
        marker.setCotType("a-k");
        assertThat(adapter.adaptCalled).isTrue();
        assertThat(marker.getIconImageURI().toString()).endsWith("unknown-type-h.png");
    }

    @Test
    public void test_changing_cot_type_after_removal_does_not_call_adapt()
    {
        final TestAdapter adapter = new TestAdapter();
        adapter.shouldAdapt = false; // Don't interfere, just record being called
        markerIconAdapter.addIconAdapter(adapter);

        final Marker marker = makeMarker("a-f-G");
        markerIconAdapter.adapt(marker);

        final String initialIconUri = marker.getIconImageURI().toString();

        markerIconAdapter.onClientMapObjectAdded(new MapObjectAdded(marker));
        markerIconAdapter.onClientMapObjectRemoved(new MapObjectRemoved(marker));

        adapter.adaptCalled = false;
        marker.setCotType("a-k-G");
        assertThat(adapter.adaptCalled).isFalse();
        assertThat(marker.getIconImageURI().toString()).isEqualTo(initialIconUri);
    }

    /**
     * Verify that a marker with the given cotType is adapted to the given iconName.
     */
    private void verifyAdapted(String cotType, String iconName)
    {
        verifyAdapted(cotType, iconName, null);
    }

    /**
     * Verify that a marker with the given cotType is adapted to the given iconName.
     * If {@code setup} is non-null, then have it process the marker before adapting.
     *
     * @param cotType  CoT type for marker
     * @param iconName Expected icon name after adapting
     * @param setup    Consumer to perform any needed setup on the marker before adapting
     */
    private void verifyAdapted(String cotType, String iconName, Consumer<Marker> setup)
    {
        final Marker marker = makeMarker(cotType);
        if (setup != null) setup.accept(marker);

        assertThat(markerIconAdapter.adapt(marker)).isTrue();
        assertThat(marker.getIconImageURI().toString()).endsWith(iconName);
    }

    /**
     * Set a role, and an optional 'how', on a Marker and validate that it is properly adapted.
     *
     * @param role     Role to set on Marker
     * @param how      'how' to set, may be {@code null}
     * @param iconName Expected icon name
     */
    private void verifyAdaptedByTeamRole(String role, String how, String iconName)
    {
        final Marker marker = makeMarker("a-f");
        marker.setMetaString(StandardAttributeNames.TEAM, "blue");

        marker.setMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, role);
        if (how != null) marker.setMetaString(StandardAttributeNames.HOW, how);

        assertThat(markerIconAdapter.adapt(marker)).isTrue();
        assertThat(marker.getMetaString(StandardAttributeNames.TEAM_ICON_ASSIGNED, ""))
                .endsWith("roles/" + iconName + ".png");
    }

    /**
     * Create a Marker with the given CoT type.
     */
    static Marker makeMarker(String cotType)
    {
        final MapObjectSpec spec = MapObjectSpec.builder("test", IMarkerMapObject.MARKER_TYPE)
                .cotType(cotType).build();

        return new MarkerMapObject(spec, null);
    }

    /**
     * Custom adapter to track calls.
     */
    private static final class TestAdapter implements IconAdapter
    {
        boolean shouldAdapt = true;
        boolean adaptCalled;
        boolean disposeCalled;

        @Override
        public boolean adapt(@Nonnull Marker marker)
        {
            adaptCalled = true;

            if (shouldAdapt)
            {
                marker.setIconImage(UIResources.getUri(TAK_ASSETS_ICONS, "no_icon.png"));
                return true;
            }
            return false;
        }

        @Override
        public void dispose()
        {
            disposeCalled = true;
        }
    }
}
