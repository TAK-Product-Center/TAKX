package gov.takx.api.geofence;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.maps.MapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.test.AContainerTest;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.3
 */
public class MonitoredTypeTest extends AContainerTest
{
    @Mocked
    private MapItem mapItem;

    @Test
    void All_matchesCorrectly()
    {
        assertWithCotType(MonitoredType.All, CotTypes.SELF).isTrue();
        assertWithCotType(MonitoredType.All, "a-f-anything").isTrue();
        assertWithCotType(MonitoredType.All, "a-n-anything").isTrue();
        assertWithCotType(MonitoredType.All, "a-h-anything").isTrue();
        assertWithCotType(MonitoredType.All, "a-u-anything").isTrue();

        assertWithCotType(MonitoredType.All, "").isFalse();
        assertWithCotType(MonitoredType.All, "random").isFalse();
        assertWithCotType(MonitoredType.All, CotTypes.EMPTY).isFalse();
    }

    @Test
    void TAKUsers_matchesCorrectly()
    {
        assertWithCotType(MonitoredType.TAKUsers, CotTypes.SELF).isTrue();
        assertWithCotTypeAndTeam("a-f-anything").isTrue();

        assertWithCotType(MonitoredType.TAKUsers, "a-f-anything").isFalse(); // Must have "team"
        assertWithCotTypeAndTeam("a-n-anything").isFalse();
        assertWithCotTypeAndTeam("a-h-anything").isFalse();
        assertWithCotTypeAndTeam("a-u-anything").isFalse();
        assertWithCotTypeAndTeam("").isFalse();
        assertWithCotTypeAndTeam("random").isFalse();
        assertWithCotTypeAndTeam(CotTypes.EMPTY).isFalse();
    }

    @Test
    void Friendly_matchesCorrectly()
    {
        assertWithCotType(MonitoredType.Friendly, CotTypes.SELF).isTrue();
        assertWithCotType(MonitoredType.Friendly, "a-f-anything").isTrue();

        assertWithCotType(MonitoredType.Friendly, "a-n-anything").isFalse();
        assertWithCotType(MonitoredType.Friendly, "a-h-anything").isFalse();
        assertWithCotType(MonitoredType.Friendly, "a-u-anything").isFalse();
        assertWithCotType(MonitoredType.Friendly, "").isFalse();
        assertWithCotType(MonitoredType.Friendly, "random").isFalse();
        assertWithCotType(MonitoredType.Friendly, CotTypes.EMPTY).isFalse();
    }

    @Test
    void Hostile_matchesCorrectly()
    {
        assertWithCotType(MonitoredType.Hostile, "a-h").isTrue();
        assertWithCotType(MonitoredType.Hostile, "a-h-anything").isTrue();

        assertWithCotType(MonitoredType.Hostile, CotTypes.SELF).isFalse();
        assertWithCotType(MonitoredType.Hostile, "a-f-anything").isFalse();
        assertWithCotType(MonitoredType.Hostile, "a-n-anything").isFalse();
        assertWithCotType(MonitoredType.Hostile, "a-u-anything").isFalse();
        assertWithCotType(MonitoredType.Hostile, "").isFalse();
        assertWithCotType(MonitoredType.Hostile, "random").isFalse();
        assertWithCotType(MonitoredType.Hostile, CotTypes.EMPTY).isFalse();
    }

    @Test
    void Custom_matchesCorrectly()
    {
        assertWithCotType(MonitoredType.Custom, CotTypes.SELF).isTrue();
        assertWithCotType(MonitoredType.Custom, "a-f-anything").isTrue();
        assertWithCotType(MonitoredType.Custom, "a-n-anything").isTrue();
        assertWithCotType(MonitoredType.Custom, "a-h-anything").isTrue();
        assertWithCotType(MonitoredType.Custom, "a-u-anything").isTrue();
    }

    private AbstractBooleanAssert<?> assertWithCotType(MonitoredType monitoredType, String cotType)
    {
        Mockito.reset(mapItem);
        Mockito.when(mapItem.getCotType()).thenReturn(cotType);
        return assertThat(monitoredType.matches(mapItem));
    }

    private AbstractBooleanAssert<?> assertWithCotTypeAndTeam(String cotType)
    {
        Mockito.reset(mapItem);
        Mockito.when(mapItem.getCotType()).thenReturn(cotType);
        Mockito.when(mapItem.hasMetaValue(StandardAttributeNames.TEAM)).thenReturn(true);
        return assertThat(MonitoredType.TAKUsers.matches(mapItem));
    }
}
