package gov.takx.core.pointdropper;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.icons.IIconSetManager;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.event.globemessage.HideGlobeMessageEvent;
import gov.takx.api.event.globemessage.ShowGlobeMessageEvent;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.api.pointdropper.CotIcon;
import gov.takx.api.pointdropper.PointDropperConfiguration;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.AIntegrationTest;
import jakarta.enterprise.event.Observes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link PointDropperService}.
 *
 * @since 3.0
 */
@SuppressWarnings({"DataFlowIssue", "removal"})
public class PointDropperServiceTest extends AIntegrationTest
{
    private static final RaptorLocation ZERO_LOCATION = new RaptorLocation(0, 0);

    private PointDropperService pointDropperService;
    private ISyntheticPluginDefinition redPushpinDefinition;
    private PointDropperConfiguration redPushPinConfiguration;

    @AfterEach
    public void cleanup()
    {
        deleteAllMapObjects();
    }

    @Override
    protected void postProjectOpen()
    {
        pointDropperService = getBean(PointDropperService.class);
        final var iconSetManager = getBean(IIconSetManager.class);

        final var redIcon = iconSetManager.getIcon(IIconSetManager.TAKX_LEGACY_ICONSET_UID, "Red.png");
        assertThat(redIcon).isNotNull();

        redPushpinDefinition = iconSetManager.getOrCreatePluginDefinition(redIcon);
        redPushPinConfiguration = PointDropperConfiguration.builder(redPushpinDefinition).build();
    }

    @Test
    @Disabled("TAKX-3244")
    void test_cot_affiliation_produces_correct_type()
    {
        // TODO (TAKX-3244): Re-evaluate these assertions
        assertThat(PointDropperConfiguration.builder(CotTypes.PREFIX_FRIENDLY).build().pluginDefinition().getType()).isEqualTo(CotIcon.FriendlyGround.getName());
        assertThat(PointDropperConfiguration.builder(CotTypes.PREFIX_HOSTILE).build().pluginDefinition().getType()).isEqualTo(CotIcon.HostileGround.getName());
        assertThat(PointDropperConfiguration.builder(CotTypes.PREFIX_NEUTRAL).build().pluginDefinition().getType()).isEqualTo(CotIcon.NeutralGround.getName());
        assertThat(PointDropperConfiguration.builder(CotTypes.PREFIX_UNKNOWN).build().pluginDefinition().getType()).isEqualTo(CotIcon.UnknownGround.getName());

        // An "unknown" affiliation (something other than 'f', 'h', 'n', or 'u') should result in a configuration
        // that uses CotIcon.UnknownGround.
        assertThat(PointDropperConfiguration.builder("a-X").build().pluginDefinition().getType()).isEqualTo(CotIcon.UnknownGround.getName());
    }

    @Test
    public void test_activate_point_dropper_fires_event()
    {
        final EventListener eventListener = new EventListener();

        pointDropperService.activatePointDropper(redPushPinConfiguration);

        assertThat(eventListener.showGlobeMessageEvent).isNotNull();

        eventListener.dispose();
    }

    @Test
    public void test_deactivate_point_dropper_fires_event()
    {
        pointDropperService.activatePointDropper(redPushPinConfiguration);

        final EventListener eventListener = new EventListener();
        pointDropperService.deactivatePointDropper();

        assertThat(eventListener.hideGlobeMessageEvent).isNotNull();

        eventListener.dispose();
    }

    /** @since 5.2 */
    @Test
    void test_drop_and_deactivate_handlers()
    {
        final AtomicInteger dropCount = new AtomicInteger(0);
        final AtomicInteger deactivationCount = new AtomicInteger(0);

        final PointDropperConfiguration configuration = PointDropperConfiguration.builder(CotIcon.FriendlyGround)
                .dropHandler(mo -> dropCount.incrementAndGet())
                .deactivationHandler(deactivationCount::incrementAndGet)
                .build();

        pointDropperService.activatePointDropper(configuration);

        for (int i = 1; i < 5; i++)
        {
            pointDropperService.dropPoint(new RaptorLocation(i, i));

            assertThat(dropCount).hasValue(i);
            assertThat(deactivationCount).hasValue(0);
        }

        pointDropperService.deactivatePointDropper();
        assertThat(deactivationCount).hasValue(1);
    }

    /** @since 5.2 */
    @Test
    void test_cot_namer()
    {
        final Map<String, String> expectedPrefix = Map.of(
                CotIcon.FriendlyGround.getCotType(), "F.",
                CotIcon.NeutralGround.getCotType(), "N.",
                CotIcon.UnknownGround.getCotType(), "U.",
                CotIcon.HostileGround.getCotType(), "R.",
                CotTypes.EVAC_POINT, "MED.",
                CotTypes.MANUALLY_PLACED_SENSOR, "S.",
                CotTypes.EMPTY, PreferenceEntry.CALL_SIGN.getStringValue());

        expectedPrefix.forEach((cotType, prefix) -> {
            final var configuration = PointDropperConfiguration.builder(cotType).useCotNamer().build();
            assertThat(configuration.nameGenerator().generateName()).startsWith(prefix);
        });
    }

    @Test
    public void test_drop_point_creates_map_object()
    {
        pointDropperService.activatePointDropper(makeConfiguration(null, null, 5, 2));

        final EventListener eventListener = new EventListener();
        final IMapObject mapObject = pointDropperService.dropPoint(new RaptorLocation(12, 34));
        assertThat(mapObject).isNotNull();
        assertThat(mapObject.getFamily()).isEqualTo(redPushpinDefinition.getFamily());
        assertThat(mapObject.getType()).isEqualTo(redPushpinDefinition.getType());
        assertThat(mapObject.getGroupName()).isEqualTo(redPushpinDefinition.getGroup());
        assertThat(mapObject.getLocation()).isEqualTo(new GeoPoint(12, 34));
        assertThat(mapObject.getCotType()).isEqualTo(CotTypes.EMPTY);

        eventListener.dispose();
    }

    @Test
    public void test_drop_point_uses_custom_cot_type()
    {
        final String cotType = "a-n-G-U-C-I";
        pointDropperService.activatePointDropper(makeConfiguration(cotType, null, -1, 1));
        IMapObject mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject.getCotType()).isEqualTo(cotType);
    }

    @Test
    public void test_drop_point_uses_custom_name_values()
    {
        // If no prefix, name should just be the number
        pointDropperService.activatePointDropper(makeConfiguration(null, null, 5, 2));
        IMapObject mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject.getName()).isEqualTo("5");

        mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject.getName()).isEqualTo("7");

        // If prefix and number, name should be "prefix #"
        pointDropperService.activatePointDropper(makeConfiguration(null, "test", 2, 3));
        mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject.getName()).isEqualTo("test 2");

        mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject.getName()).isEqualTo("test 5");

        // If just prefix, name should be "prefix #", where # starts at 1 and increments by 1 unless an MO with that
        // name already exists
        pointDropperService.activatePointDropper(makeConfiguration(null, "test", -1, 1));
        mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject.getName()).isEqualTo("test 1");

        mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject.getName()).isEqualTo("test 3"); // There was already a test 2

        mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject.getName()).isEqualTo("test 4");

        mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject.getName()).isEqualTo("test 6"); // There was already a test 5
    }

    private PointDropperConfiguration makeConfiguration(String cotType, String prefix, int start, int increment)
    {
        return PointDropperConfiguration.builder(redPushpinDefinition)
                .cotType(cotType)
                .nameGenerator(prefix, start, increment).build();
    }

    /** @since 5.2 */
    @Test
    void test_configuration_with_alternate_iconset()
    {
        // Access an icon set other than TAKX Legacy
        assertThatNoException().isThrownBy(() -> PointDropperConfiguration.builder("Default", "Bike.png").build());
    }

    /** @since 5.2 */
    @Test
    @SuppressLogging(PointDropperConfiguration.class)
    public void test_activate_api_with_invalid_config()
    {
        // Invalid group/type combo
        assertThatIllegalArgumentException().isThrownBy(() -> pointDropperService.activatePointDropper(PointDropperConfiguration.builder("Unknown", "missing").build()));
    }

    @Test
    public void test_all_in_one_drop_point_api()
    {
        assertThat(pointDropperService.isActive()).isFalse();
        final var configuration = PointDropperConfiguration.builder(CotIcon.FriendlyGround).build();
        final IMapObject mapObject = pointDropperService.dropPoint(configuration, ZERO_LOCATION);
        assertThat(pointDropperService.isActive()).isFalse(); // Should auto-deactivate

        assertThat(mapObject).isNotNull();
        assertThat(mapObject.getFamily()).startsWith(KeyUtils.RESOURCE_ICON_FAMILY);

        // TODO (TAKX-3244): Re-evaluate this assertion
        //     assertThat(mapObject.getType()).isEqualTo("Friendly Ground");
        //noinspection AssertBetweenInconvertibleTypes
        assertThat(ZERO_LOCATION).isEqualTo(mapObject.getLocation());
    }

    @Test
    public void test_activate_api()
    {
        // Valid iconset/icon
        pointDropperService.activatePointDropper(IIconSetManager.TAKX_LEGACY_ICONSET_UID, "Red.png");

        final IMapObject mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject).isNotNull();
        assertThat(mapObject.getFamily()).startsWith(KeyUtils.RESOURCE_ICON_FAMILY);
        assertThat(mapObject.getType()).isEqualTo("Red");
    }

    @Test
    public void test_deactivate_api()
    {
        pointDropperService.activatePointDropper(redPushPinConfiguration);
        pointDropperService.deactivatePointDropper();

        // dropPoint() should return null if the dropper is not active
        final IMapObject mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject).isNull();
    }

    @Test
    public void test_activate_cot_api_with_cotIcon()
    {
        pointDropperService.activateCotPointDropper(CotIcon.FriendlyGround);

        final IMapObject mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject).isNotNull();
        assertThat(mapObject.getFamily()).startsWith(KeyUtils.RESOURCE_ICON_FAMILY);

        // TODO (TAKX-3244): Re-evaluate these assertions
        //  assertThat(mapObject.getType()).isEqualTo(CotIcon.FriendlyGround.getName());
        //  assertThat(mapObject.getCotType()).isEqualTo(CotIcon.FriendlyGround.getCotType());
    }

    @Test
    public void test_activate_cot_api_with_cotType()
    {
        pointDropperService.activateCotPointDropper("a-h-G");

        final IMapObject mapObject = pointDropperService.dropPoint(ZERO_LOCATION);
        assertThat(mapObject).isNotNull();
        assertThat(mapObject.getCotType()).isEqualTo("a-h-G");
    }

    /**
     * Simple observer to test events.
     */
    @PseudoBean
    @SuppressWarnings("unused")
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

        @RemoveObserver
        public void dispose()
        {
        }
    }
}
