package gov.takx.core.globe;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObject.TooltipVisibility;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate handling of {@link IMapObject#setTooltipVisibility(TooltipVisibility)} and resulting affects on the
 * Map Objects {@link GlobeIconMapObject}.
 *
 * @since 5.6
 */
public class GlobeIconMapObjectTest extends AIntegrationTest
{
    @BeforeAll
    public void beforeAll()
    {
        PreferenceEntry.MAP_OBJECT_TOOLTIPS.put("default"); // Ensure global preference is "default"
    }

    @AfterEach
    public void afterEach()
    {
        deleteAllMapObjects();
    }

    @Test
    void newMapObject_hasDefaultTooltipVisibility()
    {
        final var globeIconMapObject = createMapObject();
        final var mapObject = globeIconMapObject.getMapObject();

        assertThat(mapObject.getTooltipVisibility()).isEqualTo(TooltipVisibility.DEFAULT);

        // When both the preference and MO visibility are DEFAULT, visibility is not forced
        assertThat(globeIconMapObject.isTooltipVisibilityForced()).isFalse();
    }

    @Test
    void whenTooltipVisibilitySet_globeIconTooltipVisibilityIsForced()
    {
        final var globeIconMapObject = createMapObject();
        final var mapObject = globeIconMapObject.getMapObject();

        final var changeListener = new ChangeListener();
        globeIconMapObject.addPropertyChangeListener(changeListener, List.of(GlobeIconMapObject.SHOW_TOOLTIP));

        mapObject.setTooltipVisibility(TooltipVisibility.ON);
        assertThat(globeIconMapObject.isTooltipVisibilityForced()).isTrue();
        assertThat(changeListener.count).isEqualTo(1);
        assertThat(changeListener.lastShowTooltip).isTrue();

        mapObject.setTooltipVisibility(TooltipVisibility.DEFAULT);
        assertThat(globeIconMapObject.isTooltipVisibilityForced()).isFalse();
        assertThat(changeListener.count).isEqualTo(2);
        assertThat(changeListener.lastShowTooltip).isFalse();

        mapObject.setTooltipVisibility(TooltipVisibility.OFF);
        assertThat(globeIconMapObject.isTooltipVisibilityForced()).isTrue();
        assertThat(changeListener.count).isEqualTo(2); // Was false, so no change expected
        assertThat(changeListener.lastShowTooltip).isFalse();
    }

    @Test
    void whenTooltipVisibilityForced_setShowTooltipCallsAreIgnored()
    {
        final var globeIconMapObject = createMapObject();
        final var mapObject = globeIconMapObject.getMapObject();

        // When not forced, calls to setShowToolTip are obeyed
        assertThat(globeIconMapObject.isShowToolTip()).isFalse();
        globeIconMapObject.setShowToolTip(true);
        assertThat(globeIconMapObject.isShowToolTip()).isTrue();
        globeIconMapObject.setShowToolTip(false);
        assertThat(globeIconMapObject.isShowToolTip()).isFalse();

        // When forced, calls are ignored
        mapObject.setTooltipVisibility(TooltipVisibility.ON);
        assertThat(globeIconMapObject.isShowToolTip()).isTrue();
        globeIconMapObject.setShowToolTip(false);
        assertThat(globeIconMapObject.isShowToolTip()).isTrue();

        mapObject.setTooltipVisibility(TooltipVisibility.OFF);
        assertThat(globeIconMapObject.isShowToolTip()).isFalse();
        globeIconMapObject.setShowToolTip(true);
        assertThat(globeIconMapObject.isShowToolTip()).isFalse();
    }

    private GlobeIconMapObject createMapObject()
    {
        final var mapObject = getBean(IMapObjectManager.class).getOrCreateMapObject(MapObjectSpec.builder("TEST", "TEST").build());

        return (GlobeIconMapObject) getBean(ApplicationGlobeManager.class).getPrimaryGlobeDelegate().getGlobeMapObjectManager()
                .getGlobeMapObject(mapObject.getKey());
    }

    private static class ChangeListener implements PropertyChangeListener
    {
        boolean lastShowTooltip;
        int count;

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            count++;
            lastShowTooltip = (boolean) evt.getNewValue();
        }
    }
}
