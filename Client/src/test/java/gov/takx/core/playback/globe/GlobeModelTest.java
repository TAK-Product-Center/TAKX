package gov.takx.core.playback.globe;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.globe.IDeviceTracksModel;
import gov.takx.core.playback.APlaybackIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.48
 */
public class GlobeModelTest extends APlaybackIntegrationTest
{
    /**
     * Construct with a registered plugin so we can use it to send messages.
     */
    public GlobeModelTest()
    {
    }

    @Test
    public void testCollection() throws Exception
    {
        final GlobeModel globeModel = new GlobeModel(timeSpan);

        assertThat(globeModel.getDevicesList()).isEmpty();
        assertThat(globeModel.getAllDevices()).hasSize(2);
        assertThat(globeModel.isDeviceInPlayback(mapObject1)).isFalse();
        assertThat(mapObject1.inPlaybackMode()).isFalse();
        assertThat(globeModel.isDeviceInPlayback(mapObject2)).isFalse();
        assertThat(mapObject2.inPlaybackMode()).isFalse();

        final IDeviceTracksModel dtm1 = globeModel.addDevice(mapObject1);
        dtm1.waitForInitialization(5, TimeUnit.SECONDS);

        assertThat(globeModel.getDevicesList()).isEqualTo(List.of(mapObject1));
        assertThat(globeModel.isDeviceInPlayback(mapObject1)).isTrue();
        // devices are only in playback when the playback is not live
        assertThat(globeModel.isDeviceInPlayback(mapObject2)).isFalse();
        assertThat(globeModel.getDeviceTracksModel(mapObject1)).isSameAs(dtm1);

        final IDeviceTracksModel dtm2 = globeModel.addDevice(mapObject2);
        dtm2.waitForInitialization(5, TimeUnit.SECONDS);

        List<IMapObject> devicesList = globeModel.getDevicesList();
        devicesList.sort(new NameComparator());
        assertThat(devicesList).isEqualTo(List.of(mapObject1, mapObject2));
        assertThat(globeModel.isDeviceInPlayback(mapObject1)).isTrue();
        assertThat(globeModel.isDeviceInPlayback(mapObject2)).isTrue();
        assertThat(globeModel.getDeviceTracksModel(mapObject2)).isSameAs(dtm2);

        // Adding a device a second time should not change things
        assertThat(globeModel.addDevice(mapObject2)).isNotNull();

        devicesList = globeModel.getDevicesList();
        devicesList.sort(new NameComparator());
        assertThat(devicesList).isEqualTo(List.of(mapObject1, mapObject2));
        assertThat(globeModel.isDeviceInPlayback(mapObject1)).isTrue();
        assertThat(globeModel.isDeviceInPlayback(mapObject2)).isTrue();
        assertThat(globeModel.getDeviceTracksModel(mapObject1)).isSameAs(dtm1);
        assertThat(globeModel.getDeviceTracksModel(mapObject2)).isSameAs(dtm2);

        globeModel.disposeResources();
    }

    @Test
    @SuppressWarnings("EnumOrdinal")
    public void testMapObjectTableModel()
    {
        final GlobeModel globeModel = new GlobeModel(timeSpan);

        final GlobeMapObjectTableModel tableModel = new GlobeMapObjectTableModel(globeModel, Collections.emptyList());

        assertThat(tableModel.getRowCount()).isEqualTo(2);
        assertThat(tableModel.getSelectedMapObjects()).isEmpty();

        final int nameColumn = GlobeMapObjectTableModel.Columns.NAME.ordinal();
        final int selectedColumn = GlobeMapObjectTableModel.Columns.SELECTED.ordinal();

        assertThat(tableModel.getValueAt(0, nameColumn)).isEqualTo(mapObject1.getName());
        assertThat(tableModel.getValueAt(1, nameColumn)).isEqualTo(mapObject2.getName());
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isFalse();
        assertThat((Boolean) tableModel.getValueAt(1, selectedColumn)).isFalse();

        tableModel.toggleAllSelected();
        assertThat(tableModel.getSelectedMapObjects()).hasSize(2);
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isTrue();
        assertThat((Boolean) tableModel.getValueAt(1, selectedColumn)).isTrue();

        tableModel.toggleAllSelected();
        assertThat(tableModel.getSelectedMapObjects()).isEmpty();
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isFalse();
        assertThat((Boolean) tableModel.getValueAt(1, selectedColumn)).isFalse();

        tableModel.setSelectedMapObjects(List.of(mapObject1));
        assertThat(tableModel.getSelectedMapObjects()).isEqualTo(List.of(mapObject1));
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isTrue();
        assertThat((Boolean) tableModel.getValueAt(1, selectedColumn)).isFalse();

        tableModel.setSelectedMapObjects(List.of(mapObject2));
        assertThat(tableModel.getSelectedMapObjects()).isEqualTo(List.of(mapObject2));
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isFalse();
        assertThat((Boolean) tableModel.getValueAt(1, selectedColumn)).isTrue();

        tableModel.setSelectedMapObjects(List.of(mapObject1, mapObject2));
        assertThat(tableModel.getSelectedMapObjects().containsAll(List.of(mapObject1, mapObject2))).isTrue();
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isTrue();
        assertThat((Boolean) tableModel.getValueAt(1, selectedColumn)).isTrue();

        tableModel.setValueAt(false, 1, selectedColumn);
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isTrue();
        assertThat((Boolean) tableModel.getValueAt(1, selectedColumn)).isFalse();

        tableModel.setValueAt(false, 0, selectedColumn);
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isFalse();
        assertThat((Boolean) tableModel.getValueAt(1, selectedColumn)).isFalse();

        // Filter down to just plugin 2
        tableModel.updateVisibleVector("2");

        assertThat(tableModel.getRowCount()).isEqualTo(1);
        assertThat(tableModel.getValueAt(0, nameColumn)).isEqualTo(mapObject2.getName());
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isFalse();

        tableModel.setValueAt(true, 0, selectedColumn);
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isTrue();
        assertThat(tableModel.getSelectedMapObjects()).isEqualTo(List.of(mapObject2));

        // Remove the filter
        tableModel.updateVisibleVector("");

        assertThat(tableModel.getRowCount()).isEqualTo(2);
        assertThat(tableModel.getValueAt(0, nameColumn)).isEqualTo(mapObject1.getName());
        assertThat(tableModel.getValueAt(1, nameColumn)).isEqualTo(mapObject2.getName());
        assertThat((Boolean) tableModel.getValueAt(0, selectedColumn)).isFalse();
        assertThat((Boolean) tableModel.getValueAt(1, selectedColumn)).isTrue();
    }

    private static final class NameComparator implements Comparator<IMapObject>
    {
        @Override
        public int compare(IMapObject o1, IMapObject o2)
        {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
