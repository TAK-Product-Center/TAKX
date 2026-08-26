package gov.takx.core.mapobject.gui;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.ui.event.GlobeCreatedEvent;
import gov.takx.api.ui.event.MapObjectTrackingEvent;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.MapObjectTestUtils;
import gov.takx.test.utils.SwingTestUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Test;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.8
 */
public class CenterOnFieldTest extends AIntegrationTest
{
    private MapObjectTestUtils moTestUtils;

    @Override
    protected void postProjectOpen()
    {
        moTestUtils = new MapObjectTestUtils(getBean(MapObjectManager.class));
    }

    @Test
    void test()
    {
        IMapObject mapObject = moTestUtils.createMarkerMapObject(mo -> mo.setName("MOF Test"), null, null);
        var eventHolder = new MutableObject<ChangeEvent>();
        ChangeListener listener = eventHolder::setValue;
        var field = new CenterOnField();
        field.addChangeListener(listener);
        var globeId = 7;

        // Adding globes should not fire event
        field.onGlobeCreated(new GlobeCreatedEvent(globeId));
        SwingTestUtils.waitForEdtEvents();
        assertThat(eventHolder.get()).isNull();

        // Programmatic selection should not fire event
        field.setSelectedGlobeIds(mapObject, false);
        assertThat(eventHolder.get()).isNull();
        field.setSelectedGlobeIds(Collections.emptySet());
        assertThat(eventHolder.get()).isNull();

        // Monitoring event should NOT fire event or change value when not monitoring
        field.setSelectedGlobeIds(mapObject, false);
        field.onTrackingEvent(new MapObjectTrackingEvent(globeId, mapObject.getController(), MapObjectTrackingEvent.TrackingState.STARTED, MapObjectTrackingEvent.TrackingType.CENTER_ON));
        SwingTestUtils.waitForEdtEvents();
        assertThat(eventHolder.get()).isNull();
        assertThat(field.getSelectedGlobeIds()).isEmpty();

        // Monitoring event should NOT fire event but SHOULD change value when monitoring
        field.setSelectedGlobeIds(mapObject, true);
        field.onTrackingEvent(new MapObjectTrackingEvent(globeId, mapObject.getController(), MapObjectTrackingEvent.TrackingState.STARTED, MapObjectTrackingEvent.TrackingType.CENTER_ON));
        SwingTestUtils.waitForEdtEvents();
        assertThat(eventHolder.get()).isNull();
        assertThat(field.getSelectedGlobeIds()).containsExactly(globeId);
        field.setSelectedGlobeIds(Collections.emptySet());
    }
}
