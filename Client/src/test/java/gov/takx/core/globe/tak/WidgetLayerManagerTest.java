package gov.takx.core.globe.tak;

import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.globe.IWidget;
import gov.takx.core.globe.widgets.CompassWidget;
import gov.takx.core.globe.widgets.MessageOverlayWidget;
import gov.takx.core.globe.widgets.NavigationWidget;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static gov.takx.core.globe.tak.WidgetLayerManager.getWidgetStateKey;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link WidgetLayerManager}.
 *
 * @since 5.8
 */
public class WidgetLayerManagerTest extends AIntegrationTest
{
    private final WidgetLayerManager manager;

    public WidgetLayerManagerTest()
    {
        manager = getBean(WidgetLayerManager.class);
    }

    @Test
    public void storeState_createsRestorableFile(@Mocked CompassWidget compassWidget,
                                                 @Mocked NavigationWidget navigationWidget,
                                                 @Mocked MessageOverlayWidget messageOverlayWidget)
    {
        when(compassWidget.isVisible()).thenReturn(false);
        when(navigationWidget.isVisible()).thenReturn(true);

        manager.storeState(List.of(compassWidget, navigationWidget, messageOverlayWidget));
        manager.restoreState();

        final var map = ((WidgetLayerManager) CdiTestUtils.getInstance(manager)).widgetStateMap;

        assertEquals(2, map.size());

        validate(compassWidget, map, true, false);
        validate(navigationWidget, map, true, true);
        validate(messageOverlayWidget, map, false, false);
    }

    private void validate(IWidget widget, Map<String, List<WidgetLayerManager.WidgetState>> map, boolean exists, boolean visible)
    {
        final var key = getWidgetStateKey(widget);
        assertThat(map.containsKey(key)).isEqualTo(exists);

        if (!exists) return;

        final var states = map.get(key);
        assertThat(states.size()).isEqualTo(1);

        final var state = states.get(0);
        assertThat(state.visible()).isEqualTo(visible);
    }
}
