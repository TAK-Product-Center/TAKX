package gov.takx.core.mapdata;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IRDMPersist;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.dataservices.persist.RaptorDataMessage;
import gov.takx.platform.globe.renderer.RendererSearchResult;
import gov.takx.platform.utils.StandardTimeFormatting;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RaptorDataMessageTableModelTest extends AIntegrationTest
{
    private static final Sector SECTOR = new Sector(Angle.fromDegreesLatitude(-10), Angle.fromDegreesLatitude(10), Angle.fromDegreesLongitude(-10), Angle.fromDegreesLongitude(10));
    private static final RaptorLocation LOCATION = new RaptorLocation(5, 5, 5);
    private static final double EPSILON = 1e-5;

    private final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
    private final StandardTimeFormatting timeFormatting = getBean(StandardTimeFormatting.class);

    private RaptorDataMessageTableModel tableModel;
    private IMapObject mapObject;
    private final List<IRaptorDataMessage> messageList = new ArrayList<>();
    int messageCount = 10;

    /**
     * Create the MO, and then create, attach to the MO, persist, and track a number of RDMs. Then check that the RDMs
     * were persisted correctly, and create the {@link RaptorDataMessageTableModel} from the {@link IRDMPersist} query.
     */
    @BeforeAll
    public void beforeAll()
    {
        mapObject = mapObjectManager.getOrCreateMapObject(MapObjectSpec.builder("test", "test", "test").location(LOCATION).build());
        ILocation moLocation = mapObject.getLocation();
        IPrePersistRaptorDataMessage message;
        for (int i = 0; i < messageCount; i++)
        {
            message = new RaptorDataMessage(DataStructureBuilder.createManualLocationStructure("test", "test").getStructure(), mapObject);
            message.setLocation(moLocation.getLatitude() + i, moLocation.getLongitude() + i, moLocation.getElevation() + i);
            message.setHeading(i);
            message.setSpeedInMps(i);
            message.setTime(System.currentTimeMillis());
            messageList.add(message);
        }
        tableModel = new RaptorDataMessageTableModel(new RendererSearchResult(messageList.size(), SECTOR, Map.of(mapObject, messageList)), SECTOR);
    }

    @AfterAll
    public void cleanupAfterTest()
    {
        deleteAllMapObjects();
    }

    @Test
    public void correct_messageCount()
    {
        assertThat(tableModel.getMessageCount()).isEqualTo(messageCount);
    }

    @Test
    public void correct_valueAtReturns()
    {
        for (int i = 0; i < messageList.size(); i++)
        {
            String time = tableModel.getValueAt(i, RaptorDataMessageTableModel.Columns.Time.ordinal()).toString();
            String description = tableModel.getValueAt(i, RaptorDataMessageTableModel.Columns.Description.ordinal()).toString();
            String type = tableModel.getValueAt(i, RaptorDataMessageTableModel.Columns.Type.ordinal()).toString();
            String id = tableModel.getValueAt(i, RaptorDataMessageTableModel.Columns.ID.ordinal()).toString();
            String heading = tableModel.getValueAt(i, RaptorDataMessageTableModel.Columns.Heading.ordinal()).toString();
            double speed = (double) tableModel.getValueAt(i, RaptorDataMessageTableModel.Columns.Speed.ordinal());
            String receivedTime = tableModel.getValueAt(i, RaptorDataMessageTableModel.Columns.Received.ordinal()).toString();

            IRaptorDataMessage message = messageList.get(i);
            assertThat(description).isEqualTo(mapObject.getName());
            assertThat(latLonElevMatches(i)).isEqualTo(true);
            assertThat(locationMatches(i)).isEqualTo(true);
            assertThat(time).isEqualTo(timeFormatting.formatWithCurrent(message.getTime()));
            assertThat(receivedTime).isEqualTo(timeFormatting.formatWithDefault(message.getReceiveTime()));
            assertThat(heading).isEqualTo(String.valueOf(message.getHeading()));
            assertThat(speed).isEqualTo(message.getSpeedInMps());
            assertThat(type).isEqualTo(message.getType());
            assertThat(id).isEqualTo(message.getUnitID());
        }
    }

    /**
     * Check if the {@link RaptorDataMessageTableModel.Columns#Latitude}, {@link RaptorDataMessageTableModel.Columns#Longitude},
     * and {@link RaptorDataMessageTableModel.Columns#Elevation} of the tracked message matches the table model message.
     * @param index Index in tracked message list and table model list
     * @return {@code true} if the locations are within {@link #EPSILON} difference.
     */
    private boolean latLonElevMatches(int index)
    {
        IRaptorDataMessage msg = messageList.get(index);
        double tableLat = ParseUtils.parseDouble((String) tableModel.getValueAt(index, RaptorDataMessageTableModel.Columns.Latitude.ordinal()), Double.NaN);
        double tableLon = ParseUtils.parseDouble((String) tableModel.getValueAt(index, RaptorDataMessageTableModel.Columns.Longitude.ordinal()), Double.NaN);
        double tableElev = (double) tableModel.getValueAt(index, RaptorDataMessageTableModel.Columns.Elevation.ordinal());

        return Math.abs(tableLat - msg.getLatitude()) <= EPSILON
                && Math.abs(tableLon - msg.getLongitude()) <= EPSILON
                && Math.abs(tableElev - msg.getElevation()) <= EPSILON;
    }

    /**
     * Check if the {@link RaptorDataMessageTableModel.Columns#Location} of the tracked message matches the table model message.
     * @param index Index in tracked message list and table model list
     * @return {@code true} if the locations are within {@link #EPSILON} difference.
     */
    private boolean locationMatches(int index)
    {
        IRaptorDataMessage msg = messageList.get(index);
        ILocation msgLatLon = CoordinateSystem.computeLatLonFromString(msg.getLocation().toString());
        ILocation tableLatLon = CoordinateSystem.computeLatLonFromString((String) tableModel.getValueAt(index, RaptorDataMessageTableModel.Columns.Location.ordinal()));

        return msgLatLon != null && tableLatLon != null
                && Math.abs(tableLatLon.getLatitude() - msgLatLon.getLatitude()) <= EPSILON
                && Math.abs(tableLatLon.getLongitude() - msgLatLon.getLongitude()) <= EPSILON
                && Math.abs(tableLatLon.getElevation() - msgLatLon.getElevation()) <= EPSILON;
    }
}
