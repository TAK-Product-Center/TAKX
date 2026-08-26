package gov.takx.core.mapobject.gui;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.MapObjectTestUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.7
 */
class MapObjectFieldTest extends AIntegrationTest
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
        var field = new MapObjectField<>();

        field.setSelectedItem(mapObject);
        assertThat(field.getSelectedItem()).isEqualTo(mapObject);
        assertThat(field.objectLabel.getText()).isEqualTo(mapObject.getName());

        var triggered = new MutableBoolean();
        field.addActionListener(event -> triggered.setTrue());
        field.setSelectedItem(null);
        assertThat(triggered.getValue()).isTrue();

        triggered.setFalse();
        field.addNewObjectActionListener(event -> triggered.setTrue());
        field.newButton.doClick();
        assertThat(triggered.getValue()).isTrue();

        field.setSelectedItem(null);
        var emptyText = "My empty text";
        field.setEmptyText(emptyText);
        assertThat(field.objectLabel.getText()).isEqualTo(emptyText);
    }
}
